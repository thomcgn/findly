package dev.thomcgn.findly.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.thomcgn.findly.analysis.AnalysisController;
import dev.thomcgn.findly.analysis.AnalysisService;
import dev.thomcgn.findly.error.ProblemDetailsExceptionHandler;
import dev.thomcgn.findly.filter.AnalysisRateLimiter;
import dev.thomcgn.findly.filter.RateLimitingFilter;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CorsFilter;
import tools.jackson.databind.json.JsonMapper;

class ApiConfigurationTest {
  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties({CorsProperties.class, RateLimitProperties.class})
  static class PropertiesConfiguration {}

  @Test
  void defaultsAndOverridesBindThroughValidatedProperties() {
    var runner =
        new ApplicationContextRunner().withUserConfiguration(PropertiesConfiguration.class);
    runner.run(
        context -> {
          assertNull(context.getStartupFailure());
          assertEquals(
              List.of("http://localhost:3000"),
              context.getBean(CorsProperties.class).allowedOrigins());
          assertEquals(20, context.getBean(RateLimitProperties.class).createsPerMinute());
          assertEquals(120, context.getBean(RateLimitProperties.class).readsPerMinute());
        });
    runner
        .withPropertyValues(
            "app.rate-limit.creates-per-minute=2",
            "app.cors.allowed-origins=https://findly.example")
        .run(
            context -> {
              assertNull(context.getStartupFailure());
              assertEquals(2, context.getBean(RateLimitProperties.class).createsPerMinute());
              assertEquals(
                  List.of("https://findly.example"),
                  context.getBean(CorsProperties.class).allowedOrigins());
            });
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "app.rate-limit.creates-per-minute=0",
        "app.rate-limit.reads-per-minute=-1",
        "app.rate-limit.max-clients=0",
        "app.cors.allowed-origins=*"
      })
  void refusesInvalidConfiguration(String property) {
    new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration.class)
        .withPropertyValues(property)
        .run(context -> assertNotNull(context.getStartupFailure()));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "*",
        "https://*.example.com",
        "https://example.com/path",
        "https://user@example.com",
        "https://example.com?query",
        "https://example.com#fragment",
        "ftp://example.com"
      })
  void rejectsWildcardOrNonOriginCorsValues(String origin) {
    assertThrows(IllegalArgumentException.class, () -> new CorsProperties(List.of(origin)));
  }

  @Test
  void preflightUsesExplicitOriginsAndDoesNotConsumeAnalysisBudget() throws Exception {
    var source =
        new SecurityConfig()
            .corsConfigurationSource(new CorsProperties(List.of("https://findly.example")));
    var limiter =
        new RateLimitingFilter(
            new AnalysisRateLimiter(new RateLimitProperties(1, 1, 10), Clock.systemUTC()),
            JsonMapper.builder().build());
    var mvc =
        MockMvcBuilders.standaloneSetup(new AnalysisController(mock(AnalysisService.class)))
            .setControllerAdvice(new ProblemDetailsExceptionHandler())
            .addFilters(new CorsFilter(source), limiter)
            .build();
    for (int i = 0; i < 3; i++) {
      mvc.perform(
              options("/api/analyses")
                  .header("Origin", "https://findly.example")
                  .header("Access-Control-Request-Method", "POST")
                  .header("Access-Control-Request-Headers", "content-type"))
          .andExpect(status().isOk())
          .andExpect(header().string("Access-Control-Allow-Origin", "https://findly.example"));
    }
    mvc.perform(
            options("/api/analyses")
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    mvc.perform(
            post("/api/analyses")
                .header("Origin", "https://findly.example")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://kleinanzeigen.de/a\"}"))
        .andExpect(status().isAccepted());
    mvc.perform(
            post("/api/analyses")
                .header("Origin", "https://findly.example")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://kleinanzeigen.de/a\"}"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Access-Control-Allow-Origin", "https://findly.example"))
        .andExpect(header().string("Access-Control-Expose-Headers", "X-Trace-ID, Retry-After"))
        .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
  }
}
