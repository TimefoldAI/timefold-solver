package ai.timefold.solver.model.maps.service.test.impl;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

public class DistanceGetUpdateResponseTransformer implements ResponseDefinitionTransformerV2 {

    public static String TRANSFORMER_NAME = "distance-get-update-response-transformer";

    @Override
    public ResponseDefinition transform(ServeEvent serveEvent) {
        return new ResponseDefinitionBuilder()
                .withStatus(410)
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
}
