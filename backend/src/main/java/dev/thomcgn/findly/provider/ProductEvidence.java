package dev.thomcgn.findly.provider;

import java.net.URI;
import java.time.Instant;

public record ProductEvidence(String attribute, String value, URI source, Instant retrievedAt) {
  public ProductEvidence {
    if (attribute == null
        || attribute.isBlank()
        || value == null
        || value.isBlank()
        || source == null
        || !"https".equals(source.getScheme())
        || source.getHost() == null
        || source.getUserInfo() != null
        || retrievedAt == null) {
      throw new IllegalArgumentException(
          "Evidence requires an attribute, value, HTTPS source and retrieval time");
    }
  }
}
