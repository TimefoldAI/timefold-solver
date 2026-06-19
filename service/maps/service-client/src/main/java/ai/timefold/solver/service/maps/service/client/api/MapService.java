package ai.timefold.solver.service.maps.service.client.api;

import java.util.List;

import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;

public interface MapService {

    TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options);

    List<Location> getWaypoints(List<Location> locations, String options);

    List<Integer> getLocationsOutOfMap(List<Location> locations, String options);

}
