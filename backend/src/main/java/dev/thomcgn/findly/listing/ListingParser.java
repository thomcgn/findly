package dev.thomcgn.findly.listing;

import dev.thomcgn.findly.error.AnalysisErrorCode;
import dev.thomcgn.findly.error.AnalysisException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;

@Component
public class ListingParser {
  private final ObjectReader json;

  public ListingParser(ObjectMapper mapper) {
    json = mapper.reader();
  }

  private record Price(BigDecimal amount, String currency) {}

  public ListingFetchResult parse(String html) {
    if (html == null || html.isBlank())
      throw new AnalysisException(AnalysisErrorCode.LISTING_PARSE_FAILED);
    var document = Jsoup.parse(html);
    if (!document
            .select("#captcha, .g-recaptcha, iframe[src*=captcha], form[action*=login]")
            .isEmpty()
        || document
            .title()
            .toLowerCase(java.util.Locale.ROOT)
            .matches(".*(captcha|access denied|zugriff verweigert|verify you are human).*")) {
      throw new AnalysisException(AnalysisErrorCode.LISTING_ACCESS_BLOCKED);
    }
    List<JsonNode> products = new ArrayList<>();
    for (Element script : document.select("script[type=application/ld+json]")) {
      try {
        collectProducts(json.readTree(script.data()), products);
      } catch (JacksonException ex) {
        /* Invalid optional structured data is not evidence. */
      }
    }
    JsonNode product = products.size() == 1 ? products.getFirst() : null;
    String title = text(document.selectFirst("#viewad-title"));
    if (title == null && product != null) title = scalar(product.get("name"));
    if (title == null) title = content(document.selectFirst("meta[property=og:title]"));
    if (title == null || title.length() > 255)
      throw new AnalysisException(AnalysisErrorCode.LISTING_PARSE_FAILED);
    String description = text(document.selectFirst("#viewad-description-text"));
    if (description == null) description = content(document.selectFirst("meta[name=description]"));
    if (description == null && product != null) description = scalar(product.get("description"));
    if (description != null && description.length() > 10000)
      description = description.substring(0, 10000);

    List<Price> evidence = new ArrayList<>();
    if (product != null) collectOffers(product.get("offers"), evidence);
    Price meta =
        price(
            content(document.selectFirst("meta[property=product:price:amount]")),
            content(document.selectFirst("meta[property=product:price:currency]")));
    if (meta != null) evidence.add(meta);
    String displayed = text(document.selectFirst("#viewad-price"));
    if (displayed != null
        && displayed.matches(
            "(?:[0-9]+|[0-9]{1,3}(?:\\.[0-9]{3})+)(?:,[0-9]{1,2})?\\s*(€|EUR)(\\s+VB)?")) {
      Price dom =
          price(displayed.replaceAll("\\s|€|EUR|VB", "").replace(".", "").replace(',', '.'), "EUR");
      if (dom != null) evidence.add(dom);
    }
    List<Price> unique = evidence.stream().distinct().toList();
    Price accepted = unique.size() == 1 ? unique.getFirst() : null;
    var images =
        document.select("meta[property=og:image]").stream()
            .map(e -> e.attr("content"))
            .filter(value -> value.startsWith("https://") && value.length() <= 2048)
            .distinct()
            .limit(5)
            .toList();
    return new ListingFetchResult(
        title,
        description,
        accepted == null ? null : accepted.amount(),
        accepted == null ? null : accepted.currency(),
        images);
  }

  private void collectProducts(JsonNode node, List<JsonNode> products) {
    if (node == null) return;
    if (node.isArray()) {
      for (JsonNode value : node) collectProducts(value, products);
    } else if (node.isObject()) {
      JsonNode type = node.get("@type");
      if (type != null
          && ("Product".equals(scalar(type))
              || (type.isArray()
                  && type.valueStream().anyMatch(t -> "Product".equals(t.asString())))))
        products.add(node);
      collectProducts(node.get("@graph"), products);
    }
  }

  private void collectOffers(JsonNode node, List<Price> prices) {
    if (node == null) return;
    if (node.isArray()) {
      for (JsonNode offer : node) collectOffers(offer, prices);
    } else if (node.isObject()) {
      Price value = price(scalar(node.get("price")), scalar(node.get("priceCurrency")));
      if (value != null) prices.add(value);
    }
  }

  private Price price(String amount, String currency) {
    if (amount == null
        || currency == null
        || !amount.matches("[0-9]+(?:\\.[0-9]{1,2})?")
        || !currency.matches("[A-Z]{3}")) return null;
    try {
      Currency.getInstance(currency);
      BigDecimal value = new BigDecimal(amount).setScale(2);
      return value.compareTo(new BigDecimal("99999999.99")) <= 0
          ? new Price(value, currency)
          : null;
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private String scalar(JsonNode value) {
    return value == null || value.isNull() || (value.isArray() || value.isObject())
        ? null
        : clean(value.asString());
  }

  private String text(Element element) {
    return element == null ? null : clean(element.text());
  }

  private String content(Element element) {
    return element == null ? null : clean(element.attr("content"));
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
