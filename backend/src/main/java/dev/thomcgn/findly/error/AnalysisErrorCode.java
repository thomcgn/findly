package dev.thomcgn.findly.error;

import org.springframework.http.HttpStatus;

public enum AnalysisErrorCode {
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request", "The request is invalid."),
  INVALID_URL(
      HttpStatus.BAD_REQUEST, "Invalid URL", "A valid HTTPS Kleinanzeigen URL is required."),
  ANALYSIS_NOT_FOUND(
      HttpStatus.NOT_FOUND, "Analysis not found", "The requested analysis does not exist."),
  RESULT_NOT_READY(HttpStatus.CONFLICT, "Result not ready", "The analysis has not finished yet."),
  ANALYSIS_FAILED(
      HttpStatus.UNPROCESSABLE_CONTENT, "Analysis failed", "The analysis could not be completed."),
  RATE_LIMIT_EXCEEDED(
      HttpStatus.TOO_MANY_REQUESTS,
      "Too Many Requests",
      "The request limit has been exceeded. Please retry later."),
  LISTING_FETCH_FAILED(
      HttpStatus.BAD_GATEWAY, "Listing fetch failed", "The listing provider could not be reached."),
  ANALYSIS_TIMEOUT(
      HttpStatus.GATEWAY_TIMEOUT, "Analysis timed out", "The analysis exceeded its time limit."),
  METHOD_NOT_ALLOWED(
      HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", "This HTTP method is not supported."),
  UNSUPPORTED_MEDIA_TYPE(
      HttpStatus.UNSUPPORTED_MEDIA_TYPE,
      "Unsupported Media Type",
      "Use application/json for analysis requests."),
  NOT_ACCEPTABLE(
      HttpStatus.NOT_ACCEPTABLE,
      "Not Acceptable",
      "The requested response format is not supported."),
  INTERNAL_ERROR(
      HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred.");

  private final HttpStatus status;
  private final String title;
  private final String detail;

  AnalysisErrorCode(HttpStatus status, String title, String detail) {
    this.status = status;
    this.title = title;
    this.detail = detail;
  }

  public HttpStatus status() {
    return status;
  }

  public String title() {
    return title;
  }

  public String detail() {
    return detail;
  }
}
