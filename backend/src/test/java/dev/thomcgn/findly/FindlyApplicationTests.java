package dev.thomcgn.findly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.thomcgn.findly.analysis.Analysis;
import dev.thomcgn.findly.analysis.AnalysisRepository;
import dev.thomcgn.findly.analysis.AnalysisStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FindlyApplicationTests {

  private final AnalysisRepository repository;

  @Autowired
  FindlyApplicationTests(AnalysisRepository repository) {
    this.repository = repository;
  }

  @Test
  void persistsProcessingMetadataAndRejectsStaleUpdates() {
    Analysis initial = repository.saveAndFlush(Analysis.builder().build());
    try {
      Analysis stale = repository.findById(initial.getId()).orElseThrow();
      Instant startedAt = Instant.parse("2026-09-18T12:00:00Z");
      Instant failedAt = startedAt.plusSeconds(30);
      initial.setStatus(AnalysisStatus.FAILED);
      initial.setProgress(100);
      initial.setStartedAt(startedAt);
      initial.setFailedAt(failedAt);
      initial.setErrorCode("LISTING_FETCH_FAILED");
      initial.setErrorMessage("Listing could not be fetched");
      Analysis saved = repository.saveAndFlush(initial);

      Analysis reloaded = repository.findById(initial.getId()).orElseThrow();
      assertEquals(AnalysisStatus.FAILED, reloaded.getStatus());
      assertEquals(100, reloaded.getProgress());
      assertEquals(startedAt, reloaded.getStartedAt());
      assertEquals(failedAt, reloaded.getFailedAt());
      assertEquals("LISTING_FETCH_FAILED", reloaded.getErrorCode());
      assertEquals("Listing could not be fetched", reloaded.getErrorMessage());
      assertEquals(initial.getVersion() + 1, saved.getVersion());
      assertEquals(saved.getVersion(), reloaded.getVersion());

      stale.setProgress(10);
      assertThrows(
          ObjectOptimisticLockingFailureException.class, () -> repository.saveAndFlush(stale));
      assertEquals(100, repository.findById(initial.getId()).orElseThrow().getProgress());
    } finally {
      repository.deleteById(initial.getId());
    }
  }
}
