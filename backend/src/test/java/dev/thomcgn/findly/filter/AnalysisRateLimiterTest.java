package dev.thomcgn.findly.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.thomcgn.findly.config.RateLimitProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class AnalysisRateLimiterTest {
  @Test
  void concurrentRequestsCannotExceedTheBudget() throws Exception {
    var limiter = new AnalysisRateLimiter(new RateLimitProperties(20, 120, 100), Clock.systemUTC());
    try (var executor = Executors.newFixedThreadPool(8)) {
      var requests =
          IntStream.range(0, 100)
              .mapToObj(i -> (Callable<Boolean>) () -> limiter.acquire("192.0.2.1", true).allowed())
              .toList();
      int admitted = 0;
      for (var result : executor.invokeAll(requests)) {
        if (result.get()) admitted++;
      }
      assertEquals(20, admitted);
    }
  }

  @Test
  void expiresWindowsAndKeepsReadsAndOtherClientsIndependent() {
    Clock clock = mock(Clock.class);
    Instant start = Instant.parse("2026-09-19T12:00:00Z");
    when(clock.instant()).thenReturn(start);
    var limiter = new AnalysisRateLimiter(new RateLimitProperties(1, 2, 10), clock);
    assertTrue(limiter.acquire("a", true).allowed());
    assertFalse(limiter.acquire("a", true).allowed());
    assertTrue(limiter.acquire("a", false).allowed());
    assertTrue(limiter.acquire("a", false).allowed());
    assertFalse(limiter.acquire("a", false).allowed());
    assertTrue(limiter.acquire("b", true).allowed());
    when(clock.instant()).thenReturn(start.plusMillis(59500));
    assertEquals(1, limiter.acquire("a", true).retryAfterSeconds());
    when(clock.instant()).thenReturn(start.plusSeconds(60));
    assertTrue(limiter.acquire("a", true).allowed());
  }

  @Test
  void capsClientStorageAndReclaimsExpiredEntries() {
    Clock clock = mock(Clock.class);
    Instant start = Instant.parse("2026-09-19T12:00:00Z");
    when(clock.instant()).thenReturn(start);
    var limiter = new AnalysisRateLimiter(new RateLimitProperties(1, 2, 1), clock);
    assertTrue(limiter.acquire("a", true).allowed());
    assertFalse(limiter.acquire("b", true).allowed());
    when(clock.instant()).thenReturn(start.plusSeconds(60));
    assertTrue(limiter.acquire("b", true).allowed());
  }
}
