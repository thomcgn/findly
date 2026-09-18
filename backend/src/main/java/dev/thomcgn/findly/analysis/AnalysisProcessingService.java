package dev.thomcgn.findly.analysis;

import dev.thomcgn.findly.listing.Listing;
import dev.thomcgn.findly.listing.ListingFetchResult;
import dev.thomcgn.findly.listing.ListingFetchService;
import dev.thomcgn.findly.price.DealScoreService;
import dev.thomcgn.findly.price.PriceResearchService;
import dev.thomcgn.findly.price.PriceSource;
import dev.thomcgn.findly.product.IdentifiedProduct;
import dev.thomcgn.findly.product.ProductIdentificationService;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisProcessingService {

  private final AnalysisRepository analysisRepository;
  private final ProductIdentificationService productIdentificationService;
  private final PriceResearchService priceResearchService;
  private final ListingFetchService listingFetchService;
  private final DealScoreService dealScoreService = new DealScoreService();

  public AnalysisProcessingService(
      AnalysisRepository analysisRepository,
      ProductIdentificationService productIdentificationService,
      PriceResearchService priceResearchService,
      ListingFetchService listingFetchService) {
    this.analysisRepository = analysisRepository;
    this.productIdentificationService = productIdentificationService;
    this.priceResearchService = priceResearchService;
    this.listingFetchService = listingFetchService;
  }

  @Async("analysisTaskExecutor")
  public void processAnalysisAsync(UUID analysisId, String rawUrl) {
    processAnalysis(analysisId, rawUrl);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void processAnalysis(UUID analysisId, String rawUrl) {
    Analysis analysis =
        analysisRepository
            .findById(analysisId)
            .orElseThrow(() -> new EntityNotFoundException("Analysis not found: " + analysisId));

    try {
      analysis.setStatus(AnalysisStatus.ANALYZING);
      analysis.setProgress(10);
      analysis.setStartedAt(Instant.now());
      analysis.setErrorCode(null);
      analysis.setErrorMessage(null);
      analysisRepository.save(analysis);

      ListingFetchResult listingFetchResult = listingFetchService.fetch(rawUrl);
      BigDecimal listingPrice = listingFetchResult.price();
      Listing listing =
          Listing.builder()
              .analysis(analysis)
              .externalUrl(rawUrl)
              .title(listingFetchResult.title())
              .description(listingFetchResult.description())
              .listingPrice(listingPrice)
              .currency(listingFetchResult.currency())
              .imageUrls(new java.util.ArrayList<>(listingFetchResult.imageUrls()))
              .build();
      analysis.setListing(listing);
      analysis.setProgress(40);
      analysisRepository.save(analysis);

      IdentifiedProduct product = productIdentificationService.identify(analysis, listing);
      analysis.setIdentifiedProduct(product);
      analysis.setProgress(70);
      analysisRepository.save(analysis);

      List<PriceSource> priceSources = priceResearchService.findComparablePrices(analysis);
      priceSources.forEach(priceSource -> priceSource.setAnalysis(analysis));
      analysis.setPriceSources(new java.util.ArrayList<>(priceSources));
      analysis.setProgress(90);
      analysisRepository.save(analysis);

      analysis.setStatus(AnalysisStatus.COMPLETED);
      analysis.setProgress(100);
      analysis.setCompletedAt(Instant.now());
      analysisRepository.save(analysis);
    } catch (Exception ex) {
      analysis.setStatus(AnalysisStatus.FAILED);
      analysis.setProgress(100);
      analysis.setFailedAt(Instant.now());
      analysis.setErrorCode("ANALYSIS_FAILED");
      analysis.setErrorMessage(ex.getMessage());
      analysisRepository.save(analysis);
    }
  }
}
