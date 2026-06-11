package ai.timefold.solver.service.maps.service.client.util;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Singleton;

@Singleton
public class MapServiceInvocationCounter {

    private AtomicInteger waypointsInvocationCounter = new AtomicInteger(0);

    public int getWaypointsInvocationCounter() {
        return waypointsInvocationCounter.get();
    }

    public void resetWaypointsInvocationCounter() {
        waypointsInvocationCounter.set(0);
    }

    public void incrementWaypointsInvocationCounter() {
        waypointsInvocationCounter.incrementAndGet();
    }
}
