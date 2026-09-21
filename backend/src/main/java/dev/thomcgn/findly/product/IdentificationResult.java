package dev.thomcgn.findly.product;

import dev.thomcgn.findly.provider.ProductCandidate;
import java.util.List;

public record IdentificationResult(
    List<ProductCandidate> candidates,
    ProductCandidate selected,
    List<String> warnings,
    List<dev.thomcgn.findly.matching.ScoredCandidate> scores,
    List<dev.thomcgn.findly.provider.ProductEvidence> attributes) {
  public IdentificationResult(
      List<ProductCandidate> candidates, ProductCandidate selected, List<String> warnings) {
    this(candidates, selected, warnings, List.of(), List.of());
  }

  public IdentificationResult {
    candidates = List.copyOf(candidates);
    scores = List.copyOf(scores);
    attributes = List.copyOf(attributes);
    warnings = List.copyOf(warnings);
    if (selected != null && !candidates.contains(selected))
      throw new IllegalArgumentException("Selected product must be a candidate");
  }
}
