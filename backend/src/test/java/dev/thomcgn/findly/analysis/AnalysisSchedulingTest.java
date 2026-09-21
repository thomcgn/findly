package dev.thomcgn.findly.analysis;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.thomcgn.findly.config.AsyncConfig;
import dev.thomcgn.findly.error.AnalysisErrorCode;
import dev.thomcgn.findly.error.AnalysisException;
import dev.thomcgn.findly.listing.ListingFetchService;
import dev.thomcgn.findly.listing.ListingParser;
import dev.thomcgn.findly.price.PriceResearchService;
import dev.thomcgn.findly.product.ProductIdentificationService;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AnalysisSchedulingTest {
  @Test
  void fullExecutorRejectsWithoutRunningWorkInCaller() throws Exception {
    var executor = new AsyncConfig().analysisTaskExecutor();
    executor.initialize();
    var deadlines = new AsyncConfig().analysisDeadlines();
    var statuses = mock(AnalysisStatusService.class);
    var fetcher = mock(ListingFetchService.class);
    var processor =
        new AnalysisProcessingService(
            statuses,
            fetcher,
            mock(ListingParser.class),
            mock(ProductIdentificationService.class),
            mock(PriceResearchService.class),
            executor,
            deadlines,
            Clock.systemUTC());
    CountDownLatch release = new CountDownLatch(1);
    CountDownLatch running = new CountDownLatch(4);
    Runnable blocker =
        () -> {
          running.countDown();
          try {
            release.await();
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          }
        };
    try {
      for (int i = 0; i < 24; i++) executor.execute(blocker);
      assertTrue(running.await(3, TimeUnit.SECONDS));
      var job =
          new AnalysisStatusService.Job(
              UUID.randomUUID(), "https://kleinanzeigen.de/full", Instant.now().plusSeconds(300));
      assertEquals(
          AnalysisErrorCode.ANALYSIS_QUEUE_FULL,
          assertThrows(AnalysisException.class, () -> processor.schedule(job)).code());
      verify(statuses).fail(job.id(), AnalysisErrorCode.ANALYSIS_QUEUE_FULL);
      verifyNoInteractions(fetcher);
      verify(statuses, never()).claim(any());
    } finally {
      release.countDown();
      executor.destroy();
      deadlines.shutdownNow();
    }
  }

  @Test
  void deadlineInterruptsBlockedWorkerAndRecordsTimeout() throws Exception {
    var executor = new AsyncConfig().analysisTaskExecutor();
    executor.initialize();
    var deadlines = new AsyncConfig().analysisDeadlines();
    var statuses = mock(AnalysisStatusService.class);
    var fetcher = mock(ListingFetchService.class);
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch interrupted = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    when(statuses.claim(any())).thenReturn(true);
    when(fetcher.fetchHtml(any()))
        .thenAnswer(
            invocation -> {
              entered.countDown();
              try {
                release.await();
              } catch (InterruptedException ex) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
              }
              throw new AnalysisException(AnalysisErrorCode.ANALYSIS_TIMEOUT);
            });
    var processor =
        new AnalysisProcessingService(
            statuses,
            fetcher,
            mock(ListingParser.class),
            mock(ProductIdentificationService.class),
            mock(PriceResearchService.class),
            executor,
            deadlines,
            Clock.systemUTC());
    var job =
        new AnalysisStatusService.Job(
            UUID.randomUUID(), "https://kleinanzeigen.de/timeout", Instant.now().plusMillis(500));
    try {
      processor.schedule(job);
      assertTrue(entered.await(2, TimeUnit.SECONDS));
      assertTrue(interrupted.await(3, TimeUnit.SECONDS));
      verify(statuses, atLeastOnce()).fail(job.id(), AnalysisErrorCode.ANALYSIS_TIMEOUT);
    } finally {
      release.countDown();
      executor.destroy();
      deadlines.shutdownNow();
    }
  }
}
