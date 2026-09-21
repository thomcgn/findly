package dev.thomcgn.findly.listing;

import dev.thomcgn.findly.common.validation.ListingUrl;
import dev.thomcgn.findly.error.AnalysisErrorCode;
import dev.thomcgn.findly.error.AnalysisException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class ApacheListingTransport implements ListingTransport {
  private final ListingClientProperties properties;
  private final PublicAddressResolver resolver;

  public ApacheListingTransport(
      ListingClientProperties properties, PublicAddressResolver resolver) {
    this.properties = properties;
    this.resolver = resolver;
  }

  @Override
  public Response get(URI uri) {
    return exchange(ListingUrl.parse(uri.toString()));
  }

  Response exchange(URI uri) {
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      throw new IllegalStateException("Listing HTTP calls must not run in a transaction");
    }
    var connections =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setDnsResolver(resolver)
            .setDefaultConnectionConfig(
                ConnectionConfig.custom()
                    .setConnectTimeout(Timeout.ofMilliseconds(properties.connectTimeoutMillis()))
                    .setSocketTimeout(Timeout.ofMilliseconds(properties.readTimeoutMillis()))
                    .build())
            .build();
    try (var client =
        HttpClients.custom()
            .setConnectionManager(connections)
            .disableRedirectHandling()
            .disableAutomaticRetries()
            .disableCookieManagement()
            .disableContentCompression()
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    .setResponseTimeout(Timeout.ofMilliseconds(properties.readTimeoutMillis()))
                    .setConnectionRequestTimeout(
                        Timeout.ofMilliseconds(properties.connectTimeoutMillis()))
                    .build())
            .build()) {
      var request = new HttpGet(uri);
      request.setHeader("User-Agent", "Findly/1.0");
      request.setHeader("Accept", "text/html,application/xhtml+xml");
      request.setHeader("Accept-Encoding", "identity");
      AtomicBoolean timedOut = new AtomicBoolean();
      try (var timer = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory())) {
        var deadline =
            timer.schedule(
                () -> {
                  timedOut.set(true);
                  request.cancel();
                },
                (long) properties.connectTimeoutMillis() + properties.readTimeoutMillis(),
                TimeUnit.MILLISECONDS);
        try {
          try (ClassicHttpResponse response = client.executeOpen(null, request, null)) {
            try {
              return readResponse(response);
            } finally {
              request.cancel();
            } // Abort rather than drain an untrusted response.
          }
        } catch (IOException ex) {
          if (timedOut.get()) throw new AnalysisException(AnalysisErrorCode.ANALYSIS_TIMEOUT);
          throw ex;
        } finally {
          deadline.cancel(false);
        }
      }
    } catch (SocketTimeoutException ex) {
      throw new AnalysisException(AnalysisErrorCode.ANALYSIS_TIMEOUT);
    } catch (IOException ex) {
      throw new AnalysisException(AnalysisErrorCode.LISTING_FETCH_FAILED);
    }
  }

  Response readResponse(ClassicHttpResponse response) throws IOException {
    int status = response.getCode();
    if (status >= 300 && status < 400) {
      var location = response.getFirstHeader("Location");
      return new Response(status, location == null ? null : location.getValue(), null);
    }
    if (status == 401 || status == 403 || status == 429)
      throw new AnalysisException(AnalysisErrorCode.LISTING_ACCESS_BLOCKED);
    if (status != 200) throw new AnalysisException(AnalysisErrorCode.LISTING_FETCH_FAILED);
    var entity = response.getEntity();
    String type =
        entity == null || entity.getContentType() == null
            ? ""
            : entity.getContentType().split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    if (!type.equals("text/html") && !type.equals("application/xhtml+xml"))
      throw new AnalysisException(AnalysisErrorCode.LISTING_CONTENT_UNSUPPORTED);
    if (entity.getContentEncoding() != null
        && !entity.getContentEncoding().equalsIgnoreCase("identity"))
      throw new AnalysisException(AnalysisErrorCode.LISTING_CONTENT_UNSUPPORTED);
    if (entity.getContentLength() > properties.maxResponseBytes())
      throw new AnalysisException(AnalysisErrorCode.LISTING_TOO_LARGE);
    InputStream input = entity.getContent();
    byte[] bytes = input.readNBytes(properties.maxResponseBytes() + 1);
    if (bytes.length > properties.maxResponseBytes())
      throw new AnalysisException(AnalysisErrorCode.LISTING_TOO_LARGE);
    return new Response(status, null, new String(bytes, StandardCharsets.UTF_8));
  }
}
