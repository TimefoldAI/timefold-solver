package ai.timefold.solver.service.testmodel;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@QuarkusTestResource(InMemoryMessagingTestResource.class)
@TestProfile(ApiVersion2ConfigProfile.class)
public class EmployeeScheduleResourceV2BetaTest extends EmployeeScheduleResourceTest {

}
