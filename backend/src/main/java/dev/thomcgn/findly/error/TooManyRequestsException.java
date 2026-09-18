package dev.thomcgn.findly.error;

public class TooManyRequestsException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public TooManyRequestsException(String message) {
    super(message);
  }
}
