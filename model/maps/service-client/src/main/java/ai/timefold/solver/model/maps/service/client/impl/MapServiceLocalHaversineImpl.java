package ai.timefold.solver.model.maps.service.client.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;

import ai.timefold.solver.model.maps.api.model.Location;
import ai.timefold.solver.model.maps.haversine.impl.HaversineTravelTimeAndDistanceMatrixProvider;
import ai.timefold.solver.model.maps.haversine.impl.HaversineWaypointsProvider;
import ai.timefold.solver.model.maps.service.client.api.MapService;
import ai.timefold.solver.model.maps.service.integration.internal.model.TravelTimeAndDistance;
import ai.timefold.solver.model.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;
import ai.timefold.solver.model.maps.service.integration.internal.provider.WaypointsProvider;

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
    public List<Location> getWaypoints(List<Location> locations, String options) {
        return waypointsProvider.getWaypoints(locations, Collections.emptyMap());
    }

    @Override
    public List<Integer> getLocationsOutOfMap(List<Location> locations, String options) {
        return dmProvider.getLocationsOutOfMap(locations, Collections.emptyMap());
    }
}
