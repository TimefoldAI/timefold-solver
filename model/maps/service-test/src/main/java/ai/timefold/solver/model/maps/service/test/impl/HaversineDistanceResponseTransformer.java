package ai.timefold.solver.model.maps.service.test.impl;

import static ai.timefold.solver.model.definition.internal.Headers.X_MAPS_LOCATIONS_CHUNK_BYTES;
import static ai.timefold.solver.model.definition.internal.Headers.X_MAPS_LOCATIONS_NOT_IN_MAP;
import static ai.timefold.solver.model.definition.internal.Headers.X_MAPS_LOCATION_HEADER;
import static ai.timefold.solver.model.definition.internal.Headers.X_MAPS_MATRIX_HASH_HEADER;
import static ai.timefold.solver.model.definition.internal.Headers.X_MAPS_PROVIDER_HEADER;
import static ai.timefold.solver.model.definition.internal.Headers.X_MAPS_RESPONSE_CHUNK_BYTES;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import ai.timefold.solver.model.maps.api.model.Location;
import ai.timefold.solver.model.maps.haversine.impl.HaversineTravelTimeAndDistanceMatrixProvider;
import ai.timefold.solver.model.maps.service.integration.internal.model.TravelTimeAndDistance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

public class HaversineDistanceResponseTransformer implements ResponseDefinitionTransformerV2 {

    public static String TRANSFORMER_NAME = "haversine-distance-response-transformer";
    public static final String RESOLVED_MAP_LOCATION = "us-georgia";
    private static final List<Location> locationsOutOfMap = List.of(new Location(-90, -90));
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HaversineTravelTimeAndDistanceMatrixProvider provider =
            new HaversineTravelTimeAndDistanceMatrixProvider(objectMapper);

    @Override
    public ResponseDefinition transform(ServeEvent serveEvent) {
        try {
            List<Location> allLocations = parseStringToLocationList(serveEvent.getRequest().getBodyAsString());
            List<Location> requestLocations = new ArrayList<>();
            List<Integer> locationsNotInMap = new ArrayList<>();

            for (int i = 0; i < allLocations.size(); i++) {
                Location location = allLocations.get(i);
                if (locationsOutOfMap.contains(location)) {
                    locationsNotInMap.add(i);
                } else {
                    requestLocations.add(location);
                }
            }

            if (requestLocations.size() < 3) {
                TravelTimeAndDistance travelTimeAndDistance =
                        objectMapper.readValue(provider.calculateTravelTimeAndDistance(requestLocations,
                                Collections.emptyMap()).response(), TravelTimeAndDistance.class);

                String matricesString = convertTravelTimeAndDistanceToJsonString(travelTimeAndDistance);
                byte[] locationBytes = objectMapper.writeValueAsBytes(requestLocations);
                List<Integer> metadataChunkBytes = List.of(locationBytes.length);
                InputStream response = new SequenceInputStream(new ByteArrayInputStream(locationBytes),
                        new ByteArrayInputStream(matricesString.getBytes()));

                return new ResponseDefinitionBuilder()
                        .withHeader("Content-Type", "application/json")
                        .withHeader(X_MAPS_PROVIDER_HEADER, provider.getProvider())
                        .withHeader(X_MAPS_LOCATION_HEADER, RESOLVED_MAP_LOCATION)
                        .withHeader(X_MAPS_MATRIX_HASH_HEADER, "hash")
                        .withHeader(X_MAPS_LOCATIONS_CHUNK_BYTES,
                                metadataChunkBytes.stream().map(Object::toString).collect(Collectors.joining(",")))
                        .withHeader(X_MAPS_LOCATIONS_NOT_IN_MAP, locationsNotInMap.stream().map(Object::toString)
                                .collect(Collectors.joining(",")))
                        .withStatus(200)
                        .withBody(response.readAllBytes())
                        .build();
            } else {
                // Simulate distance matrix with updates
                List<Location> locations1 = requestLocations.subList(0, 2);
                List<Location> locations2 = requestLocations.subList(2, requestLocations.size());

                byte[] location1Bytes = objectMapper.writeValueAsBytes(locations1);
                byte[] location2Bytes = objectMapper.writeValueAsBytes(locations2);
                List<Integer> metadataChunkBytes = List.of(location1Bytes.length, location2Bytes.length, 0, 0);
                InputStream location1InputStream = new ByteArrayInputStream(location1Bytes);
                InputStream location2InputStream = new ByteArrayInputStream(location2Bytes);
                InputStream locationsInputStream = new SequenceInputStream(location1InputStream, location2InputStream);

                TravelTimeAndDistance travelTimeAndDistance1 =
                        objectMapper.readValue(provider.calculateTravelTimeAndDistance(locations1, locations1,
                                Collections.emptyMap()).response(), TravelTimeAndDistance.class);
                TravelTimeAndDistance travelTimeAndDistance2 =
                        objectMapper.readValue(provider.calculateTravelTimeAndDistance(locations1, locations2,
                                Collections.emptyMap()).response(), TravelTimeAndDistance.class);
                TravelTimeAndDistance travelTimeAndDistance3 =
                        objectMapper.readValue(provider.calculateTravelTimeAndDistance(locations2, locations1,
                                Collections.emptyMap()).response(), TravelTimeAndDistance.class);
                TravelTimeAndDistance travelTimeAndDistance4 =
                        objectMapper.readValue(provider.calculateTravelTimeAndDistance(locations2, locations2,
                                Collections.emptyMap()).response(), TravelTimeAndDistance.class);

                InputStreamAndChunkBytes response = joinResponsesIntoInputStream(travelTimeAndDistance1,
                        travelTimeAndDistance2, travelTimeAndDistance3, travelTimeAndDistance4);

                InputStream data = new SequenceInputStream(locationsInputStream, response.inputStream);

                return new ResponseDefinitionBuilder()
                        .withHeader("Content-Type", "application/json")
                        .withHeader(X_MAPS_PROVIDER_HEADER, provider.getProvider())
                        .withHeader(X_MAPS_LOCATION_HEADER, RESOLVED_MAP_LOCATION)
                        .withHeader(X_MAPS_RESPONSE_CHUNK_BYTES,
                                response.chunkBytes().stream().map(Object::toString).collect(Collectors.joining(",")))
                        .withHeader(X_MAPS_MATRIX_HASH_HEADER, "hash")
                        .withHeader(X_MAPS_LOCATIONS_CHUNK_BYTES,
                                metadataChunkBytes.stream().map(Object::toString).collect(Collectors.joining(",")))
                        .withHeader(X_MAPS_LOCATIONS_NOT_IN_MAP, locationsNotInMap.stream().map(Object::toString)
                                .collect(Collectors.joining(",")))
                        .withStatus(200)
                        .withBody(data.readAllBytes())
                        .build();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        return TRANSFORMER_NAME;
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }

    private List<Location> parseStringToLocationList(String s) {
        try {
            List<Location> locationList = new ArrayList<>();

            double[][] locations = objectMapper.readValue(s, double[][].class);
            for (double[] location : locations) {
                locationList.add(new Location(location[0], location[1]));
            }

            return locationList;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String convertTravelTimeAndDistanceToJsonString(TravelTimeAndDistance travelTimeAndDistance) {
        try {
            return objectMapper.writeValueAsString(travelTimeAndDistance);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStreamAndChunkBytes joinResponsesIntoInputStream(TravelTimeAndDistance... travelTimeAndDistances)
            throws JsonProcessingException {
        List<InputStream> streams = new ArrayList<>();
        List<Integer> chunkBytes = new ArrayList<>();
        for (TravelTimeAndDistance travelTimeAndDistance : travelTimeAndDistances) {
            String stringValue = objectMapper.writeValueAsString(travelTimeAndDistance);
            ByteArrayInputStream stream = new ByteArrayInputStream(stringValue.getBytes());
            streams.add(stream);
            chunkBytes.add(stringValue.getBytes().length);
        }
        return new InputStreamAndChunkBytes(new SequenceInputStream(Collections.enumeration(streams)), chunkBytes);
    }

    private record InputStreamAndChunkBytes(InputStream inputStream, List<Integer> chunkBytes) {

    }
}
