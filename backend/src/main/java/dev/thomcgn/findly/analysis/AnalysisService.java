package dev.thomcgn.findly.analysis;

import dev.thomcgn.findly.common.validation.ListingUrl;
import dev.thomcgn.findly.error.AnalysisErrorCode;
import dev.thomcgn.findly.error.AnalysisException;
import dev.thomcgn.findly.listing.Listing;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisService {

  private final AnalysisRepository analysisRepository;
  private final AnalysisProcessingService analysisProcessingService;
  private final AnalysisStatusService statuses;

  public AnalysisService(
      AnalysisRepository analysisRepository,
      AnalysisProcessingService analysisProcessingService,
      AnalysisStatusService statuses) {
    this.analysisRepository = analysisRepository;
    this.analysisProcessingService = analysisProcessingService;
    this.statuses = statuses;
  }

  public AnalysisStartResponse createAnalysis(String rawUrl) {
    String url = ListingUrl.parse(rawUrl).toString();
    var job = statuses.create(url);
    analysisProcessingService.schedule(job);
    return new AnalysisStartResponse(job.id(), AnalysisStatus.CREATED);
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
        failure == null ? null : failure.detail(),
        analysis.getWarnings());
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
    if (listing == null) throw new AnalysisException(AnalysisErrorCode.INTERNAL_ERROR);
    var warnings = new java.util.ArrayList<>(analysis.getWarnings());
    boolean legacy = warnings.contains("LEGACY_RESULT_UNVERIFIED");
    var selected =
        legacy
            ? null
            : analysis.getMatches().stream()
                .filter(dev.thomcgn.findly.product.ProductMatch::isSelected)
                .findFirst()
                .orElse(null);
    var quotes =
        legacy
            ? java.util.List.<dev.thomcgn.findly.provider.PriceQuote>of()
            : analysis.getPriceEvidence().stream()
                .map(dev.thomcgn.findly.price.PriceEvidence::getQuote)
                .toList();
    if (selected == null && !warnings.contains("PRODUCT_NOT_IDENTIFIED"))
      warnings.add("PRODUCT_NOT_IDENTIFIED");
    if (quotes.isEmpty() && !warnings.contains("PRICE_NOT_VERIFIED"))
      warnings.add("PRICE_NOT_VERIFIED");
    var comparisons = new java.util.ArrayList<AnalysisDetailResponse.PriceComparison>();
    if (!legacy && listing.getCurrency() != null)
      for (var kind : dev.thomcgn.findly.provider.PriceQuote.Kind.values()) {
        var sources =
            dev.thomcgn.findly.price.PricePolicy.references(quotes, kind, listing.getCurrency());
        var reference = dev.thomcgn.findly.price.PricePolicy.median(sources);
        if (reference == null) continue;
        var savings =
            listing.getListingPrice() == null
                ? null
                : reference.subtract(listing.getListingPrice());
        var percent =
            savings == null
                ? null
                : savings
                    .multiply(java.math.BigDecimal.valueOf(100))
                    .divide(reference, 2, java.math.RoundingMode.HALF_UP);
        comparisons.add(
            new AnalysisDetailResponse.PriceComparison(
                kind,
                listing.getCurrency(),
                reference,
                savings,
                percent,
                savings == null
                    ? null
                    : savings.signum() < 0
                        ? "SURCHARGE"
                        : savings.signum() > 0 ? "SAVINGS" : "EQUAL",
                sources));
      }
    var used =
        dev.thomcgn.findly.price.PricePolicy.references(
            quotes,
            dev.thomcgn.findly.provider.PriceQuote.Kind.CURRENT_USED,
            listing.getCurrency());
    var product = selected == null ? null : selected.getProduct().getCandidate();

    return new AnalysisDetailResponse(
        analysis.getId(),
        analysis.getStatus(),
        new AnalysisDetailResponse.ListingSummary(
            listing.getTitle(),
            legacy ? null : listing.getListingPrice(),
            legacy ? null : listing.getCurrency(),
            listing.getExternalUrl(),
            listing.getImageUrls() == null ? List.of() : listing.getImageUrls()),
        product == null
            ? null
            : new AnalysisDetailResponse.ProductSummary(
                product.brand(),
                product.model(),
                product.category(),
                selected.getScore().confidence()),
        new AnalysisDetailResponse.MarketSummary(
            dev.thomcgn.findly.price.PricePolicy.median(used),
            used.isEmpty() ? null : used.getFirst().amount(),
            used.isEmpty() ? null : used.getLast().amount()),
        new AnalysisDetailResponse.DealSummary(null, null),
        warnings,
        legacy
            ? List.of()
            : analysis.getMatches().stream()
                .map(dev.thomcgn.findly.product.ProductMatch::getScore)
                .toList(),
        legacy
            ? List.of()
            : analysis.getAttributes().stream()
                .map(dev.thomcgn.findly.product.ExtractedAttribute::getEvidence)
                .toList(),
        quotes,
        comparisons);
  }

  private AnalysisErrorCode failureCode(Analysis analysis) {
    if (analysis.getErrorCode() == null) return AnalysisErrorCode.ANALYSIS_FAILED;
    try {
      return AnalysisErrorCode.valueOf(analysis.getErrorCode());
    } catch (IllegalArgumentException ex) {
      return AnalysisErrorCode.ANALYSIS_FAILED;
    }
  }
}
