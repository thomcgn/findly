package dev.thomcgn.findly.common.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.thomcgn.findly.error.AnalysisErrorCode;
import dev.thomcgn.findly.error.AnalysisException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ListingUrlTest {
  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        " ",
        "http://kleinanzeigen.de/a",
        "https://example.com/a",
        "https://kleinanzeigen.de.evil.example/a",
        "https://user@kleinanzeigen.de/a",
        "https://@kleinanzeigen.de/a",
        "https://kleinanzeigen.de:8080/a",
        "https://kleinanzeigen.de/a#fragment",
        "https://127.0.0.1/a",
        "https://[::1]/a",
        "https://kleinanzeigen.de./a",
        "https://%6bleinanzeigen.de/a",
        "https://kleinanzeigen。de/a",
        "https://xn--kleinanzeign-9db.de/a",
        "https://kleinanzeigen.de\\@evil.example/a",
        "https://kleinanzeigen.de/a\nheader",
        "https://kleinanzeigen.de",
        "https://kleinanzeigen.de/%zz"
      })
  void rejectsInvalidUrlsWithoutDns(String input) {
    assertEquals(
        AnalysisErrorCode.INVALID_URL,
        assertThrows(AnalysisException.class, () -> ListingUrl.parse(input)).code());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://kleinanzeigen.de/a",
        "https://www.kleinanzeigen.de/a",
        "https://kleinanzeigen.de:80/a",
        "https://kleinanzeigen.de:443/a"
      })
  void acceptsOnlyAllowedSchemeHostsAndPorts(String input) {
    assertEquals(input, ListingUrl.parse(input).toString());
  }

  @Test
  void canonicalizesHostWithoutDecodingPathOrQuery() {
    assertEquals(
        "https://www.kleinanzeigen.de/a%2Fb?q=x%26y",
        ListingUrl.parse(" HTTPS://WWW.KLEINANZEIGEN.DE/a%2Fb?q=x%26y ").toString());
  }

  @Test
  void enforcesLengthBeforeTrimming() {
    String prefix = "https://kleinanzeigen.de/";
    String longest = prefix + "a".repeat(2048 - prefix.length());
    assertEquals(longest, ListingUrl.parse(longest).toString());
    assertThrows(AnalysisException.class, () -> ListingUrl.parse(longest + "a"));
  }
}
