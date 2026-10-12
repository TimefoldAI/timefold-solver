package ai.timefold.solver.service.worker.impl;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import ai.timefold.solver.service.definition.impl.stats.StatisticsCollector;

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
        LOGGER.debug("Reschedule shutting down with delay of {}", duration);
        scheduledShutdownFuture =
                executor.schedule(new GracefulShutDownTask(status), duration.getSeconds(), TimeUnit.SECONDS);
    }

    private class GracefulShutDownTask implements Runnable {
        int status = 0;

        public GracefulShutDownTask(int status) {
            this.status = status;
        }

        @Override
        public void run() {

            if (scheduledDuration != null) {
                if (statisticsCollector.inflightRequestCount() > 0) {
                    // in-flight request, reschedule for short time to check after request is completed
                    LOGGER.debug("In-flight request, rescheduling to check after its completion");
                    rescheduleShutdown(Duration.ofSeconds(1), status);
                    return;
                }

                long lastRequestTimestamp = statisticsCollector.lastActivityTimestamp();
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
