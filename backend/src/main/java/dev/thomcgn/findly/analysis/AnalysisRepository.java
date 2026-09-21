package dev.thomcgn.findly.analysis;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Analysis a where a.id = :id")
  Optional<Analysis> findForUpdate(UUID id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update Analysis a set a.status = dev.thomcgn.findly.analysis.AnalysisStatus.FAILED, a.failedAt = :now, a.updatedAt = :now, a.errorCode = 'ANALYSIS_TIMEOUT', a.errorMessage = :message, a.version = a.version + 1 where a.deadlineAt <= :now and a.status not in (dev.thomcgn.findly.analysis.AnalysisStatus.COMPLETED, dev.thomcgn.findly.analysis.AnalysisStatus.FAILED)")
  int expireOverdue(Instant now, String message);
}
