package dev.thomcgn.findly.analysis;

import dev.thomcgn.findly.common.validation.ListingUrl;
import dev.thomcgn.findly.error.AnalysisErrorCode;
import dev.thomcgn.findly.error.AnalysisException;
import dev.thomcgn.findly.listing.Listing;
import dev.thomcgn.findly.listing.ListingFetchService;
import dev.thomcgn.findly.price.DealScoreService;
import dev.thomcgn.findly.price.PriceResearchService;
import dev.thomcgn.findly.price.PriceSource;
import dev.thomcgn.findly.product.IdentifiedProduct;
import dev.thomcgn.findly.product.ProductIdentificationService;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisService {

  private final AnalysisRepository analysisRepository;
  private final AnalysisProcessingService analysisProcessingService;
  private final ProductIdentificationService productIdentificationService;
  private final PriceResearchService priceResearchService;
  private final ListingFetchService listingFetchService;
  private final DealScoreService dealScoreService = new DealScoreService();

  public AnalysisService(
      AnalysisRepository analysisRepository,
      AnalysisProcessingService analysisProcessingService,
      ProductIdentificationService productIdentificationService,
      PriceResearchService priceResearchService,
      ListingFetchService listingFetchService) {
    this.analysisRepository = analysisRepository;
    this.analysisProcessingService = analysisProcessingService;
    this.productIdentificationService = productIdentificationService;
    this.priceResearchService = priceResearchService;
    this.listingFetchService = listingFetchService;
  }

  @Transactional
  public AnalysisStartResponse createAnalysis(String rawUrl) {
    String url = ListingUrl.parse(rawUrl).toString();

    Analysis analysis =
        Analysis.builder()
            .status(AnalysisStatus.PENDING)
            .progress(0)
            .startedAt(null)
            .completedAt(null)
            .failedAt(null)
            .errorCode(null)
            .errorMessage(null)
            .build();
    Analysis persistedAnalysis = analysisRepository.save(analysis);
    analysisProcessingService.processAnalysisAsync(persistedAnalysis.getId(), url);
    return new AnalysisStartResponse(persistedAnalysis.getId(), persistedAnalysis.getStatus());
  }

  @Transactional(readOnly = true)
  public AnalysisStatusResponse getAnalysisStatus(UUID id) {
    Analysis analysis =
        analysisRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Analysis not found: " + id));

    AnalysisErrorCode failure =
        analysis.getStatus() == AnalysisStatus.FAILED ? failureCode(analysis) : null;
    return new AnalysisStatusResponse(
        analysis.getId(),
        analysis.getStatus(),
        analysis.getProgress(),
        analysis.getCreatedAt(),
        analysis.getUpdatedAt(),
        analysis.getStartedAt(),
        analysis.getCompletedAt(),
        analysis.getFailedAt(),
        failure == null ? null : failure.name(),
        failure == null ? null : failure.detail());
  }

  @Transactional(readOnly = true)
  public AnalysisDetailResponse getAnalysis(UUID id) {
    Analysis analysis =
        analysisRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Analysis not found: " + id));

    if (analysis.getStatus() == AnalysisStatus.FAILED) {
      throw new AnalysisException(failureCode(analysis));
    }
    if (analysis.getStatus() != AnalysisStatus.COMPLETED) {
      throw new AnalysisException(AnalysisErrorCode.RESULT_NOT_READY);
    }

    Listing listing = analysis.getListing();
    IdentifiedProduct product = analysis.getIdentifiedProduct();
    List<PriceSource> priceSources = analysis.getPriceSources();

    if (listing == null || product == null || priceSources == null || priceSources.isEmpty()) {
      throw new AnalysisException(AnalysisErrorCode.INTERNAL_ERROR);
    }

    BigDecimal marketMedian = medianPrice(priceSources);
    BigDecimal lowestPrice = minimumPrice(priceSources);
    BigDecimal highestPrice = maximumPrice(priceSources);

    String dealScore = dealScoreService.classify(listing.getListingPrice(), marketMedian);
    BigDecimal differencePercent =
        dealScoreService.calculateDifferencePercent(listing.getListingPrice(), marketMedian);

    return new AnalysisDetailResponse(
        analysis.getId(),
        analysis.getStatus(),
        new AnalysisDetailResponse.ListingSummary(
            listing.getTitle(),
            listing.getListingPrice(),
            listing.getCurrency(),
            listing.getExternalUrl(),
            listing.getImageUrls() == null ? List.of() : listing.getImageUrls()),
        new AnalysisDetailResponse.ProductSummary(
            product.getBrand(), product.getModel(), product.getCategory(), product.getConfidence()),
        new AnalysisDetailResponse.MarketSummary(marketMedian, lowestPrice, highestPrice),
        new AnalysisDetailResponse.DealSummary(dealScore, differencePercent));
  }

  private AnalysisErrorCode failureCode(Analysis analysis) {
    return switch (analysis.getErrorCode() == null ? "" : analysis.getErrorCode()) {
      case "LISTING_FETCH_FAILED" -> AnalysisErrorCode.LISTING_FETCH_FAILED;
      case "ANALYSIS_TIMEOUT" -> AnalysisErrorCode.ANALYSIS_TIMEOUT;
      default -> AnalysisErrorCode.ANALYSIS_FAILED;
    };
  }

  private BigDecimal detectListingPrice(String url) {
    String lower = url.toLowerCase();
    if (lower.contains("ikea") || lower.contains("malm")) {
      return BigDecimal.valueOf(129.00);
    }
    if (lower.contains("brille") || lower.contains("glasses") || lower.contains("sunglasses")) {
      return BigDecimal.valueOf(290.00);
    }
    return BigDecimal.valueOf(149.00);
  }

  private BigDecimal medianPrice(List<PriceSource> priceSources) {
    List<BigDecimal> values = priceSources.stream().map(PriceSource::getPrice).sorted().toList();
    int size = values.size();
    if (size == 0) {
      return BigDecimal.ZERO;
    }
    if (size % 2 == 0) {
      BigDecimal left = values.get(size / 2 - 1);
      BigDecimal right = values.get(size / 2);
      return left.add(right).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }
    return values.get(size / 2).setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal minimumPrice(List<PriceSource> priceSources) {
    return priceSources.stream()
        .map(PriceSource::getPrice)
        .min(Comparator.naturalOrder())
        .orElse(BigDecimal.ZERO)
        .setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal maximumPrice(List<PriceSource> priceSources) {
    return priceSources.stream()
        .map(PriceSource::getPrice)
        .max(Comparator.naturalOrder())
        .orElse(BigDecimal.ZERO)
        .setScale(2, RoundingMode.HALF_UP);
  }
}
