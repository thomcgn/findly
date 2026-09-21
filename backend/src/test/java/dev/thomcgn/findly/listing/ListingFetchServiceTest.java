package dev.thomcgn.findly.listing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.thomcgn.findly.error.AnalysisErrorCode;
import dev.thomcgn.findly.error.AnalysisException;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class ListingFetchServiceTest {
  private final ListingTransport transport = mock(ListingTransport.class);
  private final ListingFetchService service =
      new ListingFetchService(transport, new ListingParser(JsonMapper.builder().build()));
  private static final String URL = "https://kleinanzeigen.de/a";

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://example.com/a",
        "https://127.0.0.1/a",
        "https://[::1]/a",
        "https://user@kleinanzeigen.de/a"
      })
  void rejectsInvalidTargetsBeforeAnyIo(String url) {
    assertThrows(AnalysisException.class, () -> service.fetch(url));
    verifyNoInteractions(transport);
  }

  @Test
  void followsThreeValidatedRelativeRedirects() {
    when(transport.get(URI.create(URL))).thenReturn(new ListingTransport.Response(302, "/b", null));
    when(transport.get(URI.create("https://kleinanzeigen.de/b")))
        .thenReturn(new ListingTransport.Response(307, "/c", null));
    when(transport.get(URI.create("https://kleinanzeigen.de/c")))
        .thenReturn(new ListingTransport.Response(301, "https://www.kleinanzeigen.de/d", null));
    when(transport.get(URI.create("https://www.kleinanzeigen.de/d")))
        .thenReturn(new ListingTransport.Response(200, null, "<h1 id=viewad-title>Fixture</h1>"));
    assertEquals("Fixture", service.fetch(URL).title());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://kleinanzeigen.de/b",
        "//127.0.0.1/b",
        "https://user@kleinanzeigen.de/b",
        "https://evil.example/b",
        "/a"
      })
  void refusesUnsafeRedirectsAndLoops(String location) {
    when(transport.get(URI.create(URL)))
        .thenReturn(new ListingTransport.Response(302, location, null));
    assertEquals(
        AnalysisErrorCode.LISTING_REDIRECT_FAILED,
        assertThrows(AnalysisException.class, () -> service.fetch(URL)).code());
    verify(transport, atMost(4)).get(any());
  }
}
