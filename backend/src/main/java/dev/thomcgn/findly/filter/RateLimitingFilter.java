package dev.thomcgn.findly.filter;

import dev.thomcgn.findly.error.TooManyRequestsException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Simple sliding-window rate limiter per remote IP for analysis endpoints.
 * Limits to 20 requests per minute by default.
 */
@Component
public class RateLimitingFilter extends HttpFilter {

  private static final long WINDOW_SECONDS = 60;
  private static final int MAX_REQUESTS = 20;

  private record Window(Instant start, int count) {}

  private static final long serialVersionUID = 1L;
  private transient final Map<String, Window> map = new ConcurrentHashMap<>();

  @Override
  protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    String path = req.getRequestURI();
    if (path != null && path.startsWith("/api/analyses")) {
      try {
        String ip = req.getRemoteAddr();
        Instant now = Instant.now();
        map.compute(ip, (k, w) -> {
          if (w == null) {
            return new Window(now, 1);
          }
          if (Duration.between(w.start, now).getSeconds() >= WINDOW_SECONDS) {
            return new Window(now, 1);
          }
          return new Window(w.start, w.count + 1);
        });
        Window current = map.get(ip);
        if (current.count() > MAX_REQUESTS) {
          throw new TooManyRequestsException("Rate limit exceeded for this IP");
        }
      } catch (TooManyRequestsException e) {
        writeProblem(res, e.getMessage());
        return;
      }
    }
    chain.doFilter(req, res);
  }

  private void writeProblem(HttpServletResponse res, String message) throws IOException {
    String traceId = UUID.randomUUID().toString();
    String safeMessage = escapeJson(message);
    String body =
        "{"
            + "\"type\":\"about:blank\","
            + "\"title\":\"Too Many Requests\","
            + "\"status\":429,"
            + "\"detail\":\""
            + safeMessage
            + "\","
            + "\"instance\":\"/api/analyses\","
            + "\"traceId\":\""
            + traceId
            + "\","
            + "\"timestamp\":\""
            + Instant.now()
            + "\"}"
            + "";

    res.setStatus(429);
    res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    res.setCharacterEncoding(StandardCharsets.UTF_8.name());
    res.getWriter().write(body);
  }

  private String escapeJson(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
  }
}
