package dev.thomcgn.findly.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record AnalysisStartResponse(UUID analysisId, AnalysisStatus status) {
  @JsonProperty("id")
  public UUID id() {
    return analysisId;
  }
}
