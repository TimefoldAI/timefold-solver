package ai.timefold.solver.service.quarkus.deployment;

import static ai.timefold.solver.service.quarkus.deployment.TimefoldModelDescriptorProcessor.ALLOW_SOLVER_SNAPSHOT_PROPERTY;
import static ai.timefold.solver.service.quarkus.deployment.TimefoldModelDescriptorProcessor.isSnapshotVersion;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.util.SolverVersionUtils;
import ai.timefold.solver.service.quarkus.deployment.testdata.defaultconfigprofile.TestdataConstraintProvider;
import ai.timefold.solver.service.quarkus.deployment.testdata.defaultconfigprofile.TestdataEntity;
import ai.timefold.solver.service.quarkus.deployment.testdata.defaultconfigprofile.TestdataModelConfigOverrides;
import ai.timefold.solver.service.quarkus.deployment.testdata.defaultconfigprofile.TestdataRest;
import ai.timefold.solver.service.quarkus.deployment.testdata.defaultconfigprofile.TestdataSolution;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Runs only with a snapshot core version.
 * The condition is evaluated before Quarkus augmentation starts.
 */
@EnabledIf("isSnapshotSolverVersion")
class SolverSnapshotValidationBuildTest {

    @RegisterExtension
    static final QuarkusExtensionTest config =
            ExtensionTestUtil.createDeploymentWithMandatoryConfig(TestdataModelConfigOverrides.class,
                    TestdataEntity.class, TestdataSolution.class, TestdataConstraintProvider.class, TestdataRest.class)
                    .overrideConfigKey(ALLOW_SOLVER_SNAPSHOT_PROPERTY, "false")
                    .assertException(throwable -> {
                        Assertions.assertThat(throwable).isInstanceOf(IllegalStateException.class);
                        Assertions.assertThat(throwable.getMessage())
                                .contains("is a snapshot version");
                    });

    static boolean isSnapshotSolverVersion() {
        return isSnapshotVersion(
                SolverVersionUtils.bareVersion(SolverVersionUtils.CORE_GIT_PROPERTIES, SolverFactory.class));
    }

    @Test
    void snapshotNotAllowedFailsBuild() {
        Assertions.fail("The test app is supposed to fail fast at build time.");
    }
}
