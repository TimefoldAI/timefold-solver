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
 * The cached routes of one run and its background-fetch flags.
 * The order of the flag updates matters:
 * a start sets {@code running} with a compare-and-set, then clears {@code requested}, then snapshots the routes;
 * a finish releases {@code running}, then reads {@code requested}.
 */
final class RunRoutes {

    private final AtomicReference<Map<String, Route>> routeMapReference = new AtomicReference<>(Map.of());
    private volatile boolean viewed;
    private final AtomicBoolean backgroundFetchRunning = new AtomicBoolean();
    private final AtomicBoolean backgroundFetchRequested = new AtomicBoolean();

    synchronized void replaceRoutes(List<Waypoints> baseWaypointsList) {
        var previousRouteMap = routeMapReference.get();
        var nextRouteMap = new LinkedHashMap<String, Route>();
        for (var baseWaypoints : baseWaypointsList) {
            var previousRoute = previousRouteMap.get(baseWaypoints.id());
            nextRouteMap.put(baseWaypoints.id(),
                    previousRoute != null && previousRoute.hasSameStops(baseWaypoints) ? previousRoute
                            : new Route(baseWaypoints));
        }
        routeMapReference.set(Collections.unmodifiableMap(nextRouteMap));
    }

    List<Route> getRouteList(Set<String> routeIdSet) {
        return routeMapReference.get().entrySet().stream()
                .filter(entry -> routeIdSet.isEmpty() || routeIdSet.contains(entry.getKey()))
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
