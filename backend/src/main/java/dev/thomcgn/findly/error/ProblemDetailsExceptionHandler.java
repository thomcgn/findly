package dev.thomcgn.findly.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ControllerAdvice
public class ProblemDetailsExceptionHandler {

  private Map<String, Object> toProblem(HttpStatus status, String title, String detail, String instance) {
    Map<String, Object> body = new HashMap<>();
    body.put("type", "about:blank");
    body.put("title", title);
    body.put("status", status.value());
    body.put("detail", detail);
    body.put("instance", instance);
    body.put("traceId", UUID.randomUUID().toString());
    body.put("timestamp", Instant.now().toString());
    return body;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseBody
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
    StringBuilder sb = new StringBuilder();
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      sb.append(fe.getField()).append(": ").append(fe.getDefaultMessage()).append("; ");
    }
    Map<String, Object> body = toProblem(HttpStatus.BAD_REQUEST, "Invalid request", sb.toString(), req.getRequestURI());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.parseMediaType("application/problem+json")).body(body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseBody
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest req) {
    Map<String, Object> body =
        toProblem(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage(), req.getRequestURI());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.parseMediaType("application/problem+json"))
        .body(body);
  }

  @ExceptionHandler(TooManyRequestsException.class)
  @ResponseBody
  public ResponseEntity<Map<String, Object>> handleRateLimit(TooManyRequestsException ex, HttpServletRequest req) {
    Map<String, Object> body = toProblem(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", ex.getMessage(), req.getRequestURI());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.parseMediaType("application/problem+json")).body(body);
  }

  @ExceptionHandler(Exception.class)
  @ResponseBody
  public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, HttpServletRequest req) {
    Map<String, Object> body = toProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ex.getMessage(), req.getRequestURI());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.parseMediaType("application/problem+json")).body(body);
  }
}
