package dev.thomcgn.findly.error;

public class AnalysisException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final AnalysisErrorCode code;

  public AnalysisException(AnalysisErrorCode code) {
    super(code.detail());
    this.code = code;
  }

  public AnalysisErrorCode code() {
    return code;
  }
}
