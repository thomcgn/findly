package dev.thomcgn.findly.filter;

import dev.thomcgn.findly.config.RateLimitProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Bounded per-instance fixed windows. Remote IPs must come from the trusted server connection. */
@Component
public class AnalysisRateLimiter {
  private record Key(String ip, boolean create) {}

  private record Window(Instant start, int count) {}

  public record Admission(boolean allowed, long retryAfterSeconds) {}

  private final RateLimitProperties properties;
  private final Clock clock;
  private final Map<Key, Window> windows = new LinkedHashMap<>();

  public AnalysisRateLimiter(RateLimitProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  public synchronized Admission acquire(String ip, boolean create) {
    Instant now = clock.instant();
    windows.entrySet().removeIf(entry -> !entry.getValue().start().plusSeconds(60).isAfter(now));
    Key key = new Key(ip, create);
    Window current = windows.get(key);
    if (current == null) {
      if (windows.size() >= properties.maxClients()) {
        return new Admission(false, 60);
      }
      windows.put(key, new Window(now, 1));
      return new Admission(true, 0);
    }
    int limit = create ? properties.createsPerMinute() : properties.readsPerMinute();
    if (current.count() >= limit) {
      long remainingMillis = Duration.between(now, current.start().plusSeconds(60)).toMillis();
      return new Admission(false, Math.max(1, (remainingMillis + 999) / 1000));
    }
    windows.put(key, new Window(current.start(), current.count() + 1));
    return new Admission(true, 0);
  }
}
