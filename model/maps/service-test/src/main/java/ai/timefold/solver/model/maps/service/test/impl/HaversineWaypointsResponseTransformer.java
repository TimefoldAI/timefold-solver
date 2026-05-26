package ai.timefold.solver.model.maps.service.test.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.model.maps.api.model.Location;
import ai.timefold.solver.model.maps.haversine.impl.HaversineWaypointsProvider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

public class HaversineWaypointsResponseTransformer implements ResponseDefinitionTransformerV2 {

    public static String TRANSFORMER_NAME = "haversine-waypoints-response-transformer";
    private final HaversineWaypointsProvider provider = new HaversineWaypointsProvider();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ResponseDefinition transform(ServeEvent serveEvent) {
        List<Location> requestLocations = parseStringToLocationList(serveEvent.getRequest().getBodyAsString());

        List<Location> locations = provider.getWaypoints(requestLocations,
                Collections.emptyMap());

        return new ResponseDefinitionBuilder()
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
                .withBody(convertLocationListToJsonString(locations))
                .build();
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

    private String convertLocationListToJsonString(List<Location> locations) {
        try {
            return objectMapper.writeValueAsString(locations);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
