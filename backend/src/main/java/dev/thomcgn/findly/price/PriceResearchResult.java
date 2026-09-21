package dev.thomcgn.findly.price;

import dev.thomcgn.findly.provider.PriceQuote;
import java.util.List;

public record PriceResearchResult(List<PriceQuote> quotes, List<String> warnings) {
  public PriceResearchResult {
    quotes = List.copyOf(quotes);
    warnings = List.copyOf(warnings);
  }
}
