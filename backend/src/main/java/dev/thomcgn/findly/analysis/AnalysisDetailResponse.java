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
    DealSummary deal,
    List<String> warnings,
    List<dev.thomcgn.findly.matching.ScoredCandidate> candidates,
    List<dev.thomcgn.findly.provider.ProductEvidence> attributes,
    List<dev.thomcgn.findly.provider.PriceQuote> priceEvidence,
    List<PriceComparison> comparisons) {
  public AnalysisDetailResponse(
      UUID id,
      AnalysisStatus status,
      ListingSummary listing,
      ProductSummary product,
      MarketSummary market,
      DealSummary deal,
      List<String> warnings) {
    this(
        id, status, listing, product, market, deal, warnings, List.of(), List.of(), List.of(),
        List.of());
  }

  public record PriceComparison(
      dev.thomcgn.findly.provider.PriceQuote.Kind kind,
      String currency,
      BigDecimal referencePrice,
      BigDecimal savings,
      BigDecimal savingsPercent,
      String outcome,
      List<dev.thomcgn.findly.provider.PriceQuote> sources) {
    public PriceComparison {
      sources = List.copyOf(sources);
    }
  }

  public AnalysisDetailResponse {
    warnings = List.copyOf(warnings);
    candidates = List.copyOf(candidates);
    attributes = List.copyOf(attributes);
    priceEvidence = List.copyOf(priceEvidence);
    comparisons = List.copyOf(comparisons);
  }

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
