package dev.thomcgn.findly.listing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sun.net.httpserver.HttpServer;
import dev.thomcgn.findly.error.AnalysisErrorCode;
import dev.thomcgn.findly.error.AnalysisException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Socket-level fixtures only. Production URL/DNS rules remain enabled and have separate tests. */
class ListingHttpIntegrationTest {
  @Test
  void connectorUsesInjectedAddressesWithoutSecondSystemResolutionAndDoesNotFollowRedirects()
      throws Exception {
    var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext(
        "/fixture",
        exchange -> {
          byte[] html = "<h1>Fixture</h1>".getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "text/html");
          exchange.sendResponseHeaders(200, html.length);
          exchange.getResponseBody().write(html);
          exchange.close();
        });
    server.createContext(
        "/redirect",
        exchange -> {
          exchange.getResponseHeaders().add("Location", "http://127.0.0.1/private");
          exchange.sendResponseHeaders(302, -1);
          exchange.close();
        });
    server.start();
    try {
      // The test connector routes only to this loopback fixture. No public host is resolved.
      var resolver = mock(PublicAddressResolver.class);
      when(resolver.resolve(anyString(), anyInt())).thenCallRealMethod();
      when(resolver.resolve("kleinanzeigen.de"))
          .thenReturn(new InetAddress[] {InetAddress.getLoopbackAddress()});
      var transport =
          new ApacheListingTransport(new ListingClientProperties(1000, 1000, 1024), resolver);
      String base = "http://kleinanzeigen.de:" + server.getAddress().getPort();
      assertEquals("<h1>Fixture</h1>", transport.exchange(URI.create(base + "/fixture")).html());
      verify(resolver, times(1)).resolve("kleinanzeigen.de");
      var redirect = transport.exchange(URI.create(base + "/redirect"));
      assertEquals(302, redirect.status());
      assertEquals("http://127.0.0.1/private", redirect.location());
      verify(resolver, times(2)).resolve("kleinanzeigen.de");
    } finally {
      server.stop(0);
    }
  }

  @Test
  void stalledAndOversizedResponsesAreAborted() throws Exception {
    var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    var workers = Executors.newVirtualThreadPerTaskExecutor();
    server.setExecutor(workers);
    CountDownLatch release = new CountDownLatch(1);
    server.createContext(
        "/stall",
        exchange -> {
          try {
            release.await(5, TimeUnit.SECONDS);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
    server.createContext(
        "/large",
        exchange -> {
          exchange.getResponseHeaders().add("Content-Type", "text/html");
          exchange.sendResponseHeaders(200, 0);
          try {
            exchange.getResponseBody().write(new byte[4096]);
          } finally {
            exchange.close();
          }
        });
    server.start();
    try {
      var resolver = mock(PublicAddressResolver.class);
      when(resolver.resolve(anyString(), anyInt())).thenCallRealMethod();
      when(resolver.resolve("kleinanzeigen.de"))
          .thenReturn(new InetAddress[] {InetAddress.getLoopbackAddress()});
      var transport =
          new ApacheListingTransport(new ListingClientProperties(200, 200, 32), resolver);
      String base = "http://kleinanzeigen.de:" + server.getAddress().getPort();
      assertTimeoutPreemptively(
          Duration.ofSeconds(3),
          () ->
              assertEquals(
                  AnalysisErrorCode.ANALYSIS_TIMEOUT,
                  assertThrows(
                          AnalysisException.class,
                          () -> transport.exchange(URI.create(base + "/stall")))
                      .code()));
      assertEquals(
          AnalysisErrorCode.LISTING_TOO_LARGE,
          assertThrows(
                  AnalysisException.class, () -> transport.exchange(URI.create(base + "/large")))
              .code());
    } finally {
      release.countDown();
      server.stop(0);
      workers.close();
    }
  }
}
