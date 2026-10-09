package ai.timefold.solver.service.maps.service.client.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import ai.timefold.solver.service.maps.api.model.Waypoints;

final class VehicleRoute {

    private final Waypoints baseWaypoints;
    private final AtomicReference<CompletableFuture<Waypoints>> waypointsFutureReference = new AtomicReference<>();

    VehicleRoute(Waypoints baseWaypoints) {
        this.baseWaypoints = baseWaypoints;
    }

    Waypoints getBaseWaypoints() {
        return baseWaypoints;
    }

    boolean hasSameStops(Waypoints otherBaseWaypoints) {
        return baseWaypoints.waypoints().equals(otherBaseWaypoints.waypoints());
    }

    CompletableFuture<Waypoints> fetchOnce(Supplier<CompletableFuture<Waypoints>> fetcher) {
        var candidateFuture = new CompletableFuture<Waypoints>();
        var currentFuture = waypointsFutureReference.updateAndGet(existing -> existing == null ? candidateFuture : existing);
        if (currentFuture == candidateFuture) {
            startFetch(candidateFuture, fetcher);
        }
        return currentFuture;
    }

    private void startFetch(CompletableFuture<Waypoints> candidateFuture, Supplier<CompletableFuture<Waypoints>> fetcher) {
        var isFetchAttached = false;
        try {
            fetcher.get().whenComplete((waypoints, failure) -> {
                if (failure == null) {
                    candidateFuture.complete(waypoints);
                } else {
                    failFetch(candidateFuture, failure);
                }
            });
            isFetchAttached = true;
        } finally {
            if (!isFetchAttached) {
                failFetch(candidateFuture, new IllegalStateException("""
                        The waypoints fetch of the route (%s) failed to start.
                        Maybe see the exception of the request that started it."""
                        .formatted(baseWaypoints.id())));
            }
        }
    }

    private void failFetch(CompletableFuture<Waypoints> candidateFuture, Throwable failure) {
        waypointsFutureReference.compareAndSet(candidateFuture, null);
        candidateFuture.completeExceptionally(failure);
    }
}
