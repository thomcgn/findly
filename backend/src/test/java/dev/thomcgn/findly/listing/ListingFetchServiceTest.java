package dev.thomcgn.findly.listing;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ListingFetchServiceTest {

  private final ListingFetchService service = new ListingFetchService();

  @Test
  void rejectsNonKleinanzeigenHosts() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.fetch("https://example.com/s-anzeige/test"));
  }

  @Test
  void rejectsPrivateIpAndLocalHosts() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.fetch("https://127.0.0.1/s-anzeige/test"));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.fetch("https://localhost/s-anzeige/test"));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.fetch("https://[::1]/s-anzeige/test"));
  }
}
