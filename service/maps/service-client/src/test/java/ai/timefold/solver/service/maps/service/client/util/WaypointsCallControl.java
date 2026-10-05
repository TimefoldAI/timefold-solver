package ai.timefold.solver.service.maps.service.client.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.inject.Singleton;

import ai.timefold.solver.service.maps.service.client.impl.error.MapServiceIllegalArgumentException;

@Singleton
public class WaypointsCallControl {

    private final AtomicLong delayMillis = new AtomicLong();
    private final AtomicInteger remainingFailureCount = new AtomicInteger();
    private final AtomicInteger remainingRetryableFailureCount = new AtomicInteger();
    private final AtomicInteger concurrentCallCount = new AtomicInteger();
    private final AtomicInteger maxConcurrentCallCount = new AtomicInteger();

    public void setDelayMillis(long delayMillis) {
        this.delayMillis.set(delayMillis);
    }

    public void failNextCalls(int failureCount) {
        remainingFailureCount.set(failureCount);
    }

    public void failNextCallsRetryably(int failureCount) {
        remainingRetryableFailureCount.set(failureCount);
    }

    public int getMaxConcurrentCalls() {
        return maxConcurrentCallCount.get();
    }

    public boolean hasCallsInFlight() {
        return concurrentCallCount.get() > 0;
    }

    public void reset() {
        delayMillis.set(0);
        remainingFailureCount.set(0);
        remainingRetryableFailureCount.set(0);
        maxConcurrentCallCount.set(0);
    }

    void beforeCall() {
        maxConcurrentCallCount.accumulateAndGet(concurrentCallCount.incrementAndGet(), Math::max);
        var isCallAllowed = false;
        try {
            sleep(delayMillis.get());
            if (takeFailure(remainingFailureCount)) {
                throw new MapServiceIllegalArgumentException("TIMEFOLD-TEST", "Injected waypoints failure.", false);
            }
            if (takeFailure(remainingRetryableFailureCount)) {
                throw new IllegalStateException("Injected retryable waypoints failure.");
            }
            isCallAllowed = true;
        } finally {
            if (!isCallAllowed) {
                concurrentCallCount.decrementAndGet();
            }
        }
    }

    private static boolean takeFailure(AtomicInteger remainingCount) {
        return remainingCount.getAndUpdate(count -> Math.max(0, count - 1)) > 0;
    }

    void afterCall() {
        concurrentCallCount.decrementAndGet();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while the test delayed a waypoints call.", e);
        }
    }
}
