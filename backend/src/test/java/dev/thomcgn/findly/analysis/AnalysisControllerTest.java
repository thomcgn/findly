package dev.thomcgn.findly.analysis;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.thomcgn.findly.config.RateLimitProperties;
import dev.thomcgn.findly.error.ProblemDetailsExceptionHandler;
import dev.thomcgn.findly.filter.AnalysisRateLimiter;
import dev.thomcgn.findly.filter.RateLimitingFilter;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

class AnalysisControllerTest {

  private MockMvc mockMvc;
  private AnalysisService analysisService;

  @BeforeEach
  void setUp() {
    analysisService = mock(AnalysisService.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new AnalysisController(analysisService))
            .setControllerAdvice(new ProblemDetailsExceptionHandler())
            .addFilters(
                new RateLimitingFilter(
                    new AnalysisRateLimiter(
                        new RateLimitProperties(20, 120, 10000), Clock.systemUTC()),
                    JsonMapper.builder().build()))
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
                    "MALM",
                    java.math.BigDecimal.valueOf(80),
                    "EUR",
                    "https://www.kleinanzeigen.de/s-anzeige/test",
                    java.util.List.of()),
                new AnalysisDetailResponse.ProductSummary(
                    "IKEA", "MALM", "Schrank", java.math.BigDecimal.valueOf(0.9)),
                new AnalysisDetailResponse.MarketSummary(
                    java.math.BigDecimal.valueOf(200),
                    java.math.BigDecimal.valueOf(190),
                    java.math.BigDecimal.valueOf(220)),
                new AnalysisDetailResponse.DealSummary(
                    "good", java.math.BigDecimal.valueOf(10.0))));

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
        .andExpect(
            content()
                .contentTypeCompatibleWith(MediaType.parseMediaType("application/problem+json")))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Invalid URL"));
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
                  .with(
                      request -> {
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
                .with(
                    request -> {
                      request.setRemoteAddr("203.0.113.42");
                      return request;
                    }))
        .andExpect(status().isTooManyRequests())
        .andExpect(
            content()
                .contentTypeCompatibleWith(MediaType.parseMediaType("application/problem+json")))
        .andExpect(jsonPath("$.status").value(429))
        .andExpect(jsonPath("$.title").value("Too Many Requests"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.EnumSource(dev.thomcgn.findly.error.AnalysisErrorCode.class)
  void serializesEveryDomainErrorWithTraceId(dev.thomcgn.findly.error.AnalysisErrorCode code)
      throws Exception {
    UUID id = UUID.randomUUID();
    when(analysisService.getAnalysis(id))
        .thenThrow(new dev.thomcgn.findly.error.AnalysisException(code));
    var response =
        mockMvc
            .perform(get("/api/analyses/{id}/result", id))
            .andExpect(status().is(code.status().value()))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.title").value(code.title()))
            .andExpect(jsonPath("$.status").value(code.status().value()))
            .andExpect(jsonPath("$.detail").value(code.detail()))
            .andExpect(jsonPath("$.code").value(code.name()))
            .andExpect(jsonPath("$.instance").value("/api/analyses/" + id + "/result"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andReturn()
            .getResponse();
    String traceId = response.getHeader("X-Trace-ID");
    UUID.fromString(traceId);
    org.junit.jupiter.api.Assertions.assertTrue(response.getContentAsString().contains(traceId));
  }

  @Test
  void missingAnalysisReturns404() throws Exception {
    UUID id = UUID.randomUUID();
    when(analysisService.getAnalysisStatus(id))
        .thenThrow(new jakarta.persistence.EntityNotFoundException("secret"));
    mockMvc
        .perform(get("/api/analyses/{id}", id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ANALYSIS_NOT_FOUND"));
  }

  @Test
  void malformedUuidReturns400() throws Exception {
    mockMvc
        .perform(get("/api/analyses/not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    org.mockito.Mockito.verifyNoInteractions(analysisService);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"{", "{}", "{\"url\":null}", "{\"url\":\"\"}"})
  void invalidBodiesReturn400(String body) throws Exception {
    mockMvc
        .perform(post("/api/analyses").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    org.mockito.Mockito.verifyNoInteractions(analysisService);
  }

  @Test
  void oversizedUrlReturns400() throws Exception {
    mockMvc
        .perform(
            post("/api/analyses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://kleinanzeigen.de/" + "a".repeat(2048) + "\"}"))
        .andExpect(status().isBadRequest());
    org.mockito.Mockito.verifyNoInteractions(analysisService);
  }

  @Test
  void unexpectedFailuresDoNotExposeInternalMessages() throws Exception {
    UUID id = UUID.randomUUID();
    when(analysisService.getAnalysisStatus(id))
        .thenThrow(new IllegalStateException("password=secret"));
    mockMvc
        .perform(get("/api/analyses/{id}", id))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
  }

  @Test
  void unsupportedMethodKeeps405AndAllowHeader() throws Exception {
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                "/api/analyses"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                .string("Allow", "POST"))
        .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
  }

  @Test
  void unsupportedRequestMediaTypeKeeps415() throws Exception {
    mockMvc
        .perform(post("/api/analyses").contentType(MediaType.TEXT_PLAIN).content("url"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"", "/result"})
  void rateLimitsBothReadEndpointsWithTheSameErrorContract(String suffix) throws Exception {
    var filter =
        new RateLimitingFilter(
            new AnalysisRateLimiter(new RateLimitProperties(1, 1, 100), Clock.systemUTC()),
            JsonMapper.builder().build());
    var mvc =
        MockMvcBuilders.standaloneSetup(new AnalysisController(analysisService))
            .setControllerAdvice(new ProblemDetailsExceptionHandler())
            .addFilters(filter)
            .build();
    String path = "/api/analyses/" + UUID.randomUUID() + suffix;
    mvc.perform(get(path)).andExpect(status().isOk());
    var response =
        mvc.perform(get(path).header("X-Forwarded-For", "192.0.2.222"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
            .andExpect(jsonPath("$.instance").value(path))
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                    .exists("Retry-After"))
            .andReturn()
            .getResponse();
    String traceId = response.getHeader("X-Trace-ID");
    UUID.fromString(traceId);
    org.junit.jupiter.api.Assertions.assertTrue(response.getContentAsString().contains(traceId));
    mvc.perform(
            post("/api/analyses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://kleinanzeigen.de/a\"}"))
        .andExpect(status().isAccepted());
  }
}
