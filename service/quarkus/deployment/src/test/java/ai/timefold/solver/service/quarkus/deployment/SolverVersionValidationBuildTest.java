package ai.timefold.solver.service.quarkus.deployment;

import static ai.timefold.solver.service.quarkus.deployment.TimefoldModelDescriptorProcessor.MAXIMAL_SOLVER_VERSION_PROPERTY;

import ai.timefold.solver.service.quarkus.deployment.testdata.defaultconfigprofile.TestdataConstraintProvider;
import ai.timefold.solver.service.quarkus.deployment.testdata.defaultconfigprofile.TestdataEntity;
import ai.timefold.solver.service.quarkus.deployment.testdata.defaultconfigprofile.TestdataModelConfigOverrides;
import ai.timefold.solver.service.quarkus.deployment.testdata.defaultconfigprofile.TestdataRest;
import ai.timefold.solver.service.quarkus.deployment.testdata.defaultconfigprofile.TestdataSolution;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class SolverVersionValidationBuildTest {

    @RegisterExtension
    static final QuarkusExtensionTest config =
            ExtensionTestUtil.createDeploymentWithMandatoryConfig(TestdataModelConfigOverrides.class,
                    TestdataEntity.class, TestdataSolution.class, TestdataConstraintProvider.class, TestdataRest.class)
                    .overrideConfigKey(MAXIMAL_SOLVER_VERSION_PROPERTY, "0.0.1")
                    .assertException(throwable -> {
                        Assertions.assertThat(throwable).isInstanceOf(IllegalStateException.class);
                        Assertions.assertThat(throwable.getMessage())
                                .contains("higher than the maximal solver version (0.0.1)");
                    });

    @Test
    void solverVersionAboveMaximalFailsBuild() {
        Assertions.fail("The test app is supposed to fail fast at build time.");
    }
}
