package ai.timefold.solver.service.maps.service.test.impl;

import java.util.Map;

import ai.timefold.solver.service.maps.service.integration.internal.MapServiceOptions;
import ai.timefold.solver.service.maps.service.integration.internal.model.LocationSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

public class SaveLocationSetResponseTransformer implements ResponseDefinitionTransformerV2 {

    public static String TRANSFORMER_NAME = "save-location-set-response-transformer";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ResponseDefinition transform(ServeEvent serveEvent) {
        LocationSet locationSet = readLocationSetFromString(serveEvent.getRequest().getBodyAsString());
        Map<String, String> options = MapServiceOptions.parse(locationSet.options());
        String provider = options.getOrDefault(MapServiceOptions.PROVIDER, null);

        if (provider != null && provider.equals("error")) {
            return new ResponseDefinitionBuilder()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(400)
                    .build();
        }

        return new ResponseDefinitionBuilder()
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
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

    private LocationSet readLocationSetFromString(String body) {
        try {
            return objectMapper.readValue(body, LocationSet.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
