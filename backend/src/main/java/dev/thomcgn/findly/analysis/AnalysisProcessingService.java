package dev.thomcgn.findly.analysis;

import dev.thomcgn.findly.error.AnalysisErrorCode;
import dev.thomcgn.findly.error.AnalysisException;
import dev.thomcgn.findly.listing.ListingFetchService;
import dev.thomcgn.findly.listing.ListingParser;
import dev.thomcgn.findly.price.PriceResearchService;
import dev.thomcgn.findly.product.ProductIdentificationService;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AnalysisProcessingService {
  private final AnalysisStatusService statuses;
  private final ListingFetchService fetcher;
  private final ListingParser parser;
  private final ProductIdentificationService products;
  private final PriceResearchService prices;
  private final Executor executor;
  private final ScheduledExecutorService deadlines;
  private final Clock clock;

  public AnalysisProcessingService(
      AnalysisStatusService statuses,
      ListingFetchService fetcher,
      ListingParser parser,
      ProductIdentificationService products,
      PriceResearchService prices,
      @Qualifier("analysisTaskExecutor") Executor executor,
      ScheduledExecutorService deadlines,
      Clock clock) {
    this.statuses = statuses;
    this.fetcher = fetcher;
    this.parser = parser;
    this.products = products;
    this.prices = prices;
    this.executor = executor;
    this.deadlines = deadlines;
    this.clock = clock;
  }

  public void schedule(AnalysisStatusService.Job job) {
    AtomicReference<ScheduledFuture<?>> deadline = new AtomicReference<>();
    FutureTask<Void> task =
        new FutureTask<>(
            () -> {
              try {
                process(job.id(), job.url());
              } finally {
                var timer = deadline.get();
                if (timer != null) timer.cancel(false);
              }
              return null;
            });
    try {
      var timer =
          deadlines.schedule(
              () -> {
                try {
                  statuses.fail(job.id(), AnalysisErrorCode.ANALYSIS_TIMEOUT);
                } finally {
                  task.cancel(true);
                }
              },
              Math.max(0, Duration.between(clock.instant(), job.deadline()).toMillis()),
              TimeUnit.MILLISECONDS);
      deadline.set(timer);
      executor.execute(task);
      if (task.isDone()) timer.cancel(false);
    } catch (RejectedExecutionException ex) {
      task.cancel(true);
      var timer = deadline.get();
      if (timer != null) timer.cancel(false);
      statuses.fail(job.id(), AnalysisErrorCode.ANALYSIS_QUEUE_FULL);
      throw new AnalysisException(AnalysisErrorCode.ANALYSIS_QUEUE_FULL);
    }
  }

  public void process(UUID id, String url) {
    if (TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Analysis orchestration must not run in a transaction");
    try {
      if (!statuses.claim(id)) return;
      String html = fetcher.fetchHtml(url);
      statuses.extracting(id);
      var data = parser.parse(html);
      statuses.storeListing(id, data);
      var identification = products.identify(data);
      statuses.storeIdentification(id, identification);
      statuses.complete(id, prices.research(identification.selected()));
    } catch (AnalysisException ex) {
      statuses.fail(id, ex.code());
    } catch (RuntimeException ex) {
      statuses.fail(id, AnalysisErrorCode.ANALYSIS_FAILED);
    }
  }

  @Scheduled(fixedDelay = 1000)
  public void expireOverdue() {
    statuses.expireOverdue();
  }
}
