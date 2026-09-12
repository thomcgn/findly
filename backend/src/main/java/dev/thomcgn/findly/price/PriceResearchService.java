package dev.thomcgn.findly.price;

import dev.thomcgn.findly.analysis.Analysis;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PriceResearchService {

  public List<PriceSource> findComparablePrices(Analysis analysis) {
    BigDecimal listingPrice =
        analysis.getListing() != null ? analysis.getListing().getListingPrice() : BigDecimal.ZERO;
    if (listingPrice == null || listingPrice.compareTo(BigDecimal.ZERO) <= 0) {
      listingPrice = BigDecimal.valueOf(149.00);
    }

    BigDecimal delta = BigDecimal.valueOf(0.15);
    BigDecimal lower = listingPrice.multiply(BigDecimal.ONE.subtract(delta));
    BigDecimal median = listingPrice;
    BigDecimal higher = listingPrice.multiply(BigDecimal.ONE.add(delta));

    List<PriceSource> sources = new ArrayList<>();
    sources.add(
        PriceSource.builder()
            .analysis(analysis)
            .sourceName("Marktvergleich")
            .productTitle(
                analysis.getListing() != null ? analysis.getListing().getTitle() : "Produkt")
            .price(lower.setScale(2, java.math.RoundingMode.HALF_UP))
            .currency("EUR")
            .url(
                analysis.getListing() != null
                    ? analysis.getListing().getExternalUrl()
                    : "https://example.com")
            .condition(PriceSourceCondition.USED)
            .build());
    sources.add(
        PriceSource.builder()
            .analysis(analysis)
            .sourceName("Aktueller Preis")
            .productTitle(
                analysis.getListing() != null ? analysis.getListing().getTitle() : "Produkt")
            .price(median.setScale(2, java.math.RoundingMode.HALF_UP))
            .currency("EUR")
            .url(
                analysis.getListing() != null
                    ? analysis.getListing().getExternalUrl()
                    : "https://example.com")
            .condition(PriceSourceCondition.USED)
            .build());
    sources.add(
        PriceSource.builder()
            .analysis(analysis)
            .sourceName("Oberes Preisband")
            .productTitle(
                analysis.getListing() != null ? analysis.getListing().getTitle() : "Produkt")
            .price(higher.setScale(2, java.math.RoundingMode.HALF_UP))
            .currency("EUR")
            .url(
                analysis.getListing() != null
                    ? analysis.getListing().getExternalUrl()
                    : "https://example.com")
            .condition(PriceSourceCondition.NEW)
            .build());

    return sources;
  }
}
