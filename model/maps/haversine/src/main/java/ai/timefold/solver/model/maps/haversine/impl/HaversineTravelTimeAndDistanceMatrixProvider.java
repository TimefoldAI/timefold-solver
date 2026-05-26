package ai.timefold.solver.model.maps.haversine.impl;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import ai.timefold.solver.model.definition.internal.error.ErrorCodes;
import ai.timefold.solver.model.definition.internal.error.TimefoldRuntimeException;
import ai.timefold.solver.model.maps.api.DistanceMatrix;
import ai.timefold.solver.model.maps.api.model.Location;
import ai.timefold.solver.model.maps.service.integration.internal.model.TravelTimeAndDistance;
import ai.timefold.solver.model.maps.service.integration.internal.provider.TravelTimeAndDistanceMatrixProvider;
import ai.timefold.solver.model.maps.service.integration.internal.provider.TravelTimeAndDistanceMatrixResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class HaversineTravelTimeAndDistanceMatrixProvider extends HaversineProviderIdentification
        implements TravelTimeAndDistanceMatrixProvider {

    private static final int EARTH_RADIUS_IN_M = 6371000;
    private static final int TWICE_EARTH_RADIUS_IN_M = 2 * EARTH_RADIUS_IN_M;
    public static final int AVERAGE_SPEED_KMPH = 50;

    private ObjectMapper mapper;

    @Inject
    public HaversineTravelTimeAndDistanceMatrixProvider(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TravelTimeAndDistanceMatrixResponse calculateTravelTimeAndDistance(List<Location> locations,
            Map<String, String> options) {
        return calculateTravelTimeAndDistance(locations, locations, options);
    }

    @Override
    public List<Integer> getLocationsOutOfMap(List<Location> locations, Map<String, String> options) {
        return List.of();
    }

    @Override
    public TravelTimeAndDistanceMatrixResponse calculateTravelTimeAndDistance(List<Location> locationsSource,
            List<Location> locationsDestination,
            Map<String, String> options) {
        TravelTimeAndDistance data = calculateBulkDistance(locationsSource, locationsDestination);

        try {
            return new TravelTimeAndDistanceMatrixResponse(new ByteArrayInputStream(mapper.writeValueAsBytes(data)),
                    Collections.emptyList());
        } catch (JsonProcessingException e) {
            throw new TimefoldRuntimeException(ErrorCodes.MAP_SERVICE_PROVIDER_ERROR,
                    "Unable to write response " + e.getMessage(), e);
        }
    }

    public TravelTimeAndDistance calculateBulkDistance(
            Collection<Location> fromLocations,
            Collection<Location> toLocations) {
        Set<Location> locationSet = Collections.newSetFromMap(new IdentityHashMap<>());
        locationSet.addAll(fromLocations);
        locationSet.addAll(toLocations);
        DistanceMatrix distanceMatrix = DistanceMatrix.getInstance(locationSet.size());
        DistanceMatrix travelTimeMatrix = DistanceMatrix.getInstance(locationSet.size());
        TravelTimeAndDistance distanceMatrixCollection = new TravelTimeAndDistance(travelTimeMatrix, distanceMatrix);
        for (Location fromLocation : fromLocations) {
            for (Location toLocation : toLocations) {
                updateDistanceMatrices(fromLocation, toLocation, distanceMatrixCollection);
            }
        }
        return distanceMatrixCollection;
    }

    public static long metersToDrivingSeconds(long meters) {
        return Math.round((double) meters / AVERAGE_SPEED_KMPH * 3.6);
    }

    public long calculateDistance(Location from, Location to) {
        if (from.equals(to)) {
            return 0;
        }

        CartesianCoordinate fromCartesian = locationToCartesian(from);
        CartesianCoordinate toCartesian = locationToCartesian(to);
        return calculateDistance(fromCartesian, toCartesian);
    }

    public long calculateTravelTime(Location from, Location to) {
        return metersToDrivingSeconds(calculateDistance(from, to));
    }

    private void updateDistanceMatrices(Location from, Location to, TravelTimeAndDistance travelTimeAndDistance) {
        long distance = calculateDistance(from, to);
        travelTimeAndDistance.distance().put(from, to, distance);
        travelTimeAndDistance.travelTime().put(from, to, metersToDrivingSeconds(distance));
    }

    private long calculateDistance(CartesianCoordinate from, CartesianCoordinate to) {
        if (from.equals(to)) {
            return 0;
        }

        double dX = from.x - to.x;
        double dY = from.y - to.y;
        double dZ = from.z - to.z;
        double r = Math.sqrt((dX * dX) + (dY * dY) + (dZ * dZ));
        return Math.round(TWICE_EARTH_RADIUS_IN_M * Math.asin(r));
    }

    private CartesianCoordinate locationToCartesian(Location location) {
        double latitudeInRads = Math.toRadians(location.getLatitude());
        double longitudeInRads = Math.toRadians(location.getLongitude());
        // Cartesian coordinates, normalized for a sphere of diameter 1.0
        double cartesianX = 0.5 * Math.cos(latitudeInRads) * Math.sin(longitudeInRads);
        double cartesianY = 0.5 * Math.cos(latitudeInRads) * Math.cos(longitudeInRads);
        double cartesianZ = 0.5 * Math.sin(latitudeInRads);
        return new CartesianCoordinate(cartesianX, cartesianY, cartesianZ);
    }

    private record CartesianCoordinate(double x, double y, double z) {
    }
}
