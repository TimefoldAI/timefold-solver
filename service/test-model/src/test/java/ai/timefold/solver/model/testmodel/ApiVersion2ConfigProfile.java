package ai.timefold.solver.service.testmodel;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Config profile that defines different version of the model api
 * to test out build of multiple versions of the model by config change only
 */
public class ApiVersion2ConfigProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("model.api.version", "v2-beta");
    }

}
