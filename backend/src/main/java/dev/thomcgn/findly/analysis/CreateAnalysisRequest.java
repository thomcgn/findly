package dev.thomcgn.findly.analysis;

import jakarta.validation.constraints.NotBlank;

public record CreateAnalysisRequest(@NotBlank String url) {}
