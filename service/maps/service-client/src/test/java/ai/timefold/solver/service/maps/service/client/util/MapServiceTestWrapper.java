package ai.timefold.solver.service.maps.service.client.util;

import java.util.List;

import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.service.client.api.MapService;
import ai.timefold.solver.service.maps.service.client.api.model.TravelTimesByTimeframeWithMetadata;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;

public class MapServiceTestWrapper implements MapService {

    private MapService delegate;
    private MapServiceInvocationCounter mapServiceInvocationCounter;
    private WaypointsCallControl waypointsCallControl;

    public MapServiceTestWrapper(MapService delegate, MapServiceInvocationCounter mapServiceInvocationCounter,
            WaypointsCallControl waypointsCallControl) {
        this.delegate = delegate;
        this.mapServiceInvocationCounter = mapServiceInvocationCounter;
        this.waypointsCallControl = waypointsCallControl;
    }

    @Override
    public TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options) {
        return delegate.getTravelTimeAndDistance(locations, options);
    }

    @Override
    public TravelTimesByTimeframeWithMetadata getTravelTimeAndDistanceByTimeframe(List<Location> locations,
            String options) {
        return delegate.getTravelTimeAndDistanceByTimeframe(locations, options);
    }

    @Override
    public List<Location> getWaypoints(List<Location> locations, String options) {
        mapServiceInvocationCounter.incrementWaypointsInvocationCounter();
        waypointsCallControl.beforeCall();
        try {
            return delegate.getWaypoints(locations, options);
        } finally {
            waypointsCallControl.afterCall();
        }
    }

    @Override
    public List<Integer> getLocationsOutOfMap(List<Location> locations, String options) {
        return delegate.getLocationsOutOfMap(locations, options);
    }

}
