package ai.timefold.solver.service.maps.service.client.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import ai.timefold.solver.service.maps.api.model.Waypoints;

final class RunRoutes {

    private volatile Map<String, VehicleRoute> vehicleRouteMap = Map.of();
    private volatile boolean viewed;
    private final AtomicBoolean backgroundFetchRunning = new AtomicBoolean();
    private final AtomicBoolean backgroundFetchRequested = new AtomicBoolean();

    synchronized void replaceRoutes(List<Waypoints> baseWaypointsList) {
        var previousVehicleRouteMap = vehicleRouteMap;
        var nextVehicleRouteMap = new LinkedHashMap<String, VehicleRoute>();
        for (var baseWaypoints : baseWaypointsList) {
            var previousRoute = previousVehicleRouteMap.get(baseWaypoints.id());
            nextVehicleRouteMap.put(baseWaypoints.id(),
                    previousRoute != null && previousRoute.hasSameStops(baseWaypoints) ? previousRoute
                            : new VehicleRoute(baseWaypoints));
        }
        vehicleRouteMap = Collections.unmodifiableMap(nextVehicleRouteMap);
    }

    List<VehicleRoute> getRouteList(Set<String> objectIdSet) {
        return vehicleRouteMap.entrySet().stream()
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
