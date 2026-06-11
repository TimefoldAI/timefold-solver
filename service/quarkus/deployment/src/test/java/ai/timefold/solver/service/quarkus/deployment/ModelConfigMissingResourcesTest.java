package ai.timefold.solver.service.quarkus.deployment;

import static ai.timefold.solver.service.worker.impl.termination.TerminationConfigParams.TERMINATION_SPENT_LIMIT;

import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigschema.ObjectParameter;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigschema.TestdataConstraintProvider;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigschema.TestdataEntity;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigschema.TestdataModelConfigOverrides;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigschema.TestdataModelConvertor;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigschema.TestdataRest;
import ai.timefold.solver.service.quarkus.deployment.testdata.modelconfigschema.TestdataSolution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ModelConfigMissingResourcesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestdataModelConfigOverrides.class, ObjectParameter.class, TestdataEntity.class,
                            TestdataSolution.class, TestdataConstraintProvider.class, TestdataModelConvertor.class,
                            TestdataRest.class))
            .overrideConfigKey(TERMINATION_SPENT_LIMIT, "PT1S")
            .setExpectedException(IllegalStateException.class);

    @Test
    void testMissingResourcesProperties() {
        // When registering extension above, IllegalStateException should be thrown
        // This test should be empty to trigger the extension registration
    }
}
