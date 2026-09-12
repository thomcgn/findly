package dev.thomcgn.findly.product;

import dev.thomcgn.findly.analysis.Analysis;
import dev.thomcgn.findly.listing.Listing;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ProductIdentificationService {

  public IdentifiedProduct identify(Analysis analysis, Listing listing) {
    String combinedText =
        (listing.getTitle() == null ? "" : listing.getTitle())
            + " "
            + (listing.getDescription() == null ? "" : listing.getDescription())
            + " "
            + String.join(
                " ", listing.getImageUrls() == null ? java.util.List.of() : listing.getImageUrls());
    String normalized = combinedText.toLowerCase(Locale.ROOT);

    String brand = "Unbekannt";
    String model = "Unbekanntes Modell";
    String category = "Allgemeiner Artikel";
    String modelNumber = "-";
    BigDecimal confidence = BigDecimal.valueOf(0.55);

    if (normalized.contains("ikea") && normalized.contains("malm")) {
      brand = "IKEA";
      model = "MALM";
      category = "Rollcontainer";
      modelNumber = "MALM";
      confidence = BigDecimal.valueOf(0.94);
    } else if (normalized.contains("ikea") && normalized.contains("rollcontainer")) {
      brand = "IKEA";
      model = "Rollcontainer";
      category = "Rollcontainer";
      modelNumber = "ROLLCONTAINER";
      confidence = BigDecimal.valueOf(0.9);
    } else if (normalized.contains("schrank") || normalized.contains("wardrobe")) {
      brand = "Unbekannt";
      model = "Schrank";
      category = "Schrank";
      confidence = BigDecimal.valueOf(0.7);
    } else if (normalized.contains("kommode")
        || normalized.contains("dresser")
        || normalized.contains("chest")) {
      brand = "Unbekannt";
      model = "Kommode";
      category = "Kommode";
      confidence = BigDecimal.valueOf(0.72);
    } else if (normalized.contains("ray-ban") || normalized.contains("ray ban")) {
      brand = "Ray-Ban";
      model = "Clubmaster";
      modelNumber = "CM-01";
      category = "Sonnenbrille";
      confidence = BigDecimal.valueOf(0.81);
    } else if (normalized.contains("oakley")) {
      brand = "Oakley";
      model = "Jawbreaker";
      modelNumber = "JB-2";
      category = "Sonnenbrille";
      confidence = BigDecimal.valueOf(0.81);
    } else if (normalized.contains("nike")) {
      brand = "Nike";
      model = "AeroFlex";
      modelNumber = "AF-7";
      category = "Sportbrille";
      confidence = BigDecimal.valueOf(0.79);
    } else if (normalized.contains("brille")
        || normalized.contains("glasses")
        || normalized.contains("sunglasses")) {
      brand = "Allgemeine Marke";
      model = "Brille";
      category = "Brillen";
      confidence = BigDecimal.valueOf(0.62);
    }

    String productName = brand.equals("Unbekannt") ? model : brand + " " + model;
    if (category != null
        && !category.equals("Allgemeiner Artikel")
        && !productName.toLowerCase(Locale.ROOT).contains(category.toLowerCase(Locale.ROOT))) {
      productName = productName + " " + category;
    }

    return IdentifiedProduct.builder()
        .analysis(analysis)
        .brand(brand)
        .name(productName)
        .model(model)
        .modelNumber(modelNumber)
        .category(category)
        .confidence(confidence)
        .build();
  }
}
