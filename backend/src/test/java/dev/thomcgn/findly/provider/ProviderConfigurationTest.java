package dev.thomcgn.findly.provider;

import static org.junit.jupiter.api.Assertions.*;

import dev.thomcgn.findly.listing.ListingFetchResult;
import dev.thomcgn.findly.price.PriceResearchService;
import dev.thomcgn.findly.product.ProductIdentificationService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ProviderConfigurationTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withBean(java.time.Clock.class, java.time.Clock::systemUTC)
          .withUserConfiguration(
              dev.thomcgn.findly.matching.ProductMatcher.class,
              dev.thomcgn.findly.price.PricePolicy.class,
              ProviderConfiguration.class,
              ProductIdentificationService.class,
              PriceResearchService.class);
  private final ListingFetchResult listing =
      new ListingFetchResult(
          "IKEA MALM Ray-Ban Nike Oakley",
          "Neupreis 999 EUR",
          new BigDecimal("290"),
          "EUR",
          List.of("https://example.test/ray-ban-clubmaster.jpg"));

  @Test
  void unsafeMatchingConfigurationFailsStartup() {
    for (String property :
        List.of(
            "app.matching.minimum-confidence=.74",
            "app.matching.minimum-margin=.09",
            "app.matching.model-weight=0"))
      runner
          .withPropertyValues(property)
          .run(context -> assertNotNull(context.getStartupFailure()));
  }

  @Test
  void applicationServicesStartWithoutKeysAndDoNotInventModelsPricesOrConfidence() {
    runner.run(
        context -> {
          assertNull(context.getStartupFailure());
          var identification =
              context.getBean(ProductIdentificationService.class).identify(listing);
          assertTrue(identification.candidates().isEmpty());
          assertNull(identification.selected());
          assertTrue(
              identification
                  .warnings()
                  .containsAll(
                      List.of(
                          "OCR_UNAVAILABLE",
                          "VISION_UNAVAILABLE",
                          "SEARCH_UNAVAILABLE",
                          "PRODUCT_NOT_IDENTIFIED")));
          var price =
              context.getBean(PriceResearchService.class).research(identification.selected());
          assertTrue(price.quotes().isEmpty());
          assertEquals(List.of("PRICE_NOT_VERIFIED"), price.warnings());
        });
  }

  @ParameterizedTest
  @EnumSource(ProviderProperties.Mode.class)
  void disabledAndLocalStubsAlwaysReportUnavailability(ProviderProperties.Mode mode) {
    runner
        .withPropertyValues(
            "app.providers.ocr=" + mode,
            "app.providers.vision=" + mode,
            "app.providers.search=" + mode,
            "app.providers.pricing=" + mode)
        .run(
            context -> {
              assertNull(context.getStartupFailure());
              assertEquals(
                  ProviderResult.unavailable(),
                  context.getBean(OcrProvider.class).extract(listing));
              assertEquals(
                  ProviderResult.unavailable(),
                  context.getBean(VisionProvider.class).analyze(listing));
              assertEquals(
                  ProviderResult.unavailable(),
                  context.getBean(SearchProvider.class).search(listing, List.of()));
              assertEquals(
                  ProviderResult.unavailable(),
                  context.getBean(PricingProvider.class).research(null));
            });
  }

  @Test
  void unknownProviderModeFailsInsteadOfFallingBackSilently() {
    runner
        .withPropertyValues("app.providers.search=unknown-vendor")
        .run(context -> assertNotNull(context.getStartupFailure()));
  }
}
