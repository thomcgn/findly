package dev.thomcgn.findly.provider;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Currency;

public record PriceQuote(
    BigDecimal amount,
    String currency,
    String sourceName,
    URI source,
    Instant retrievedAt,
    Kind kind,
    BigDecimal confidence,
    SourceType sourceType) {
  public enum SourceType {
    MANUFACTURER,
    RETAILER,
    MARKETPLACE,
    OTHER
  }

  public PriceQuote(
      BigDecimal amount,
      String currency,
      String sourceName,
      URI source,
      Instant retrievedAt,
      Kind kind,
      BigDecimal confidence) {
    this(amount, currency, sourceName, source, retrievedAt, kind, confidence, SourceType.OTHER);
  }

  public enum Kind {
    ORIGINAL,
    HISTORICAL_ORIGINAL,
    CURRENT_USED
  }

  public PriceQuote {
    if (sourceType == null
        || amount == null
        || amount.signum() < 0
        || currency == null
        || sourceName == null
        || sourceName.isBlank()
        || source == null
        || !"https".equals(source.getScheme())
        || source.getHost() == null
        || source.getUserInfo() != null
        || retrievedAt == null
        || kind == null
        || confidence == null
        || confidence.signum() < 0
        || confidence.compareTo(BigDecimal.ONE) > 0) {
      throw new IllegalArgumentException("Price quotes require complete, valid evidence");
    }
    Currency.getInstance(currency);
  }
}
