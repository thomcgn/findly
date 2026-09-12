package dev.thomcgn.findly.listing;

import java.math.BigDecimal;
import java.util.List;

public record ListingFetchResult(
        String title,
        String description,
        BigDecimal price,
        String currency,
        List<String> imageUrls
) {
}
