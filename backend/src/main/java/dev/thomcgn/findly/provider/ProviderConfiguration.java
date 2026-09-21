package dev.thomcgn.findly.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ProviderProperties.class)
public class ProviderConfiguration {
  @Bean
  @ConditionalOnMissingBean(OcrProvider.class)
  OcrProvider ocrProvider(ProviderProperties properties) {
    return switch (properties.ocr()) {
      case DISABLED, LOCAL_STUB -> listing -> ProviderResult.unavailable();
    };
  }

  @Bean
  @ConditionalOnMissingBean(VisionProvider.class)
  VisionProvider visionProvider(ProviderProperties properties) {
    return switch (properties.vision()) {
      case DISABLED, LOCAL_STUB -> listing -> ProviderResult.unavailable();
    };
  }

  @Bean
  @ConditionalOnMissingBean(SearchProvider.class)
  SearchProvider searchProvider(ProviderProperties properties) {
    return switch (properties.search()) {
      case DISABLED, LOCAL_STUB -> (listing, evidence) -> ProviderResult.unavailable();
    };
  }

  @Bean
  @ConditionalOnMissingBean(PricingProvider.class)
  PricingProvider pricingProvider(ProviderProperties properties) {
    return switch (properties.pricing()) {
      case DISABLED, LOCAL_STUB -> product -> ProviderResult.unavailable();
    };
  }
}
