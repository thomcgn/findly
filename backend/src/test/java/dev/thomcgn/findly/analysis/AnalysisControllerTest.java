package dev.thomcgn.findly.analysis;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.thomcgn.findly.error.ProblemDetailsExceptionHandler;
import dev.thomcgn.findly.filter.RateLimitingFilter;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AnalysisControllerTest {

  private MockMvc mockMvc;
  private AnalysisService analysisService;

  @BeforeEach
  void setUp() {
    analysisService = mock(AnalysisService.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new AnalysisController(analysisService))
            .setControllerAdvice(new ProblemDetailsExceptionHandler())
            .addFilters(new RateLimitingFilter())
            .build();
  }

  @Test
  void createAnalysis_withValidUrl_returnsAccepted() throws Exception {
    UUID analysisId = UUID.randomUUID();
    when(analysisService.createAnalysis(anyString()))
        .thenReturn(new AnalysisStartResponse(analysisId, AnalysisStatus.PENDING));

    mockMvc
        .perform(
            post("/api/analyses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://www.kleinanzeigen.de/s-anzeige/test\"}"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.id").value(analysisId.toString()))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void getAnalysisStatus_returnsStatusPayload() throws Exception {
    UUID analysisId = UUID.randomUUID();
    when(analysisService.getAnalysisStatus(analysisId))
        .thenReturn(
            new AnalysisStatusResponse(
                analysisId,
                AnalysisStatus.ANALYZING,
                42,
                java.time.Instant.now(),
                java.time.Instant.now(),
                java.time.Instant.now(),
                null,
                null,
                null,
                null));

    mockMvc
        .perform(get("/api/analyses/{id}", analysisId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ANALYZING"))
        .andExpect(jsonPath("$.progress").value(42));
  }

  @Test
  void getAnalysisResult_returnsDetailPayload() throws Exception {
    UUID analysisId = UUID.randomUUID();
    when(analysisService.getAnalysis(analysisId))
        .thenReturn(
            new AnalysisDetailResponse(
                analysisId,
                AnalysisStatus.COMPLETED,
                new AnalysisDetailResponse.ListingSummary(
                    "MALM", java.math.BigDecimal.valueOf(80), "EUR", "https://www.kleinanzeigen.de/s-anzeige/test", java.util.List.of()),
                new AnalysisDetailResponse.ProductSummary("IKEA", "MALM", "Schrank", java.math.BigDecimal.valueOf(0.9)),
                new AnalysisDetailResponse.MarketSummary(
                    java.math.BigDecimal.valueOf(200),
                    java.math.BigDecimal.valueOf(190),
                    java.math.BigDecimal.valueOf(220)),
                new AnalysisDetailResponse.DealSummary("good", java.math.BigDecimal.valueOf(10.0))));

    mockMvc
        .perform(get("/api/analyses/{id}/result", analysisId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.listing.title").value("MALM"));
  }

  @Test
  void createAnalysis_withInvalidUrl_returnsProblemDetails() throws Exception {
    mockMvc
        .perform(
            post("/api/analyses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"ftp://example.com\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("application/problem+json")))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Invalid request"));
  }

  @Test
  void createAnalysis_whenRateLimitExceeded_returns429ProblemDetails() throws Exception {
    when(analysisService.createAnalysis(anyString()))
        .thenReturn(new AnalysisStartResponse(UUID.randomUUID(), AnalysisStatus.PENDING));

    String body = "{\"url\":\"https://www.kleinanzeigen.de/s-anzeige/test\"}";
    for (int i = 0; i < 20; i++) {
      mockMvc
          .perform(
              post("/api/analyses")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body)
                  .with(request -> {
                    request.setRemoteAddr("203.0.113.42");
                    return request;
                  }))
          .andExpect(status().isAccepted());
    }

    mockMvc
        .perform(
            post("/api/analyses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(request -> {
                  request.setRemoteAddr("203.0.113.42");
                  return request;
                }))
        .andExpect(status().isTooManyRequests())
        .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("application/problem+json")))
        .andExpect(jsonPath("$.status").value(429))
        .andExpect(jsonPath("$.title").value("Too Many Requests"));
  }
}
