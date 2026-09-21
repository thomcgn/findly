package dev.thomcgn.findly.config;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableScheduling
public class AsyncConfig {
  @Bean(name = "analysisTaskExecutor")
  public ThreadPoolTaskExecutor analysisTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(20);
    executor.setThreadNamePrefix("analysis-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.setWaitForTasksToCompleteOnShutdown(false);
    return executor;
  }

  @Bean(destroyMethod = "shutdownNow")
  public ScheduledExecutorService analysisDeadlines() {
    var executor =
        new java.util.concurrent.ScheduledThreadPoolExecutor(
            2, Thread.ofPlatform().daemon().name("analysis-deadline-", 0).factory());
    executor.setRemoveOnCancelPolicy(true);
    return executor;
  }
}
