package dev.thomcgn.findly.matching;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.matching")
public record MatchingProperties(
    @DefaultValue("0.75") @DecimalMin("0.75") @DecimalMax("1") BigDecimal minimumConfidence,
    @DefaultValue("0.10") @DecimalMin("0.10") @DecimalMax("1") BigDecimal minimumMargin,
    @DefaultValue("5") @DecimalMin("0.001") BigDecimal eanWeight,
    @DefaultValue("4") @DecimalMin("0.001") BigDecimal skuWeight,
    @DefaultValue("3") @DecimalMin("0.001") BigDecimal modelWeight,
    @DefaultValue("1") @DecimalMin("0.001") BigDecimal brandWeight,
    @DefaultValue("1") @DecimalMin("0.001") BigDecimal categoryWeight) {}
