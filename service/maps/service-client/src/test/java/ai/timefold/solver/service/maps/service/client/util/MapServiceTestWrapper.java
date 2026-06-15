package ai.timefold.solver.service.maps.service.client.util;

import java.util.List;
import java.util.Map;

import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.api.model.TimeInterval;
import ai.timefold.solver.service.maps.service.client.api.MapService;
import ai.timefold.solver.service.maps.service.client.api.model.TravelTimesByAvailabilityWithMetadata;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;

public class MapServiceTestWrapper implements MapService {

    private MapService delegate;
    private MapServiceInvocationCounter mapServiceInvocationCounter;

    public MapServiceTestWrapper(MapService delegate, MapServiceInvocationCounter mapServiceInvocationCounter) {
        this.delegate = delegate;
        this.mapServiceInvocationCounter = mapServiceInvocationCounter;
    }

    @Override
    public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
        return delegate.getTravelTimeAndDistance(locations, options);
    }

    @Override
    public TravelTimesByAvailabilityWithMetadata getTravelTimeAndDistance(List<Location> locations, String options,
            Map<Location, List<TimeInterval>> timeAvailability) {
        return delegate.getTravelTimeAndDistance(locations, options, timeAvailability);
    }

    @Override
    public List<Location> getWaypoints(List<Location> locations, String options) {
        mapServiceInvocationCounter.incrementWaypointsInvocationCounter();
        return delegate.getWaypoints(locations, options);
    }

    @Override
    public List<Integer> getLocationsOutOfMap(List<Location> locations, String options) {
        return delegate.getLocationsOutOfMap(locations, options);
    }

}
