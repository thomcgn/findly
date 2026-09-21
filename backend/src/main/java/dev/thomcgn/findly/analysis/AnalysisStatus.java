package dev.thomcgn.findly.analysis;

public enum AnalysisStatus {
  CREATED(0),
  FETCHING_LISTING(10),
  EXTRACTING_LISTING(30),
  IDENTIFYING_PRODUCT(50),
  RESEARCHING_PRICES(75),
  COMPLETED(100),
  FAILED(100);

  private final int progress;

  AnalysisStatus(int progress) {
    this.progress = progress;
  }

  public int progress() {
    return progress;
  }

  public boolean terminal() {
    return this == COMPLETED || this == FAILED;
  }

  public boolean canTransitionTo(AnalysisStatus next) {
    if (terminal()) return false;
    if (next == FAILED) return true;
    return switch (this) {
      case CREATED -> next == FETCHING_LISTING;
      case FETCHING_LISTING -> next == EXTRACTING_LISTING;
      case EXTRACTING_LISTING -> next == IDENTIFYING_PRODUCT;
      case IDENTIFYING_PRODUCT -> next == RESEARCHING_PRICES;
      case RESEARCHING_PRICES -> next == COMPLETED;
      default -> false;
    };
  }
}
