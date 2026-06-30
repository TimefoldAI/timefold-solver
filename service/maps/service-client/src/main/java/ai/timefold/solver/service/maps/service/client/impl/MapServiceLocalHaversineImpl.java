package ai.timefold.solver.service.maps.service.client.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import ai.timefold.solver.service.maps.api.DistanceMatrix;
import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.haversine.impl.HaversineTravelTimeAndDistanceMatrixProvider;
import ai.timefold.solver.service.maps.haversine.impl.HaversineWaypointsProvider;
import ai.timefold.solver.service.maps.service.client.api.MapService;
import ai.timefold.solver.service.maps.service.client.api.model.TravelTimesByAvailabilityWithMetadata;
import ai.timefold.solver.service.maps.service.client.impl.bucketing.SingleTimeframeBucketing;
import ai.timefold.solver.service.maps.service.client.impl.bucketing.TimeframeBucketing;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistance;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;
import ai.timefold.solver.service.maps.service.integration.internal.provider.WaypointsProvider;

public class MapServiceLocalHaversineImpl implements MapService {

    private final HaversineTravelTimeAndDistanceMatrixProvider dmProvider;
    private final WaypointsProvider waypointsProvider;

    @Inject
    public MapServiceLocalHaversineImpl(HaversineTravelTimeAndDistanceMatrixProvider travelTimeAndDistanceProvider,
            HaversineWaypointsProvider waypointsProvider) {
        this.dmProvider = travelTimeAndDistanceProvider;
        this.waypointsProvider = waypointsProvider;
    }

    @Override
    public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
        TravelTimeAndDistance travelTimeAndDistance = dmProvider.calculateBulkDistance(locations, locations);
        return new TravelTimeAndDistanceWithMetadata(travelTimeAndDistance, new ArrayList<>());
    }

    @Override
    public TravelTimesByAvailabilityWithMetadata getTravelTimeAndDistanceByTimeframe(List<Location> locations,
            String options) {
        // Haversine is timeframe-independent by definition, so we use a single-bucket bucketing: a single entry in the
        // arrays covers every lookup, and the index resolver always returns 0.
        TimeframeBucketing bucketing = new SingleTimeframeBucketing();
        TravelTimeAndDistanceWithMetadata result = getTravelTimeAndDistance(locations, options);
        DistanceMatrix[] travelTimesByTimeframe = { result.travelTimeAndDistance().travelTime() };
        DistanceMatrix[] distancesByTimeframe = { result.travelTimeAndDistance().distance() };

        Set<Location> notInMapSet = new HashSet<>();
        for (int index : result.locationsNotInMapIdx()) {
            if (index >= 0 && index < locations.size()) {
                notInMapSet.add(locations.get(index));
            }
        }
        List<Location> locationsNotInMap = locations.stream().filter(notInMapSet::contains).toList();
        return new TravelTimesByAvailabilityWithMetadata(travelTimesByTimeframe, distancesByTimeframe,
                locationsNotInMap, bucketing::indexOf);
    }

    @Override
    public List<Location> getWaypoints(List<Location> locations, String options) {
        return waypointsProvider.getWaypoints(locations, Collections.emptyMap());
    }

    @Override
    public List<Integer> getLocationsOutOfMap(List<Location> locations, String options) {
        return dmProvider.getLocationsOutOfMap(locations, Collections.emptyMap());
    }
}
