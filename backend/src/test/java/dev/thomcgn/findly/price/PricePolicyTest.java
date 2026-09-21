package dev.thomcgn.findly.price;

import static org.junit.jupiter.api.Assertions.*;

import dev.thomcgn.findly.provider.PriceQuote;
import java.math.BigDecimal;
import java.net.URI;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class PricePolicyTest {
  private final Instant now = Instant.parse("2026-09-19T10:00:00Z");
  private final PricePolicy policy = new PricePolicy(Clock.fixed(now, ZoneOffset.UTC));

  private PriceQuote quote(
      String amount,
      String currency,
      PriceQuote.Kind kind,
      PriceQuote.SourceType type,
      Instant time,
      String confidence) {
    return new PriceQuote(
        new BigDecimal(amount),
        currency,
        "Fixture",
        URI.create("https://fixture.test/price"),
        time,
        kind,
        new BigDecimal(confidence),
        type);
  }

  @Test
  void rejectsUnusableUntrustedStaleAndFuturePrices() {
    for (var amount : List.of("0", "0.00001", "1234567890123456789", "1E+30"))
      assertFalse(
          policy.accepts(
              quote(
                  amount,
                  "EUR",
                  PriceQuote.Kind.ORIGINAL,
                  PriceQuote.SourceType.MANUFACTURER,
                  now,
                  "1")));
    for (var time : List.of(now.plusSeconds(1), now.minus(Duration.ofDays(30))))
      assertFalse(
          policy.accepts(
              quote(
                  "100",
                  "EUR",
                  PriceQuote.Kind.ORIGINAL,
                  PriceQuote.SourceType.MANUFACTURER,
                  time,
                  "1")));
    assertFalse(
        policy.accepts(
            quote("100", "EUR", PriceQuote.Kind.ORIGINAL, PriceQuote.SourceType.OTHER, now, "1")));
    assertFalse(
        policy.accepts(
            quote(
                "100",
                "EUR",
                PriceQuote.Kind.ORIGINAL,
                PriceQuote.SourceType.MANUFACTURER,
                now,
                ".749")));
    assertTrue(
        policy.accepts(
            quote(
                "100",
                "EUR",
                PriceQuote.Kind.ORIGINAL,
                PriceQuote.SourceType.MANUFACTURER,
                now,
                ".75")));
  }

  @Test
  void separatesKindsCurrenciesAndSourcePriority() {
    var manufacturer =
        quote("200", "EUR", PriceQuote.Kind.ORIGINAL, PriceQuote.SourceType.MANUFACTURER, now, "1");
    var quotes =
        List.of(
            manufacturer,
            quote("90", "EUR", PriceQuote.Kind.ORIGINAL, PriceQuote.SourceType.RETAILER, now, "1"),
            quote(
                "120",
                "USD",
                PriceQuote.Kind.ORIGINAL,
                PriceQuote.SourceType.MANUFACTURER,
                now,
                "1"),
            quote(
                "50",
                "EUR",
                PriceQuote.Kind.CURRENT_USED,
                PriceQuote.SourceType.MARKETPLACE,
                now,
                "1"));
    assertEquals(
        List.of(manufacturer), PricePolicy.references(quotes, PriceQuote.Kind.ORIGINAL, "EUR"));
    assertTrue(
        PricePolicy.references(quotes, PriceQuote.Kind.HISTORICAL_ORIGINAL, "EUR").isEmpty());
    assertTrue(PricePolicy.references(quotes, PriceQuote.Kind.ORIGINAL, "GBP").isEmpty());
  }

  @Test
  void medianUsesDecimalArithmeticAndUnknownRemainsNull() {
    assertNull(PricePolicy.median(List.of()));
    var a =
        quote(
            "10.10",
            "EUR",
            PriceQuote.Kind.CURRENT_USED,
            PriceQuote.SourceType.MARKETPLACE,
            now,
            "1");
    var b =
        quote(
            "20.20",
            "EUR",
            PriceQuote.Kind.CURRENT_USED,
            PriceQuote.SourceType.MARKETPLACE,
            now,
            "1");
    assertEquals(new BigDecimal("15.15"), PricePolicy.median(List.of(b, a)));
  }
}
