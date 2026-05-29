package ai.timefold.solver.model.maps.service.client.api;

import java.util.List;
import java.util.Map;

import ai.timefold.solver.model.maps.api.model.Location;
import ai.timefold.solver.model.maps.api.model.TimeInterval;
import ai.timefold.solver.model.maps.service.client.api.model.TravelTimesByAvailabilityWithMetadata;
import ai.timefold.solver.model.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;

public interface MapService {

    TravelTimeAndDistanceWithMetadata getTravelTimeAndDistance(List<Location> locations, String options);

    /**
     * Fetches one travel-time matrix and one distance matrix per timeframe needed by the given availability map. Each
     * timeframe request is pruned to the locations that will be queried in that timeframe. The bucketing used is
     * returned in the result so downstream code can stamp the same resolver onto the locations and guarantee lookup-time
     * resolution matches build-time resolution.
     *
     * @param timeAvailability for each location, the time intervals during which it may be involved in travel. Each
     *        {@link TimeInterval} is half-open {@code [from, to)} and is mapped to the set of timeframe buckets it
     *        overlaps, and the location is included in each of those timeframe matrices. E.g. for a location {@code L}
     *        with interval {@code [07:00, 13:00)} under morning/afternoon/night bucketing, {@code L} appears in both
     *        the morning and afternoon matrices but not in the night matrix. Locations not listed (or listed with an
     *        empty interval list) are excluded from every timeframe; if no intervals are provided across the whole map,
     *        an {@link IllegalArgumentException} is thrown.
     */
    TravelTimesByAvailabilityWithMetadata getTravelTimeAndDistance(List<Location> locations, String options,
            Map<Location, List<TimeInterval>> timeAvailability);

    List<Location> getWaypoints(List<Location> locations, String options);

    List<Integer> getLocationsOutOfMap(List<Location> locations, String options);

}
