package ai.timefold.solver.service.worker.impl;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import ai.timefold.solver.service.definition.internal.stats.StatisticsCollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.Quarkus;

@ApplicationScoped
public class ShutdownExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownExecutor.class);

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private Future<?> scheduledShutdownFuture = null;

    private StatisticsCollector statisticsCollector;

    private Duration scheduledDuration;

    private AtomicInteger rescheduleLimit = new AtomicInteger(50);

    @Inject
    public ShutdownExecutor(StatisticsCollector statisticsCollector) {
        this.statisticsCollector = statisticsCollector;
    }

    @PreDestroy
    public void close() {
        this.executor.shutdown();
    }

    public void shutdown(int status) {
        executor.submit(new GracefulShutDownTask(status));
    }

    public void scheduleShutdown(Duration duration, int status) {
        this.scheduledDuration = duration;
        scheduledShutdownFuture =
                executor.schedule(new GracefulShutDownTask(status), duration.getSeconds(), TimeUnit.SECONDS);
    }

    public void rescheduleShutdown(Duration duration, int status) {
        if (scheduledShutdownFuture != null) {
            scheduledShutdownFuture.cancel(false);
        }
        LOGGER.info("Reschedule shutting down with delay of {}", duration);
        scheduledShutdownFuture =
                executor.schedule(new GracefulShutDownTask(status), duration.getSeconds(), TimeUnit.SECONDS);
        // take one of reschedule attempts to avoid never-ending reschedules
        rescheduleLimit.decrementAndGet();
    }

    private class GracefulShutDownTask implements Runnable {
        int status = 0;

        public GracefulShutDownTask(int status) {
            this.status = status;
        }

        @Override
        public void run() {

            if (scheduledDuration != null && rescheduleLimit.get() > 0) {
                long lastRequestTimestamp = statisticsCollector.lastRequestTimestamp();

                if (lastRequestTimestamp < 0) {
                    // in-flight request, reschedule for short time to check after request is completed
                    LOGGER.debug("In-flight request, reschduling to check after its completion");
                    rescheduleShutdown(Duration.ofSeconds(1), status);
                    return;
                }

                long durationInSecondsSinceLastRequest = (System.currentTimeMillis() - lastRequestTimestamp) / 1000;

                long shutdownDifference = scheduledDuration.getSeconds() - durationInSecondsSinceLastRequest;

                if (shutdownDifference > 0) {
                    Duration delayed = Duration.ofSeconds(shutdownDifference);
                    rescheduleShutdown(delayed, status);
                    return;
                }

            }
            Quarkus.asyncExit(status);
        }
    }
}
