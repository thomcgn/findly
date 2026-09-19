package dev.thomcgn.findly.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("app.rate-limit")
@Validated
public record RateLimitProperties(
    @DefaultValue("20") @Min(1) int createsPerMinute,
    @DefaultValue("120") @Min(1) int readsPerMinute,
    @DefaultValue("10000") @Min(1) int maxClients) {}
