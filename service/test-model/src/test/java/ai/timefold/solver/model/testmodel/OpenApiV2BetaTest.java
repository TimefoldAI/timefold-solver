package ai.timefold.solver.service.testmodel;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(ApiVersion2ConfigProfile.class)
public class OpenApiV2BetaTest extends OpenApiTest {

}
