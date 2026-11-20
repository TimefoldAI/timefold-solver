package ai.timefold.solver.service.definition.impl.executionprofile.performanceprofiling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class PerformanceProfilingExecutionProfileTest {

    private final PerformanceProfilingExecutionProfile profile = new PerformanceProfilingExecutionProfile();

    @Test
    void enablesFlightRecorder() {
        assertThat(profile.toEnvironment(Map.of()))
                .containsExactly(Map.entry("TIMEFOLD_PLATFORM_MONITORING_JFR_ENABLED", "true"));
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
