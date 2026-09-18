package dev.thomcgn.findly.analysis;

import java.time.Instant;
import java.util.UUID;

public record AnalysisStatusResponse(
    UUID id,
    AnalysisStatus status,
    int progress,
    Instant createdAt,
    Instant updatedAt,
    Instant startedAt,
    Instant completedAt,
    Instant failedAt,
    String errorCode,
    String errorMessage) {}
