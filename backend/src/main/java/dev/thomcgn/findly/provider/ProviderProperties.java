package dev.thomcgn.findly.provider;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("app.providers")
@Validated
public record ProviderProperties(
    @DefaultValue("DISABLED") @NotNull Mode ocr,
    @DefaultValue("DISABLED") @NotNull Mode vision,
    @DefaultValue("DISABLED") @NotNull Mode search,
    @DefaultValue("DISABLED") @NotNull Mode pricing) {
  public enum Mode {
    DISABLED,
    LOCAL_STUB
  }
}
