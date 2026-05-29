package ai.timefold.solver.model.maps.service.test.api;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

import ai.timefold.solver.model.maps.api.DistanceMatrix;
import ai.timefold.solver.model.maps.api.model.Location;
import ai.timefold.solver.model.maps.service.integration.internal.model.TravelTimeAndDistance;

public class TestDistanceCalculator {

    /**
     * The travel time of an unreachable route to be used by the travel time/distance matrix.
     */
    public static long UNREACHABLE_TIME = Long.MAX_VALUE;

    /**
     * The travel distance of an unreachable route to be used by the travel time/distance matrix.
     */
    public static long UNREACHABLE_DISTANCE = Long.MAX_VALUE;

    public static void initDistanceMaps(List<Location> locations,
            BiFunction<Location, Location, Long> distanceProvider,
            BiFunction<Location, Location, Long> travelTimeProvider) {
        TravelTimeAndDistance distanceMatrices = calculateBulkDistance(locations, locations,
                distanceProvider, travelTimeProvider);
        locations.forEach(location -> {
            location.setTravelTimeMatrix(distanceMatrices.travelTime());
            location.setDistanceMatrix(distanceMatrices.distance());
        });
    }

    /**
     * Traffic-aware variant of {@link #initDistanceMaps}: builds one travel-time matrix and one distance matrix per
     * timeframe and wires them onto each location together with a resolver that maps an {@link OffsetDateTime} to the
     * matching array index. The caller defines the bucketing — for the production three-bucket morning/afternoon/night
     * split, pass {@code new StaticDaypartBucketing()::indexOf} as {@code indexResolver} and three providers in the same
     * order as that bucketing's {@code allTimeframes()}.
     *
     * @param distanceProviders one {@code (from, to) -> distance} per timeframe; size and order define the timeframe
     *        array layout (matched against {@code indexResolver}). Must be the same size as {@code travelTimeProviders}.
     * @param travelTimeProviders one {@code (from, to) -> travel time} per timeframe; same size and order as
     *        {@code distanceProviders}.
     * @param indexResolver maps a query instant to the timeframe array index used by the matrices above.
     */
    public static void initDistanceMaps(List<Location> locations,
            List<BiFunction<Location, Location, Long>> distanceProviders,
            List<BiFunction<Location, Location, Long>> travelTimeProviders,
            ToIntFunction<OffsetDateTime> indexResolver) {
        if (distanceProviders.size() != travelTimeProviders.size()) {
            throw new IllegalArgumentException(("distanceProviders (size %d) and travelTimeProviders (size %d) must " +
                    "have the same size; one entry per timeframe.")
                    .formatted(distanceProviders.size(), travelTimeProviders.size()));
        }
        int timeframeCount = distanceProviders.size();
        DistanceMatrix[] travelTimesByTimeframe = new DistanceMatrix[timeframeCount];
        DistanceMatrix[] distancesByTimeframe = new DistanceMatrix[timeframeCount];
        for (int idx = 0; idx < timeframeCount; idx++) {
            TravelTimeAndDistance matrices = calculateBulkDistance(locations, locations,
                    distanceProviders.get(idx), travelTimeProviders.get(idx));
            travelTimesByTimeframe[idx] = matrices.travelTime();
            distancesByTimeframe[idx] = matrices.distance();
        }
        locations.forEach(location -> {
            location.setTravelTimeMatrices(travelTimesByTimeframe, indexResolver);
            location.setDistanceMatrices(distancesByTimeframe, indexResolver);
        });
    }

    private static TravelTimeAndDistance calculateBulkDistance(
            Collection<Location> fromLocations,
            Collection<Location> toLocations,
            BiFunction<Location, Location, Long> distanceProvider,
            BiFunction<Location, Location, Long> travelTimeProvider) {
        Set<Location> locationSet = Collections.newSetFromMap(new IdentityHashMap<>());
        locationSet.addAll(fromLocations);
        locationSet.addAll(toLocations);

        DistanceMatrix distanceMatrix = DistanceMatrix.getInstance(locationSet.size());
        DistanceMatrix travelTimeMatrix = DistanceMatrix.getInstance(locationSet.size());
        TravelTimeAndDistance distanceMatrixCollection = new TravelTimeAndDistance(travelTimeMatrix, distanceMatrix);

        for (Location fromLocation : fromLocations) {
            for (Location toLocation : toLocations) {
                updateDistanceMatrices(fromLocation, toLocation, distanceMatrixCollection, distanceProvider,
                        travelTimeProvider);
            }
        }
        return distanceMatrixCollection;
    }

    private static void updateDistanceMatrices(Location from, Location to,
            TravelTimeAndDistance travelTimeAndDistance,
            BiFunction<Location, Location, Long> distanceProvider,
            BiFunction<Location, Location, Long> travelTimeProvider) {
        travelTimeAndDistance.distance().put(from, to, distanceProvider.apply(from, to));
        travelTimeAndDistance.travelTime().put(from, to, travelTimeProvider.apply(from, to));
    }
}
