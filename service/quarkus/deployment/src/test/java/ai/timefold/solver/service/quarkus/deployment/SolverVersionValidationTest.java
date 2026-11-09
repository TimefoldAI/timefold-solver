package ai.timefold.solver.service.quarkus.deployment;

import static ai.timefold.solver.service.quarkus.deployment.TimefoldModelDescriptorProcessor.ALLOW_SOLVER_SNAPSHOT_PROPERTY;
import static ai.timefold.solver.service.quarkus.deployment.TimefoldModelDescriptorProcessor.MAXIMAL_SOLVER_VERSION_PROPERTY;
import static ai.timefold.solver.service.quarkus.deployment.TimefoldModelDescriptorProcessor.MINIMAL_SOLVER_VERSION_PROPERTY;
import static ai.timefold.solver.service.quarkus.deployment.TimefoldModelDescriptorProcessor.validateSolverVersion;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SolverVersionValidationTest {

    @Test
    void noLimitsAndReleaseVersion() {
        assertThatCode(() -> validateSolverVersion("1.20.0", null, null, false)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = { "1.20.0-SNAPSHOT", "999-snapshot", "1.20.0-Snapshot" })
    void snapshotNotAllowed(String solverVersion) {
        assertThatThrownBy(() -> validateSolverVersion(solverVersion, null, null, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(solverVersion)
                .hasMessageContaining("snapshot")
                .hasMessageContaining(ALLOW_SOLVER_SNAPSHOT_PROPERTY);
    }

    @Test
    void snapshotNotEnforcedWhenPropertyNotSet() {
        assertThatCode(() -> validateSolverVersion("1.20.0-SNAPSHOT", null, null, null)).doesNotThrowAnyException();
    }

    @Test
    void snapshotAllowed() {
        assertThatCode(() -> validateSolverVersion("1.20.0-SNAPSHOT", null, null, true)).doesNotThrowAnyException();
    }

    @Test
    void lowerThanMinimal() {
        assertThatThrownBy(() -> validateSolverVersion("1.19.0", "1.20.0", null, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1.19.0")
                .hasMessageContaining("1.20.0")
                .hasMessageContaining("lower")
                .hasMessageContaining(MINIMAL_SOLVER_VERSION_PROPERTY);
    }

    @Test
    void equalToMinimal() {
        assertThatCode(() -> validateSolverVersion("1.20.0", "1.20.0", null, false)).doesNotThrowAnyException();
    }

    @Test
    void higherThanMaximal() {
        assertThatThrownBy(() -> validateSolverVersion("1.21.0", null, "1.20.0", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1.21.0")
                .hasMessageContaining("1.20.0")
                .hasMessageContaining("higher")
                .hasMessageContaining(MAXIMAL_SOLVER_VERSION_PROPERTY);
    }

    @Test
    void equalToMaximal() {
        assertThatCode(() -> validateSolverVersion("1.20.0", null, "1.20.0", false)).doesNotThrowAnyException();
    }

    @Test
    void insideRange() {
        assertThatCode(() -> validateSolverVersion("1.20.5", "1.20.0", "1.21.0", false)).doesNotThrowAnyException();
    }

    @Test
    void comparesNumericallyNotLexically() {
        assertThatCode(() -> validateSolverVersion("1.100.0", "1.20.0", null, false)).doesNotThrowAnyException();
    }

    @Test
    void snapshotIsLowerThanItsRelease() {
        assertThatThrownBy(() -> validateSolverVersion("1.20.0-SNAPSHOT", "1.20.0", null, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("lower");
    }
}
