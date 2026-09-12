package dev.thomcgn.findly.analysis;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AnalysisDetailResponse(
    UUID id,
    AnalysisStatus status,
    ListingSummary listing,
    ProductSummary product,
    MarketSummary market,
    DealSummary deal) {
  public record ListingSummary(
      String title, BigDecimal price, String currency, String url, List<String> imageUrls) {
    public ListingSummary {
      imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }
  }

  public record ProductSummary(
      String brand, String model, String category, BigDecimal confidence) {}

  public record MarketSummary(
      BigDecimal medianPrice, BigDecimal lowestPrice, BigDecimal highestPrice) {}

  public record DealSummary(String score, BigDecimal differencePercent) {}
}
