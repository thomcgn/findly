package dev.thomcgn.findly.provider;

import java.util.List;
import java.util.Map;

public record ProductCandidate(
    String name,
    String brand,
    String model,
    String category,
    Map<String, String> identifiers,
    List<ProductEvidence> evidence) {
  public ProductCandidate {
    identifiers = Map.copyOf(identifiers);
    evidence = List.copyOf(evidence);
    if (name == null || name.isBlank() || evidence.isEmpty())
      throw new IllegalArgumentException("Candidates require a name and evidence");
  }
}
