package ai.timefold.solver.model.maps.service.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import ai.timefold.solver.model.definition.internal.error.ErrorCodes;
import ai.timefold.solver.model.maps.api.DistanceMatrix;
import ai.timefold.solver.model.maps.api.model.Location;
import ai.timefold.solver.model.maps.service.integration.internal.model.IllegalDistanceResponseException;
import ai.timefold.solver.model.maps.service.integration.internal.model.TravelTimeAndDistance;
import ai.timefold.solver.model.maps.service.integration.internal.model.TravelTimeAndDistanceConverter;
import ai.timefold.solver.model.maps.service.integration.internal.model.TravelTimeAndDistanceConverterException;
import ai.timefold.solver.model.maps.service.integration.internal.model.TravelTimeAndDistanceWithMetadata;

import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class HaversineTravelTimeAndDistanceConverter implements TravelTimeAndDistanceConverter {

    public static final String PROVIDER = "haversine";
    private final ObjectMapper mapper;

    @Inject
    public HaversineTravelTimeAndDistanceConverter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TravelTimeAndDistanceWithMetadata convert(List<Location> locations, InputStream data,
            List<Integer> locationsNotInMap) {
        try {
            return new TravelTimeAndDistanceWithMetadata(mapper.readValue(data, TravelTimeAndDistance.class),
                    locationsNotInMap);
        } catch (Exception e) {
            throw new TravelTimeAndDistanceConverterException(ErrorCodes.MAP_SERVICE_CLIENT_CONVERT_DISTANCE_RESPONSE_ERROR,
                    "Unable to convert distance matrix due to " + e.getMessage(), e);
        }
    }

    @Override
    public TravelTimeAndDistanceWithMetadata convert(List<Location> locations, InputStream data,
            List<Integer> inputStreamBytes, List<Integer> locationsNotInMap) {
        List<TravelTimeAndDistance> travelTimeAndDistances;
        try {
            travelTimeAndDistances = readChunksFromStream(mapper, data, inputStreamBytes, TravelTimeAndDistance.class);
        } catch (IOException e) {
            throw new IllegalDistanceResponseException(PROVIDER, "Could not decode distance matrix due to " + e.getMessage());
        }

        List<DistanceMatrix> travelMatrices = travelTimeAndDistances.stream().map(TravelTimeAndDistance::travelTime).toList();
        List<DistanceMatrix> distanceMatrices = travelTimeAndDistances.stream().map(TravelTimeAndDistance::distance).toList();

        return new TravelTimeAndDistanceWithMetadata(
                joinTravelTimeAndDistanceMatrices(getLocationsInMap(locations, locationsNotInMap), travelMatrices,
                        distanceMatrices),
                locationsNotInMap);
    }

    @Override
    public TravelTimeAndDistanceWithMetadata update(TravelTimeAndDistance travelTimeAndDistance, List<Location> oldLocations,
            List<Location> newLocations, InputStream data, List<Integer> inputStreamBytes, List<Integer> oldLocationsNotInMap,
            List<Integer> newLocationsNotInMap) {
        List<TravelTimeAndDistance> travelTimeAndDistances;
        try {
            travelTimeAndDistances = readChunksFromStream(mapper, data, inputStreamBytes, TravelTimeAndDistance.class);
        } catch (IOException e) {
            throw new IllegalDistanceResponseException(PROVIDER, "Could not decode distance matrix due to " + e.getMessage());
        }

        DistanceMatrix previousTravelTime = travelTimeAndDistance.travelTime();
        DistanceMatrix previousDistance = travelTimeAndDistance.distance();

        ArrayList<DistanceMatrix> allTravelMatrices = travelTimeAndDistances.stream().map(TravelTimeAndDistance::travelTime)
                .collect(Collectors.toCollection(ArrayList::new));
        ArrayList<DistanceMatrix> allDistanceMatrices = travelTimeAndDistances.stream().map(TravelTimeAndDistance::distance)
                .collect(Collectors.toCollection(ArrayList::new));

        List<Location> allLocations = Stream.concat(getLocationsInMap(oldLocations, oldLocationsNotInMap).stream(),
                getLocationsInMap(newLocations, newLocationsNotInMap).stream()).toList();
        allTravelMatrices.addFirst(previousTravelTime);
        allDistanceMatrices.addFirst(previousDistance);

        return new TravelTimeAndDistanceWithMetadata(
                joinTravelTimeAndDistanceMatrices(allLocations, allTravelMatrices, allDistanceMatrices), newLocationsNotInMap);
    }

    @Override
    public boolean canConvert(String provider) {
        return PROVIDER.equalsIgnoreCase(provider);
    }

    private TravelTimeAndDistance joinTravelTimeAndDistanceMatrices(List<Location> locations,
            List<DistanceMatrix> travelMatrices, List<DistanceMatrix> distanceMatrices) {
        DistanceMatrix travelTime = DistanceMatrix.getInstance(locations.size());
        DistanceMatrix distanceMatrix = DistanceMatrix.getInstance(locations.size());

        int firstDimLocationsProcessed = 0;
        int secondDimLocationsProcessed = 0;
        for (int i = 0; i < travelMatrices.size(); i++) {
            DistanceMatrix travelTimeHaversineMatrix = travelMatrices.get(i);
            DistanceMatrix distanceHaversineMatrix = distanceMatrices.get(i);
            int firstDimSize = travelTimeHaversineMatrix.getNumberOfOriginLocations();
            int secondDimSize = distanceHaversineMatrix.getNumberOfDestinationLocations();
            if (i == 0) {
                List<Location> locationsSource = locations.subList(0, firstDimSize);
                List<Location> locationsDestination = locations.subList(0, secondDimSize);
                updateMatrices(travelTimeHaversineMatrix, distanceHaversineMatrix, travelTime, distanceMatrix, locationsSource,
                        locationsDestination);
                firstDimLocationsProcessed += firstDimSize;
                secondDimLocationsProcessed += secondDimSize;
            } else if ((i - 1) % 3 == 0) {
                List<Location> locationsSource = locations.subList(0, firstDimSize);
                List<Location> locationsDestination =
                        locations.subList(secondDimLocationsProcessed, secondDimLocationsProcessed + secondDimSize);
                updateMatrices(travelTimeHaversineMatrix, distanceHaversineMatrix, travelTime, distanceMatrix, locationsSource,
                        locationsDestination);
                secondDimLocationsProcessed += secondDimSize;
            } else if ((i - 1) % 3 == 1) {
                List<Location> locationsSource =
                        locations.subList(firstDimLocationsProcessed, firstDimLocationsProcessed + firstDimSize);
                List<Location> locationsDestination = locations.subList(0, secondDimSize);
                updateMatrices(travelTimeHaversineMatrix, distanceHaversineMatrix, travelTime, distanceMatrix, locationsSource,
                        locationsDestination);
                firstDimLocationsProcessed += firstDimSize;
            } else {
                List<Location> locationsSource =
                        locations.subList(firstDimLocationsProcessed - firstDimSize, firstDimLocationsProcessed);
                List<Location> locationsDestination =
                        locations.subList(secondDimLocationsProcessed - secondDimSize, secondDimLocationsProcessed);
                updateMatrices(travelTimeHaversineMatrix, distanceHaversineMatrix, travelTime, distanceMatrix, locationsSource,
                        locationsDestination);
            }
        }

        return new TravelTimeAndDistance(travelTime, distanceMatrix);
    }

    private void updateMatrices(DistanceMatrix travelTimeHaversineMatrix, DistanceMatrix distanceHaversineMatrix,
            DistanceMatrix travelTimeMatrix, DistanceMatrix distanceMatrix,
            List<Location> source, List<Location> destination) {
        for (Location sourceLocation : source) {
            for (Location destinationLocation : destination) {
                long travelTime = travelTimeHaversineMatrix.get(sourceLocation, destinationLocation);
                long distance = distanceHaversineMatrix.get(sourceLocation, destinationLocation);
                travelTimeMatrix.put(sourceLocation, destinationLocation, travelTime);
                distanceMatrix.put(sourceLocation, destinationLocation, distance);
            }
        }
    }

}
