package dev.thomcgn.findly.provider;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProviderDtoTest {
  @Test
  void unavailableCannotCarryResultsAndDomainListsAreImmutable() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ProviderResult<>(ProviderResult.Availability.UNAVAILABLE, List.of("invented")));
    var items = new ArrayList<String>(List.of("evidence"));
    var result = new ProviderResult<>(ProviderResult.Availability.AVAILABLE, items);
    items.clear();
    assertEquals(List.of("evidence"), result.items());
    assertThrows(UnsupportedOperationException.class, () -> result.items().clear());
  }

  @Test
  void candidatesRequireSourceEvidence() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ProductCandidate("Model", null, null, null, Map.of(), List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ProductEvidence("model", "Model", null, Instant.EPOCH));
  }

  @Test
  void priceQuotesRequireValidatedCompleteEvidence() {
    URI source = URI.create("https://example.test/offer");
    var valid =
        new PriceQuote(
            BigDecimal.TEN,
            "EUR",
            "Fixture",
            source,
            Instant.EPOCH,
            PriceQuote.Kind.CURRENT_USED,
            BigDecimal.ONE);
    assertEquals(new BigDecimal("10"), valid.amount());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PriceQuote(
                BigDecimal.TEN,
                "EUR",
                "",
                source,
                Instant.EPOCH,
                PriceQuote.Kind.CURRENT_USED,
                BigDecimal.ONE));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PriceQuote(
                BigDecimal.TEN,
                "ZZZ",
                "Fixture",
                source,
                Instant.EPOCH,
                PriceQuote.Kind.CURRENT_USED,
                BigDecimal.ONE));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PriceQuote(
                BigDecimal.TEN.negate(),
                "EUR",
                "Fixture",
                source,
                Instant.EPOCH,
                PriceQuote.Kind.CURRENT_USED,
                BigDecimal.ONE));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PriceQuote(
                BigDecimal.TEN,
                "EUR",
                "Fixture",
                source,
                Instant.EPOCH,
                PriceQuote.Kind.CURRENT_USED,
                BigDecimal.TEN));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PriceQuote(
                BigDecimal.TEN,
                "EUR",
                "Fixture",
                URI.create("http://example.test"),
                Instant.EPOCH,
                PriceQuote.Kind.CURRENT_USED,
                BigDecimal.ONE));
  }
}
