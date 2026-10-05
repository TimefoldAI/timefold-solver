package ai.timefold.solver.service.maps.service.client.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.inject.Singleton;

import ai.timefold.solver.service.maps.service.client.impl.error.MapServiceIllegalArgumentException;

@Singleton
public class WaypointsCallControl {

    private final AtomicLong delayMillis = new AtomicLong();
    private final AtomicInteger remainingFailureCount = new AtomicInteger();
    private final AtomicInteger concurrentCallCount = new AtomicInteger();
    private final AtomicInteger maxConcurrentCallCount = new AtomicInteger();

    public void setDelayMillis(long delayMillis) {
        this.delayMillis.set(delayMillis);
    }

    public void failNextCalls(int failureCount) {
        remainingFailureCount.set(failureCount);
    }

    public int getMaxConcurrentCalls() {
        return maxConcurrentCallCount.get();
    }

    public void reset() {
        delayMillis.set(0);
        remainingFailureCount.set(0);
        maxConcurrentCallCount.set(0);
    }

    void beforeCall() {
        maxConcurrentCallCount.accumulateAndGet(concurrentCallCount.incrementAndGet(), Math::max);
        sleep(delayMillis.get());
        if (remainingFailureCount.getAndUpdate(count -> Math.max(0, count - 1)) > 0) {
            concurrentCallCount.decrementAndGet();
            throw new MapServiceIllegalArgumentException("TIMEFOLD-TEST", "Injected waypoints failure.", false);
        }
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
