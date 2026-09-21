package dev.thomcgn.findly.listing;

import static org.junit.jupiter.api.Assertions.*;

import dev.thomcgn.findly.error.AnalysisException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PublicAddressResolverTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "0.0.0.0",
        "127.1.2.3",
        "10.1.2.3",
        "172.16.0.1",
        "192.168.1.2",
        "169.254.169.254",
        "100.100.100.200",
        "224.1.2.3",
        "255.255.255.255",
        "198.18.0.1",
        "::",
        "::1",
        "::ffff:127.0.0.1",
        "fc00::1",
        "fd00:ec2::254",
        "fe80::1",
        "ff02::1",
        "2001:db8::1",
        "2002:7f00:1::1",
        "64:ff9b::7f00:1"
      })
  void rejectsNonPublicIpv4AndIpv6(String address) throws Exception {
    var resolver =
        new PublicAddressResolver(host -> new InetAddress[] {InetAddress.getByName(address)});
    assertThrows(AnalysisException.class, () -> resolver.resolve("kleinanzeigen.de"));
  }

  @Test
  void rejectsMixedAnswersAndEmptyAnswers() throws Exception {
    var resolver =
        new PublicAddressResolver(
            host ->
                new InetAddress[] {
                  InetAddress.getByName("8.8.8.8"), InetAddress.getByName("127.0.0.1")
                });
    assertThrows(AnalysisException.class, () -> resolver.resolve("kleinanzeigen.de"));
    assertThrows(
        UnknownHostException.class,
        () -> new PublicAddressResolver(host -> new InetAddress[0]).resolve("kleinanzeigen.de"));
  }

  @Test
  void validatesEveryResolutionAndReturnsBoundAddressObjects() throws Exception {
    AtomicInteger calls = new AtomicInteger();
    var resolver =
        new PublicAddressResolver(
            host ->
                new InetAddress[] {
                  InetAddress.getByName(
                      calls.getAndIncrement() == 0 ? "2606:4700:4700::1111" : "127.0.0.1")
                });
    assertEquals(
        "2606:4700:4700:0:0:0:0:1111", resolver.resolve("kleinanzeigen.de")[0].getHostAddress());
    assertThrows(AnalysisException.class, () -> resolver.resolve("kleinanzeigen.de"));
    assertEquals(2, calls.get());
  }
}
