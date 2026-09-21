package dev.thomcgn.findly.product;

import dev.thomcgn.findly.listing.ListingFetchResult;
import dev.thomcgn.findly.provider.OcrProvider;
import dev.thomcgn.findly.provider.ProductEvidence;
import dev.thomcgn.findly.provider.ProviderResult.Availability;
import dev.thomcgn.findly.provider.SearchProvider;
import dev.thomcgn.findly.provider.VisionProvider;
import java.util.ArrayList;
import org.springframework.stereotype.Service;

@Service
public class ProductIdentificationService {
  private final dev.thomcgn.findly.matching.ProductMatcher matcher;
  private final OcrProvider ocr;
  private final VisionProvider vision;
  private final SearchProvider search;

  public ProductIdentificationService(
      OcrProvider ocr,
      VisionProvider vision,
      SearchProvider search,
      dev.thomcgn.findly.matching.ProductMatcher matcher) {
    this.matcher = matcher;
    this.ocr = ocr;
    this.vision = vision;
    this.search = search;
  }

  public IdentificationResult identify(ListingFetchResult listing) {
    var warnings = new ArrayList<String>();
    var evidence = new ArrayList<ProductEvidence>();
    var ocrResult = ocr.extract(listing);
    if (ocrResult.availability() == Availability.UNAVAILABLE) warnings.add("OCR_UNAVAILABLE");
    evidence.addAll(ocrResult.items());
    var visionResult = vision.analyze(listing);
    if (visionResult.availability() == Availability.UNAVAILABLE) warnings.add("VISION_UNAVAILABLE");
    evidence.addAll(visionResult.items());
    var candidates = search.search(listing, java.util.List.copyOf(evidence));
    if (candidates.availability() == Availability.UNAVAILABLE) warnings.add("SEARCH_UNAVAILABLE");
    var scores = matcher.rank(candidates.items(), evidence);
    var selected = matcher.select(scores);
    if (selected == null) warnings.add("PRODUCT_NOT_IDENTIFIED");
    return new IdentificationResult(candidates.items(), selected, warnings, scores, evidence);
  }
}
