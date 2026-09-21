package dev.thomcgn.findly.listing;

import static org.junit.jupiter.api.Assertions.*;

import dev.thomcgn.findly.error.AnalysisErrorCode;
import dev.thomcgn.findly.error.AnalysisException;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ApacheListingTransportTest {
  private final ApacheListingTransport transport =
      new ApacheListingTransport(
          new ListingClientProperties(100, 100, 32),
          new PublicAddressResolver(InetAddress::getAllByName));

  @Test
  void rejectsOversizedUnknownLengthStreamAfterLimitPlusOneBytes() throws Exception {
    var input = new ByteArrayInputStream(new byte[1000]);
    try (var response = new BasicClassicHttpResponse(200)) {
      response.setEntity(new InputStreamEntity(input, -1, ContentType.TEXT_HTML));
      assertEquals(
          AnalysisErrorCode.LISTING_TOO_LARGE,
          assertThrows(AnalysisException.class, () -> transport.readResponse(response)).code());
      assertEquals(967, input.available());
    }
  }

  @Test
  void acceptsExactLimitAndRejectsMissingContentType() throws Exception {
    try (var response = new BasicClassicHttpResponse(200)) {
      response.setEntity(
          new InputStreamEntity(
              new ByteArrayInputStream("a".repeat(32).getBytes(StandardCharsets.UTF_8)),
              32,
              ContentType.TEXT_HTML));
      assertEquals(32, transport.readResponse(response).html().length());
    }
    try (var response = new BasicClassicHttpResponse(200)) {
      response.setEntity(new InputStreamEntity(new ByteArrayInputStream(new byte[0]), 0, null));
      assertEquals(
          AnalysisErrorCode.LISTING_CONTENT_UNSUPPORTED,
          assertThrows(AnalysisException.class, () -> transport.readResponse(response)).code());
    }
  }

  @ParameterizedTest
  @CsvSource({
    "401,LISTING_ACCESS_BLOCKED",
    "403,LISTING_ACCESS_BLOCKED",
    "429,LISTING_ACCESS_BLOCKED",
    "404,LISTING_FETCH_FAILED",
    "500,LISTING_FETCH_FAILED"
  })
  void mapsHttpErrorsWithoutReadingTheirBody(int status, AnalysisErrorCode code) throws Exception {
    try (var response = new BasicClassicHttpResponse(status)) {
      assertEquals(
          code,
          assertThrows(AnalysisException.class, () -> transport.readResponse(response)).code());
    }
  }
}
