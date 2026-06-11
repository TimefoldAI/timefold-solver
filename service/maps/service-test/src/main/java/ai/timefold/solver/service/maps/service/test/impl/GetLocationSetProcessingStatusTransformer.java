package ai.timefold.solver.service.maps.service.test.impl;

import ai.timefold.solver.service.maps.service.integration.internal.model.LocationSetStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

public class GetLocationSetProcessingStatusTransformer implements ResponseDefinitionTransformerV2 {

    public static String TRANSFORMER_NAME = "get-location-set-processing-status-response-transformer";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ResponseDefinition transform(ServeEvent serveEvent) {
        try {
            return new ResponseDefinitionBuilder()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(objectMapper.writeValueAsString(LocationSetStatus.PROCESSING))
                    .build();
        } catch (JsonProcessingException e) {
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
}
