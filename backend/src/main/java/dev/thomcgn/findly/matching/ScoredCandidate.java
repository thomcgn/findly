package dev.thomcgn.findly.matching;

import dev.thomcgn.findly.provider.ProductCandidate;
import java.math.BigDecimal;
import java.util.Map;

public record ScoredCandidate(
    ProductCandidate product,
    BigDecimal confidence,
    Map<String, BigDecimal> components,
    boolean identifierConflict,
    boolean eligible,
    Map<String, BigDecimal> weights) {
  public ScoredCandidate(
      ProductCandidate product,
      BigDecimal confidence,
      Map<String, BigDecimal> components,
      boolean identifierConflict,
      boolean eligible) {
    this(product, confidence, components, identifierConflict, eligible, Map.of());
  }

  public ScoredCandidate {
    components = Map.copyOf(components);
    weights = Map.copyOf(weights);
  }
}
