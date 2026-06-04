package ai.timefold.solver.model.definition.api.termination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class SolverTerminationConfigTest {

    @Test
    void rejectsZeroMinimumImprovementRatio() {
        assertThatThrownBy(() -> new SolverTerminationConfig(Duration.ofMinutes(1), null, null, null, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimumImprovementRatio")
                .hasMessageContaining("must be strictly positive");
    }

    @Test
    void rejectsNegativeMinimumImprovementRatio() {
        assertThatThrownBy(() -> new SolverTerminationConfig(Duration.ofMinutes(1), null, null, null, -0.01))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be strictly positive");
    }

    @Test
    void acceptsNullMinimumImprovementRatio() {
        SolverTerminationConfig config = new SolverTerminationConfig(Duration.ofMinutes(1), null, null, null, null);

        assertThat(config.minimumImprovementRatio()).isNull();
        assertThat(config.slidingWindowDuration()).isNull();
    }

    @Test
    void acceptsPositiveMinimumImprovementRatio() {
        SolverTerminationConfig config =
                new SolverTerminationConfig(Duration.ofMinutes(1), null, null, Duration.ofMinutes(5), 0.01);

        assertThat(config.slidingWindowDuration()).isEqualTo(Duration.ofMinutes(5));
        assertThat(config.minimumImprovementRatio()).isEqualTo(0.01);
    }

    @Test
    void threeArgConstructorDefaultsDiminishedReturnsTuningToNull() {
        SolverTerminationConfig config = new SolverTerminationConfig(Duration.ofMinutes(1), null, 100);

        assertThat(config.slidingWindowDuration()).isNull();
        assertThat(config.minimumImprovementRatio()).isNull();
    }

    @Test
    void overrideFillsMissingDiminishedReturnsTuningFromFallback() {
        SolverTerminationConfig primary = new SolverTerminationConfig(Duration.ofMinutes(1), null, null, null, null);
        SolverTerminationConfig fallback =
                new SolverTerminationConfig(null, null, null, Duration.ofMinutes(2), 0.001);

        SolverTerminationConfig merged = primary.override(fallback);

        assertThat(merged.slidingWindowDuration()).isEqualTo(Duration.ofMinutes(2));
        assertThat(merged.minimumImprovementRatio()).isEqualTo(0.001);
    }

    @Test
    void overrideKeepsPrimaryDiminishedReturnsTuningWhenPresent() {
        SolverTerminationConfig primary =
                new SolverTerminationConfig(Duration.ofMinutes(1), null, null, Duration.ofMinutes(10), 0.5);
        SolverTerminationConfig fallback =
                new SolverTerminationConfig(null, null, null, Duration.ofMinutes(2), 0.001);

        SolverTerminationConfig merged = primary.override(fallback);

        assertThat(merged.slidingWindowDuration()).isEqualTo(Duration.ofMinutes(10));
        assertThat(merged.minimumImprovementRatio()).isEqualTo(0.5);
    }
}
