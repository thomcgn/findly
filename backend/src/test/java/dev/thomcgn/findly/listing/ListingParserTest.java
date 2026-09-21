package dev.thomcgn.findly.listing;

import static org.junit.jupiter.api.Assertions.*;

import dev.thomcgn.findly.error.AnalysisException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ListingParserTest {
  private final ListingParser parser = new ListingParser(JsonMapper.builder().build());

  @Test
  void parsesListingFieldsAndGermanPriceWithoutUsingOriginalPrice() {
    var result =
        parser.parse(
            "<h1 id='viewad-title'>Schrank &amp; Tisch</h1><h2 id='viewad-price'>1.234,50 € VB</h2><p id='viewad-description-text'>Neupreis 2.999 €</p><meta content='https://images.example/a.jpg' property='og:image'>");
    assertEquals("Schrank & Tisch", result.title());
    assertEquals(new BigDecimal("1234.50"), result.price());
    assertEquals("EUR", result.currency());
    assertEquals(1, result.imageUrls().size());
  }

  @Test
  void missingPriceIsNotZeroAndDescriptionPricesAreNotEvidence() {
    var result = parser.parse("<h1 id=viewad-title>Fixture</h1><p>Neupreis: 500 €</p>");
    assertNull(result.price());
    assertNull(result.currency());
    assertNull(result.description());
  }

  @Test
  void structuredProductOffersAreAcceptedButAmbiguityIsUnknown() {
    String json =
        "<script type='application/ld+json'>{\"@graph\":[{\"@type\":\"Product\",\"name\":\"Fixture\",\"offers\":{\"price\":\"0\",\"priceCurrency\":\"EUR\"}}]}</script>";
    assertEquals(new BigDecimal("0.00"), parser.parse(json).price());
    assertNull(parser.parse(json + "<h2 id=viewad-price>20 €</h2>").price());
  }

  @Test
  void invalidOrBlockedHtmlDoesNotBecomePlaceholderListing() {
    assertThrows(AnalysisException.class, () -> parser.parse("<title>Just a page</title>"));
    assertThrows(
        AnalysisException.class,
        () -> parser.parse("<h1 id=viewad-title>Fixture</h1><div id=captcha>Challenge</div>"));
  }

  @Test
  void malformedSeparatorsAndUnknownCurrenciesDoNotBecomePrices() {
    assertNull(
        parser.parse("<h1 id=viewad-title>Fixture</h1><h2 id=viewad-price>12.50 €</h2>").price());
    assertNull(
        parser
            .parse(
                "<meta property=og:title content=Fixture><meta property='product:price:amount' content=12><meta property='product:price:currency' content=ZZZ>")
            .price());
  }
}
