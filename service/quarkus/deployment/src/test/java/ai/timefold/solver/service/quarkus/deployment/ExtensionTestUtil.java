package ai.timefold.solver.service.quarkus.deployment;

import static ai.timefold.solver.service.quarkus.deployment.DefaultConfigProfileProcessor.MODEL_CONFIG_TERMINATION_SPENT_LIMIT;
import static ai.timefold.solver.service.quarkus.deployment.TimefoldModelDescriptorProcessor.APPLICATION_VERSION_PROPERTY;
import static ai.timefold.solver.service.worker.impl.termination.TerminationConfigParams.TERMINATION_SPENT_LIMIT;

import io.quarkus.test.QuarkusExtensionTest;

public final class ExtensionTestUtil {

    private ExtensionTestUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a test deployment from the provided classes and sets the mandatory configuration keys to default values.
     * 
     * @param classes Classes to include in the test deployment
     * @return pre-configured {@link QuarkusExtensionTest} instance
     */
    public static QuarkusExtensionTest createDeploymentWithMandatoryConfig(Class<?>... classes) {
        return new QuarkusExtensionTest()
                .withApplicationRoot((jar) -> jar
                        .addClasses(ExtensionTestUtil.class)
                        .addClasses(classes))
                .overrideConfigKey(MODEL_CONFIG_TERMINATION_SPENT_LIMIT, "PT1S")
                .overrideConfigKey(TERMINATION_SPENT_LIMIT, "PT1S")
                .overrideConfigKey(APPLICATION_VERSION_PROPERTY, "v1");
    }
}
