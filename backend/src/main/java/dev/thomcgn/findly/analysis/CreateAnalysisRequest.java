package dev.thomcgn.findly.analysis;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAnalysisRequest(
    @NotBlank
    @Size(max = 2048)
    @Pattern(
        regexp = "https://(?:www\\.)?kleinanzeigen\\.de/.*",
        message = "Only https://kleinanzeigen.de and https://www.kleinanzeigen.de URLs are supported")
    String url) {}
