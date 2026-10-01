package ai.timefold.solver.service.definition.api.termination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class SolverTerminationConfigTest {

    @Test
    void rejectsZeroMinimumImprovementRatio() {
        assertThatThrownBy(() -> new SolverTerminationConfig(Duration.ofMinutes(1), null, null, null, null, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimumImprovementRatio")
                .hasMessageContaining("must be strictly positive");
    }

    @Test
    void rejectsNegativeStepCountLimit() {
        assertThatThrownBy(() -> new SolverTerminationConfig(Duration.ofMinutes(1), null, -1, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stepCountLimit")
                .hasMessageContaining("cannot be negative");
    }

    @Test
    void rejectsNegativeMoveCountLimit() {
        assertThatThrownBy(() -> new SolverTerminationConfig(Duration.ofMinutes(1), null, null, -1L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moveCountLimit")
                .hasMessageContaining("cannot be negative");
    }

    @Test
    void acceptsZeroCountLimits() {
        SolverTerminationConfig config = new SolverTerminationConfig(Duration.ofMinutes(1), null, 0, 0L, null, null);

        assertThat(config.stepCountLimit()).isZero();
        assertThat(config.moveCountLimit()).isZero();
    }

    @Test
    void rejectsUnimprovedSpentLimitCombinedWithStepCountLimit() {
        assertThatThrownBy(
                () -> new SolverTerminationConfig(Duration.ofMinutes(1), Duration.ofSeconds(30), 1000, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unimprovedSpentLimit")
                .hasMessageContaining("stepCountLimit");
    }

    @Test
    void rejectsUnimprovedSpentLimitCombinedWithMoveCountLimit() {
        assertThatThrownBy(
                () -> new SolverTerminationConfig(Duration.ofMinutes(1), Duration.ofSeconds(30), null, 100_000L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unimprovedSpentLimit")
                .hasMessageContaining("moveCountLimit");
    }

    @Test
    void rejectsNegativeMinimumImprovementRatio() {
        assertThatThrownBy(() -> new SolverTerminationConfig(Duration.ofMinutes(1), null, null, null, null, -0.01))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be strictly positive");
    }

    @Test
    void acceptsNullMinimumImprovementRatio() {
        SolverTerminationConfig config = new SolverTerminationConfig(Duration.ofMinutes(1), null, null, null, null, null);

        assertThat(config.minimumImprovementRatio()).isNull();
        assertThat(config.slidingWindowDuration()).isNull();
    }

    @Test
    void acceptsPositiveMinimumImprovementRatio() {
        SolverTerminationConfig config =
                new SolverTerminationConfig(Duration.ofMinutes(1), null, null, null, Duration.ofMinutes(5), 0.01);

        assertThat(config.slidingWindowDuration()).isEqualTo(Duration.ofMinutes(5));
        assertThat(config.minimumImprovementRatio()).isEqualTo(0.01);
    }

    @Test
    void fiveArgConstructorPreservesLegacyShapeAndDefaultsMoveCountLimitToNull() {
        SolverTerminationConfig config = new SolverTerminationConfig(
                Duration.ofMinutes(1), null, 1000, Duration.ofMinutes(5), 0.01);

        assertThat(config.spentLimit()).isEqualTo(Duration.ofMinutes(1));
        assertThat(config.stepCountLimit()).isEqualTo(1000);
        assertThat(config.moveCountLimit()).isNull();
        assertThat(config.slidingWindowDuration()).isEqualTo(Duration.ofMinutes(5));
        assertThat(config.minimumImprovementRatio()).isEqualTo(0.01);
    }

    @Test
    void threeArgConstructorDefaultsDiminishedReturnsTuningToNull() {
        SolverTerminationConfig config = new SolverTerminationConfig(Duration.ofMinutes(1), null, 100);

        assertThat(config.moveCountLimit()).isNull();
        assertThat(config.slidingWindowDuration()).isNull();
        assertThat(config.minimumImprovementRatio()).isNull();
    }

    @Test
    void fourArgConstructorDefaultsDiminishedReturnsTuningToNull() {
        SolverTerminationConfig config = new SolverTerminationConfig(Duration.ofMinutes(1), null, null, 100_000L);

        assertThat(config.moveCountLimit()).isEqualTo(100_000L);
        assertThat(config.slidingWindowDuration()).isNull();
        assertThat(config.minimumImprovementRatio()).isNull();
    }

    @Test
    void acceptsMoveCountLimit() {
        SolverTerminationConfig config =
                new SolverTerminationConfig(Duration.ofMinutes(1), null, null, 100_000L, null, null);

        assertThat(config.moveCountLimit()).isEqualTo(100_000L);
    }

    @Test
    void moveCountLimitAndStepCountLimitCanCombine() {
        SolverTerminationConfig config =
                new SolverTerminationConfig(Duration.ofMinutes(1), null, 1000, 100_000L, null, null);

        assertThat(config.stepCountLimit()).isEqualTo(1000);
        assertThat(config.moveCountLimit()).isEqualTo(100_000L);
    }

    @Test
    void overrideFillsMissingDiminishedReturnsTuningFromFallback() {
        SolverTerminationConfig primary = new SolverTerminationConfig(Duration.ofMinutes(1), null, null, null, null, null);
        SolverTerminationConfig fallback =
                new SolverTerminationConfig(null, null, null, null, Duration.ofMinutes(2), 0.001);

        SolverTerminationConfig merged = primary.override(fallback);

        assertThat(merged.slidingWindowDuration()).isEqualTo(Duration.ofMinutes(2));
        assertThat(merged.minimumImprovementRatio()).isEqualTo(0.001);
    }

    @Test
    void overrideKeepsPrimaryDiminishedReturnsTuningWhenPresent() {
        SolverTerminationConfig primary =
                new SolverTerminationConfig(Duration.ofMinutes(1), null, null, null, Duration.ofMinutes(10), 0.5);
        SolverTerminationConfig fallback =
                new SolverTerminationConfig(null, null, null, null, Duration.ofMinutes(2), 0.001);

        SolverTerminationConfig merged = primary.override(fallback);

        assertThat(merged.slidingWindowDuration()).isEqualTo(Duration.ofMinutes(10));
        assertThat(merged.minimumImprovementRatio()).isEqualTo(0.5);
    }

    @Test
    void overrideFillsMissingMoveCountLimitFromFallback() {
        SolverTerminationConfig primary = new SolverTerminationConfig(Duration.ofMinutes(1), null, null, null, null, null);
        SolverTerminationConfig fallback = new SolverTerminationConfig(null, null, null, 100_000L, null, null);

        SolverTerminationConfig merged = primary.override(fallback);

        assertThat(merged.moveCountLimit()).isEqualTo(100_000L);
    }

    @Test
    void overrideKeepsPrimaryMoveCountLimitWhenPresent() {
        SolverTerminationConfig primary =
                new SolverTerminationConfig(Duration.ofMinutes(1), null, null, 500_000L, null, null);
        SolverTerminationConfig fallback = new SolverTerminationConfig(null, null, null, 100_000L, null, null);

        SolverTerminationConfig merged = primary.override(fallback);

        assertThat(merged.moveCountLimit()).isEqualTo(500_000L);
    }

    @Test
    void overrideRejectsMoveCountLimitCombinedWithUnimprovedSpentLimit() {
        SolverTerminationConfig primary =
                new SolverTerminationConfig(Duration.ofMinutes(1), null, null, 100_000L, null, null);
        SolverTerminationConfig fallback =
                new SolverTerminationConfig(null, Duration.ofSeconds(30), null, null, null, null);

        assertThatThrownBy(() -> primary.override(fallback))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unimprovedSpentLimit")
                .hasMessageContaining("moveCountLimit");
    }

    @Test
    void overrideRejectsStepCountLimitCombinedWithUnimprovedSpentLimit() {
        SolverTerminationConfig primary = new SolverTerminationConfig(Duration.ofMinutes(1), null, 1000);
        SolverTerminationConfig fallback =
                new SolverTerminationConfig(null, Duration.ofSeconds(30), null, null, null, null);

        assertThatThrownBy(() -> primary.override(fallback))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unimprovedSpentLimit")
                .hasMessageContaining("stepCountLimit");
    }

    @Test
    void nullConfigurationOverrideReturnsThis() {
        SolverTerminationConfig primary =
                new SolverTerminationConfig(Duration.ofMinutes(1), null, null, 100_000L, null, null);

        assertThat(primary.override(null)).isSameAs(primary);
    }
}
