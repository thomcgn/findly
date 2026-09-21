package dev.thomcgn.findly.provider;

import dev.thomcgn.findly.listing.ListingFetchResult;

public interface OcrProvider {
  ProviderResult<ProductEvidence> extract(ListingFetchResult listing);
}
