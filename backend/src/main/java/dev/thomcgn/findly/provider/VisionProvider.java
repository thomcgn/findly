package dev.thomcgn.findly.provider;

import dev.thomcgn.findly.listing.ListingFetchResult;

public interface VisionProvider {
  ProviderResult<ProductEvidence> analyze(ListingFetchResult listing);
}
