package ai.timefold.solver.service.maps.service.client.api;

import java.util.List;

import ai.timefold.solver.service.maps.api.model.Location;
import ai.timefold.solver.service.maps.service.client.api.model.TravelTimesByTimeframeWithMetadata;
import ai.timefold.solver.service.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;

public interface MapService {

    TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options);

    /**
     * Fetches one travel-time matrix and one distance matrix per timeframe, with every given location included in every
     * timeframe. The bucketing used is returned in the result so downstream code can stamp the same resolver onto the
     * locations and guarantee lookup-time resolution matches build-time resolution.
     * <p>
     * With traffic disabled, returns a single non-traffic matrix wrapped as a one-bucket array (resolver always
     * returns 0).
     */
    TravelTimesByTimeframeWithMetadata getTravelTimeAndDistanceByTimeframe(List<Location> locations, String options);

    List<Location> getWaypoints(List<Location> locations, String options);

    List<Integer> getLocationsOutOfMap(List<Location> locations, String options);

}
