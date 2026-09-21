package dev.thomcgn.findly.matching;

import static org.junit.jupiter.api.Assertions.*;

import dev.thomcgn.findly.provider.*;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class ProductMatcherTest {
  private final ProductMatcher matcher =
      new ProductMatcher(
          new MatchingProperties(n(".75"), n(".10"), n("5"), n("4"), n("3"), n("1"), n("1")));

  private static BigDecimal n(String s) {
    return new BigDecimal(s);
  }

  private static ProductEvidence e(String key, String value) {
    return new ProductEvidence(
        key, value, URI.create("https://fixture.test/spec"), Instant.parse("2026-09-01T00:00:00Z"));
  }

  private static ProductCandidate c(String model, Map<String, String> ids) {
    var evidence =
        new ArrayList<>(List.of(e("model", model), e("brand", "Acme"), e("category", "Chair")));
    ids.forEach((k, v) -> evidence.add(e(k, v)));
    return new ProductCandidate("Acme " + model, "Acme", model, "Chair", ids, evidence);
  }

  @Test
  void uncorroboratedCandidateFieldsAndBrandConflictsCannotSelect() {
    var unsupported =
        new ProductCandidate("A12", "Acme", "A12", null, Map.of(), List.of(e("name", "A12")));
    assertNull(matcher.select(matcher.rank(List.of(unsupported), List.of(e("model", "A12")))));
    assertNull(
        matcher.select(
            matcher.rank(
                List.of(c("A12", Map.of("sku", "12"))),
                List.of(e("model", "A12"), e("sku", "12"), e("brand", "Another")))));
  }

  @Test
  void changingWeightsChangesTheEvidenceScore() {
    var candidate = c("A12", Map.of());
    var observations = List.of(e("model", "A12"), e("category", "Table"));
    assertEquals(
        0,
        n(".75").compareTo(matcher.rank(List.of(candidate), observations).getFirst().confidence()));
    var custom =
        new ProductMatcher(
            new MatchingProperties(n(".75"), n(".10"), n("5"), n("4"), n("1"), n("1"), n("1")));
    assertEquals(
        0,
        n(".5").compareTo(custom.rank(List.of(candidate), observations).getFirst().confidence()));
    assertNull(custom.select(custom.rank(List.of(candidate), observations)));
  }

  @Test
  void normalizationUsesUnicodeCaseAndWhitespace() {
    assertEquals("acme a 12", ProductMatcher.normalize(" ＡＣＭＥ  A-12 "));
    assertEquals("möbel", ProductMatcher.normalize("MÖBEL"));
    assertEquals("", ProductMatcher.normalize(null));
  }

  @Test
  void missingFeaturesAreExcludedFromDenominator() {
    var candidate = c("A12", Map.of());
    var ranked = matcher.rank(List.of(candidate), List.of(e("model", "a12")));
    assertEquals(0, ranked.getFirst().confidence().compareTo(BigDecimal.ONE));
    assertEquals(Map.of("model", BigDecimal.ONE), ranked.getFirst().components());
    assertEquals(candidate, matcher.select(ranked));
  }

  @Test
  void brandAloneAndAbsentEvidenceNeverIdentify() {
    assertNull(
        matcher.select(matcher.rank(List.of(c("A12", Map.of())), List.of(e("brand", "Acme")))));
    assertNull(matcher.select(matcher.rank(List.of(c("A12", Map.of())), List.of())));
  }

  @Test
  void rankingAndTieAreDeterministic() {
    var a = c("A12", Map.of());
    var b = c("B12", Map.of());
    var ranked = matcher.rank(List.of(b, a), List.of(e("model", "A12"), e("brand", "Acme")));
    assertEquals(a, matcher.select(ranked));
    assertNull(
        matcher.select(matcher.rank(List.of(a, c("a12", Map.of())), List.of(e("model", "A12")))));
  }

  @Test
  void thresholdAndMarginAreInclusive() {
    var a = c("A12", Map.of());
    var b = c("B12", Map.of());
    assertEquals(
        a,
        matcher.select(
            List.of(
                new ScoredCandidate(a, n(".75"), Map.of(), false, true),
                new ScoredCandidate(b, n(".65"), Map.of(), false, true))));
    assertNull(matcher.select(List.of(new ScoredCandidate(a, n(".7499"), Map.of(), false, true))));
    assertNull(
        matcher.select(
            List.of(
                new ScoredCandidate(a, n(".75"), Map.of(), false, true),
                new ScoredCandidate(b, n(".6501"), Map.of(), false, true))));
  }

  @Test
  void eanCheckDigitAndConflictsOverrideOtherMatches() {
    assertTrue(ProductMatcher.validEan("4006381333931"));
    assertTrue(ProductMatcher.validEan("96385074"));
    assertFalse(ProductMatcher.validEan("4006381333932"));
    assertFalse(ProductMatcher.validEan("123"));
    var candidate = c("A12", Map.of("ean", "4006381333931"));
    assertEquals(
        candidate,
        matcher.select(matcher.rank(List.of(candidate), List.of(e("ean", "4006381333931")))));
    var ranked = matcher.rank(List.of(candidate), List.of(e("ean", "96385074"), e("model", "A12")));
    assertTrue(ranked.getFirst().identifierConflict());
    assertNull(matcher.select(ranked));
  }

  @Test
  void skuRequiresMatchingBrandAndConflictsVetoSelection() {
    var candidate = c("A12", Map.of("sku", "AB-12"));
    assertNull(matcher.select(matcher.rank(List.of(candidate), List.of(e("sku", "AB-12")))));
    assertEquals(
        candidate,
        matcher.select(
            matcher.rank(List.of(candidate), List.of(e("sku", "AB-12"), e("brand", "Acme")))));
    assertNull(
        matcher.select(
            matcher.rank(List.of(candidate), List.of(e("sku", "AB-13"), e("model", "A12")))));
  }

  @Test
  void contradictoryExtractedValuesRemainUncertain() {
    assertNull(
        matcher.select(
            matcher.rank(
                List.of(c("A12", Map.of())), List.of(e("model", "A12"), e("model", "B12")))));
  }
}
