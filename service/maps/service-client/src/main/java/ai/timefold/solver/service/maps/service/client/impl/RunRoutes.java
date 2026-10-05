package ai.timefold.solver.service.maps.service.client.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import ai.timefold.solver.service.maps.api.model.Waypoints;

/**
 * The cached routes of one run and its background-fetch flags. Order matters: start sets {@code running} (CAS), then clears
 * {@code requested}, then snapshots the routes; finish releases {@code running}, then reads {@code requested}.
 */
final class RunRoutes {

    private final AtomicReference<Map<String, VehicleRoute>> vehicleRouteMapReference = new AtomicReference<>(Map.of());
    private volatile boolean viewed;
    private final AtomicBoolean backgroundFetchRunning = new AtomicBoolean();
    private final AtomicBoolean backgroundFetchRequested = new AtomicBoolean();

    synchronized void replaceRoutes(List<Waypoints> baseWaypointsList) {
        var previousVehicleRouteMap = vehicleRouteMapReference.get();
        var nextVehicleRouteMap = new LinkedHashMap<String, VehicleRoute>();
        for (var baseWaypoints : baseWaypointsList) {
            var previousRoute = previousVehicleRouteMap.get(baseWaypoints.id());
            nextVehicleRouteMap.put(baseWaypoints.id(),
                    previousRoute != null && previousRoute.hasSameStops(baseWaypoints) ? previousRoute
                            : new VehicleRoute(baseWaypoints));
        }
        vehicleRouteMapReference.set(Collections.unmodifiableMap(nextVehicleRouteMap));
    }

    List<VehicleRoute> getRouteList(Set<String> objectIdSet) {
        return vehicleRouteMapReference.get().entrySet().stream()
                .filter(entry -> objectIdSet.isEmpty() || objectIdSet.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
    }

    void markViewed() {
        viewed = true;
    }

    boolean isViewed() {
        return viewed;
    }

    void requestBackgroundFetch() {
        backgroundFetchRequested.set(true);
    }

    boolean tryStartBackgroundFetch() {
        if (backgroundFetchRunning.compareAndSet(false, true)) {
            backgroundFetchRequested.set(false);
            return true;
        }
        return false;
    }

    void finishBackgroundFetch() {
        backgroundFetchRunning.set(false);
    }

    boolean isBackgroundFetchRequested() {
        return backgroundFetchRequested.get();
    }
}
