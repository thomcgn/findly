package dev.thomcgn.findly.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class ApiProblem {
  private ApiProblem() {}

  public static Map<String, Object> create(
      AnalysisErrorCode code, HttpServletRequest request, HttpServletResponse response) {
    String traceId = UUID.randomUUID().toString();
    response.setHeader("X-Trace-ID", traceId);
    return Map.of(
        "type",
        "about:blank",
        "title",
        code.title(),
        "status",
        code.status().value(),
        "detail",
        code.detail(),
        "instance",
        request.getRequestURI(),
        "code",
        code.name(),
        "traceId",
        traceId,
        "timestamp",
        Instant.now().toString());
  }
}
