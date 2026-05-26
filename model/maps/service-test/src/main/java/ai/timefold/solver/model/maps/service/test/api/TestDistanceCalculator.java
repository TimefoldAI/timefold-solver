package ai.timefold.solver.model.maps.service.test.api;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

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
