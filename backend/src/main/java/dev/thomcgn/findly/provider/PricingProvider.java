package dev.thomcgn.findly.provider;

public interface PricingProvider {
  ProviderResult<PriceQuote> research(ProductCandidate product);
}
