package ai.timefold.solver.model.maps.service.client.util;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class RemoteMapServiceConfigurationProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "ai.timefold.platform.map-service.use-remote", "true",
                "quarkus.rest-client.map-service.url", "${ai.timefold.platform.map-service.url}");
    }

}
