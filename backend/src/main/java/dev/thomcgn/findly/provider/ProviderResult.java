package dev.thomcgn.findly.provider;

import java.util.List;

public record ProviderResult<T>(Availability availability, List<T> items) {
  public enum Availability {
    AVAILABLE,
    UNAVAILABLE
  }

  public ProviderResult {
    java.util.Objects.requireNonNull(availability);
    items = List.copyOf(items);
    if (availability == Availability.UNAVAILABLE && !items.isEmpty()) {
      throw new IllegalArgumentException("Unavailable providers cannot return results");
    }
  }

  public static <T> ProviderResult<T> unavailable() {
    return new ProviderResult<>(Availability.UNAVAILABLE, List.of());
  }
}
