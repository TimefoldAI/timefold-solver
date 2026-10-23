package ai.timefold.solver.service.definition.impl.executionprofile.distancematrix;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class DistanceMatrixExecutionProfileTest {

    private final DistanceMatrixExecutionProfile profile = new DistanceMatrixExecutionProfile();

    @Test
    void enablesDistanceMatrixRecording() {
        assertThat(profile.toEnvironment(Map.of()))
                .containsExactly(Map.entry("TIMEFOLD_PLATFORM_MAP_SERVICE_RECORDING_ENABLED", "true"));
    }

    @Test
    void ignoresUnrelatedOptions() {
        assertThat(profile.toEnvironment(Map.of("seed", "1")))
                .isEqualTo(profile.toEnvironment(Map.of()));
    }

    @Test
    void acceptsNullOptions() {
        assertThat(profile.toEnvironment(null)).isEqualTo(profile.toEnvironment(Map.of()));
    }
}
