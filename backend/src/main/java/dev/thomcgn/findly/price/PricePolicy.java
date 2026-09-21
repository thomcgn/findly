package dev.thomcgn.findly.price;

import dev.thomcgn.findly.provider.PriceQuote;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class PricePolicy {
  private final Clock clock;

  public PricePolicy(Clock clock) {
    this.clock = clock;
  }

  public boolean accepts(PriceQuote q) {
    return q.amount().signum() > 0
        && q.amount().precision() <= 18
        && (long) q.amount().precision() - q.amount().scale() <= 18
        && q.amount().scale() <= 4
        && q.confidence().compareTo(new BigDecimal("0.75")) >= 0
        && !q.retrievedAt().isAfter(clock.instant())
        && q.retrievedAt().isAfter(clock.instant().minus(Duration.ofDays(30)))
        && q.sourceType() != PriceQuote.SourceType.OTHER;
  }

  public static int priority(PriceQuote q) {
    return switch (q.kind()) {
      case ORIGINAL, HISTORICAL_ORIGINAL ->
          switch (q.sourceType()) {
            case MANUFACTURER -> 0;
            case RETAILER -> 1;
            case MARKETPLACE -> 2;
            case OTHER -> 3;
          };
      case CURRENT_USED ->
          switch (q.sourceType()) {
            case MARKETPLACE -> 0;
            case RETAILER -> 1;
            case MANUFACTURER -> 2;
            case OTHER -> 3;
          };
    };
  }

  public static List<PriceQuote> references(
      List<PriceQuote> quotes, PriceQuote.Kind kind, String currency) {
    var compatible =
        quotes.stream().filter(q -> q.kind() == kind && q.currency().equals(currency)).toList();
    int best = compatible.stream().mapToInt(PricePolicy::priority).min().orElse(4);
    return compatible.stream()
        .filter(q -> priority(q) == best)
        .sorted(Comparator.comparing(PriceQuote::amount))
        .toList();
  }

  public static BigDecimal median(List<PriceQuote> quotes) {
    if (quotes.isEmpty()) return null;
    var values = quotes.stream().map(PriceQuote::amount).sorted().toList();
    int n = values.size();
    return n % 2 == 1
        ? values.get(n / 2)
        : values.get(n / 2 - 1).add(values.get(n / 2)).divide(BigDecimal.TWO);
  }
}
