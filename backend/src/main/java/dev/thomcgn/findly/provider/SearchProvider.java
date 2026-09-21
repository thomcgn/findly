package dev.thomcgn.findly.provider;

import dev.thomcgn.findly.listing.ListingFetchResult;
import java.util.List;

public interface SearchProvider {
  ProviderResult<ProductCandidate> search(
      ListingFetchResult listing, List<ProductEvidence> evidence);
}
