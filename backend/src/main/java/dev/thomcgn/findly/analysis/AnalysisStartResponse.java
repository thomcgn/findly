package dev.thomcgn.findly.analysis;

import java.util.UUID;

public record AnalysisStartResponse(UUID analysisId, AnalysisStatus status) {
}
