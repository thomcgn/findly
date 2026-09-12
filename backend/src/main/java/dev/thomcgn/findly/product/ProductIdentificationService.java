package dev.thomcgn.findly.product;

import dev.thomcgn.findly.analysis.Analysis;
import dev.thomcgn.findly.listing.Listing;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;

@Service
public class ProductIdentificationService {

    public IdentifiedProduct identify(Analysis analysis, Listing listing) {
        String normalized = (listing.getTitle() + " " + listing.getDescription()).toLowerCase(Locale.ROOT);

        String brand = "Unbekannt";
        String model = "Unbekanntes Modell";
        String category = "Allgemeiner Artikel";
        String modelNumber = "-";
        BigDecimal confidence = BigDecimal.valueOf(0.55);

        if (normalized.contains("ikea") || normalized.contains("malm")) {
            brand = "IKEA";
            model = "MALM";
            category = "Möbel";
            modelNumber = "MALM";
            confidence = BigDecimal.valueOf(0.88);
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
        } else if (normalized.contains("brille") || normalized.contains("glasses") || normalized.contains("sunglasses")) {
            brand = "Allgemeine Marke";
            model = "Brille";
            category = "Brillen";
            confidence = BigDecimal.valueOf(0.62);
        }

        return IdentifiedProduct.builder()
                .analysis(analysis)
                .brand(brand)
                .name(brand + " " + model)
                .model(model)
                .modelNumber(modelNumber)
                .category(category)
                .confidence(confidence)
                .build();
    }
}
