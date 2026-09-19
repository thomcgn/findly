package dev.thomcgn.findly.filter;

import dev.thomcgn.findly.error.AnalysisErrorCode;
import dev.thomcgn.findly.error.ApiProblem;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
  private final AnalysisRateLimiter limiter;
  private final ObjectWriter writer;

  public RateLimitingFilter(AnalysisRateLimiter limiter, ObjectMapper mapper) {
    this.limiter = limiter;
    this.writer = mapper.writer();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    boolean create = request.getMethod().equals("POST") && path.equals("/api/analyses");
    boolean read =
        request.getMethod().equals("GET") && path.matches("/api/analyses/[^/]+(?:/result)?");
    if (create || read) {
      var admission = limiter.acquire(request.getRemoteAddr(), create);
      if (!admission.allowed()) {
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(admission.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        writer.writeValue(
            response.getWriter(),
            ApiProblem.create(AnalysisErrorCode.RATE_LIMIT_EXCEEDED, request, response));
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
