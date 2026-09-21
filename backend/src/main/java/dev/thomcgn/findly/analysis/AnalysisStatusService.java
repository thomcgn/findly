package dev.thomcgn.findly.analysis;

import dev.thomcgn.findly.error.AnalysisErrorCode;
import dev.thomcgn.findly.error.AnalysisException;
import dev.thomcgn.findly.listing.Listing;
import dev.thomcgn.findly.listing.ListingFetchResult;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class AnalysisStatusService {
  public record Job(UUID id, String url, Instant deadline) {}

  private final AnalysisRepository repository;
  private final Clock clock;

  public AnalysisStatusService(AnalysisRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  public Job create(String url) {
    Instant deadline = clock.instant().plusSeconds(300);
    Analysis analysis =
        repository.saveAndFlush(Analysis.builder().sourceUrl(url).deadlineAt(deadline).build());
    return new Job(analysis.getId(), url, deadline);
  }

  public boolean claim(UUID id) {
    Analysis analysis = locked(id);
    if (analysis.getStatus() != AnalysisStatus.CREATED) return false;
    if (expired(analysis)) {
      failLocked(analysis, AnalysisErrorCode.ANALYSIS_TIMEOUT);
      return false;
    }
    transition(analysis, AnalysisStatus.FETCHING_LISTING);
    analysis.setStartedAt(clock.instant());
    return true;
  }

  public void extracting(UUID id) {
    transition(active(id), AnalysisStatus.EXTRACTING_LISTING);
  }

  public void storeListing(UUID id, ListingFetchResult data) {
    Analysis analysis = active(id);
    transition(analysis, AnalysisStatus.IDENTIFYING_PRODUCT);
    analysis.setListing(
        Listing.builder()
            .analysis(analysis)
            .externalUrl(analysis.getSourceUrl())
            .title(data.title())
            .description(data.description())
            .listingPrice(data.price())
            .currency(data.currency())
            .imageUrls(new ArrayList<>(data.imageUrls()))
            .build());
  }

  public void storeIdentification(UUID id, dev.thomcgn.findly.product.IdentificationResult result) {
    Analysis analysis = active(id);
    transition(analysis, AnalysisStatus.RESEARCHING_PRICES);
    analysis.getWarnings().addAll(result.warnings());
    for (var attribute : result.attributes()) {
      var entity = new dev.thomcgn.findly.product.ExtractedAttribute();
      entity.setAnalysis(analysis);
      entity.setEvidence(attribute);
      analysis.getAttributes().add(entity);
    }
    int position = 0;
    for (var score : result.scores()) {
      var product = new dev.thomcgn.findly.product.Product();
      product.setCandidate(score.product());
      var match = new dev.thomcgn.findly.product.ProductMatch();
      match.setAnalysis(analysis);
      match.setProduct(product);
      match.setScore(score);
      match.setPosition(position++);
      match.setSelected(score.product().equals(result.selected()));
      analysis.getMatches().add(match);
    }
  }

  public void complete(UUID id, dev.thomcgn.findly.price.PriceResearchResult result) {
    Analysis analysis = active(id);
    if (analysis.getListing() == null)
      throw new IllegalStateException("Cannot complete without a listing result");
    analysis.getWarnings().addAll(result.warnings());
    if (!result.quotes().isEmpty()
        && analysis.getMatches().stream()
            .noneMatch(dev.thomcgn.findly.product.ProductMatch::isSelected))
      throw new IllegalStateException("Price evidence requires a selected product");
    for (var quote : result.quotes()) {
      var entity = new dev.thomcgn.findly.price.PriceEvidence();
      entity.setAnalysis(analysis);
      entity.setQuote(quote);
      analysis.getPriceEvidence().add(entity);
    }
    transition(analysis, AnalysisStatus.COMPLETED);
    analysis.setCompletedAt(clock.instant());
  }

  public void fail(UUID id, AnalysisErrorCode code) {
    Analysis analysis = locked(id);
    if (!analysis.getStatus().terminal()) failLocked(analysis, code);
  }

  public int expireOverdue() {
    return repository.expireOverdue(clock.instant(), AnalysisErrorCode.ANALYSIS_TIMEOUT.detail());
  }

  private void failLocked(Analysis analysis, AnalysisErrorCode code) {
    transition(analysis, AnalysisStatus.FAILED);
    analysis.setFailedAt(clock.instant());
    analysis.setErrorCode(code.name());
    analysis.setErrorMessage(code.detail());
  }

  private Analysis active(UUID id) {
    Analysis analysis = locked(id);
    if (analysis.getStatus().terminal() || expired(analysis))
      throw new AnalysisException(AnalysisErrorCode.ANALYSIS_TIMEOUT);
    return analysis;
  }

  private boolean expired(Analysis analysis) {
    return analysis.getDeadlineAt() != null && !analysis.getDeadlineAt().isAfter(clock.instant());
  }

  private Analysis locked(UUID id) {
    return repository
        .findForUpdate(id)
        .orElseThrow(() -> new AnalysisException(AnalysisErrorCode.ANALYSIS_NOT_FOUND));
  }

  private void transition(Analysis analysis, AnalysisStatus next) {
    if (!analysis.getStatus().canTransitionTo(next))
      throw new IllegalStateException("Invalid analysis transition");
    analysis.setStatus(next);
    if (next != AnalysisStatus.FAILED) analysis.setProgress(next.progress());
  }
}
