package dev.thomcgn.findly.matching;

import dev.thomcgn.findly.provider.*;
import java.math.*;
import java.text.Normalizer;
import java.util.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(MatchingProperties.class)
public class ProductMatcher {
  private final MatchingProperties properties;

  public ProductMatcher(MatchingProperties properties) {
    this.properties = properties;
  }

  public static String normalize(String value) {
    return value == null
        ? ""
        : Normalizer.normalize(value, Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{N}]+", " ")
            .strip()
            .replaceAll(" +", " ");
  }

  public static boolean validEan(String value) {
    if (value == null || !value.matches("[0-9]{8}|[0-9]{13}")) return false;
    int sum = 0;
    for (int i = value.length() - 2, weight = 3; i >= 0; i--, weight = 4 - weight)
      sum += (value.charAt(i) - '0') * weight;
    return (10 - sum % 10) % 10 == value.charAt(value.length() - 1) - '0';
  }

  public List<ScoredCandidate> rank(
      List<ProductCandidate> candidates, List<ProductEvidence> evidence) {
    return candidates.stream()
        .distinct()
        .map(c -> score(c, evidence))
        .sorted(
            Comparator.comparing(ScoredCandidate::confidence)
                .reversed()
                .thenComparing(c -> c.product().name()))
        .toList();
  }

  public ProductCandidate select(List<ScoredCandidate> ranked) {
    if (ranked.isEmpty()) return null;
    var best = ranked.getFirst();
    if (best.identifierConflict()
        || !best.eligible()
        || best.confidence().compareTo(properties.minimumConfidence()) < 0) return null;
    if (ranked.size() > 1
        && best.confidence()
                .subtract(ranked.get(1).confidence())
                .compareTo(properties.minimumMargin())
            < 0) return null;
    return best.product();
  }

  private ScoredCandidate score(ProductCandidate c, List<ProductEvidence> evidence) {
    Map<String, String> values = new HashMap<>(c.identifiers());
    if (c.brand() != null) values.put("brand", c.brand());
    if (c.model() != null) values.put("model", c.model());
    if (c.category() != null) values.put("category", c.category());
    Map<String, BigDecimal> weights =
        Map.of(
            "ean",
            properties.eanWeight(),
            "sku",
            properties.skuWeight(),
            "model",
            properties.modelWeight(),
            "brand",
            properties.brandWeight(),
            "category",
            properties.categoryWeight());
    Map<String, BigDecimal> components = new TreeMap<>();
    BigDecimal numerator = BigDecimal.ZERO, denominator = BigDecimal.ZERO;
    boolean conflict = false, strong = false;
    for (var feature : weights.entrySet()) {
      String key = feature.getKey(), target = values.get(key);
      var observed =
          evidence.stream()
              .filter(e -> key.equals(e.attribute()))
              .map(ProductEvidence::value)
              .toList();
      if (target == null || normalize(target).isBlank() || observed.isEmpty()) continue;
      boolean match = observed.stream().allMatch(v -> normalize(v).equals(normalize(target)));
      boolean supported =
          c.evidence().stream()
              .anyMatch(
                  e -> key.equals(e.attribute()) && normalize(target).equals(normalize(e.value())));
      match = match && supported;
      if (key.equals("sku"))
        match =
            match && observed.stream().allMatch(v -> v.strip().equalsIgnoreCase(target.strip()));
      if (key.equals("ean")) {
        match = match && validEan(target) && observed.stream().allMatch(ProductMatcher::validEan);
        if (!match) conflict = true;
      }
      if (key.equals("sku") && !match) conflict = true;
      BigDecimal component = match ? BigDecimal.ONE : BigDecimal.ZERO;
      components.put(key, component);
      denominator = denominator.add(feature.getValue());
      numerator = numerator.add(component.multiply(feature.getValue()));
      if (match && (key.equals("ean") || key.equals("model"))) strong = true;
    }
    if (BigDecimal.ONE.equals(components.get("sku"))
        && BigDecimal.ONE.equals(components.get("brand"))) strong = true;
    BigDecimal confidence =
        denominator.signum() == 0
            ? BigDecimal.ZERO
            : numerator.divide(denominator, 8, RoundingMode.HALF_UP);
    return new ScoredCandidate(
        c,
        confidence,
        components,
        conflict,
        strong && !conflict && !BigDecimal.ZERO.equals(components.get("brand")),
        weights);
  }
}
