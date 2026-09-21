package dev.thomcgn.findly.price;

import dev.thomcgn.findly.provider.PricingProvider;
import dev.thomcgn.findly.provider.ProductCandidate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PriceResearchService {
  private final PricingProvider provider;
  private final PricePolicy policy;

  public PriceResearchService(PricingProvider provider, PricePolicy policy) {
    this.provider = provider;
    this.policy = policy;
  }

  public PriceResearchResult research(ProductCandidate selected) {
    if (selected == null) return new PriceResearchResult(List.of(), List.of("PRICE_NOT_VERIFIED"));
    var result = provider.research(selected);
    var accepted = result.items().stream().filter(policy::accepts).distinct().toList();
    var warnings = new java.util.ArrayList<String>();
    if (accepted.isEmpty()) warnings.add("PRICE_NOT_VERIFIED");
    if (accepted.size() != result.items().size()) warnings.add("PRICE_EVIDENCE_REJECTED");
    return new PriceResearchResult(accepted, warnings);
  }
}
