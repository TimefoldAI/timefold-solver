package ai.timefold.solver.service.definition.impl.executionprofile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ThreadDumpOnOomExecutionProfileTest {

    private final ThreadDumpOnOomExecutionProfile profile = new ThreadDumpOnOomExecutionProfile();

    @Test
    void enablesMemoryWatchdog() {
        assertThat(profile.toEnvironment(Map.of()))
                .containsExactly(Map.entry("AI_TIMEFOLD_SOLVER_MONITORING_MEMORY_ENABLED", "true"));
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
