package dev.thomcgn.findly.listing;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("app.listing")
@Validated
public record ListingClientProperties(
    @DefaultValue("5000") @Min(1) @Max(30000) int connectTimeoutMillis,
    @DefaultValue("10000") @Min(1) @Max(60000) int readTimeoutMillis,
    @DefaultValue("10485760") @Min(1) @Max(10485760) int maxResponseBytes) {}
