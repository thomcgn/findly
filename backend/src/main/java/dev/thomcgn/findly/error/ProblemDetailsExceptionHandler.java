package dev.thomcgn.findly.error;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ProblemDetailsExceptionHandler {
  @ExceptionHandler(AnalysisException.class)
  public ResponseEntity<Map<String, Object>> handleAnalysis(
      AnalysisException ex, HttpServletRequest request, HttpServletResponse response) {
    return problem(ex.code(), request, response);
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    IllegalArgumentException.class
  })
  public ResponseEntity<Map<String, Object>> handleValidation(
      Exception ex, HttpServletRequest request, HttpServletResponse response) {
    return problem(AnalysisErrorCode.INVALID_REQUEST, request, response);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFound(
      EntityNotFoundException ex, HttpServletRequest request, HttpServletResponse response) {
    return problem(AnalysisErrorCode.ANALYSIS_NOT_FOUND, request, response);
  }

  @ExceptionHandler(TooManyRequestsException.class)
  public ResponseEntity<Map<String, Object>> handleRateLimit(
      TooManyRequestsException ex, HttpServletRequest request, HttpServletResponse response) {
    return problem(AnalysisErrorCode.RATE_LIMIT_EXCEEDED, request, response);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<Map<String, Object>> handleMethod(
      HttpRequestMethodNotSupportedException ex,
      HttpServletRequest request,
      HttpServletResponse response) {
    response.setHeader(
        "Allow",
        String.join(
            ", ", ex.getSupportedMethods() == null ? new String[0] : ex.getSupportedMethods()));
    return problem(AnalysisErrorCode.METHOD_NOT_ALLOWED, request, response);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<Map<String, Object>> handleContentType(
      HttpMediaTypeNotSupportedException ex,
      HttpServletRequest request,
      HttpServletResponse response) {
    return problem(AnalysisErrorCode.UNSUPPORTED_MEDIA_TYPE, request, response);
  }

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  public ResponseEntity<Map<String, Object>> handleAccept(
      HttpMediaTypeNotAcceptableException ex,
      HttpServletRequest request,
      HttpServletResponse response) {
    return problem(AnalysisErrorCode.NOT_ACCEPTABLE, request, response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGeneric(
      Exception ex, HttpServletRequest request, HttpServletResponse response) {
    return problem(AnalysisErrorCode.INTERNAL_ERROR, request, response);
  }

  private ResponseEntity<Map<String, Object>> problem(
      AnalysisErrorCode code, HttpServletRequest request, HttpServletResponse response) {
    return ResponseEntity.status(code.status())
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(ApiProblem.create(code, request, response));
  }
}
