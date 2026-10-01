package ai.timefold.solver.service.worker.impl.termination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Optional;

import ai.timefold.solver.core.config.solver.termination.TerminationCompositionStyle;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.service.definition.api.termination.SolverTerminationConfig;
import ai.timefold.solver.service.definition.internal.error.TimefoldRuntimeException;

import org.junit.jupiter.api.Test;

class TerminationServiceTest {

    private static TerminationService service() {
        return new TerminationService("PT10S", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Test
    void nullInputUsesPlatformSpentLimitAndDiminishedReturnsDefaults() {
        TerminationConfig resolved = service().resolveTerminationConfig(null);

        assertThat(resolved.getTerminationCompositionStyle()).isEqualTo(TerminationCompositionStyle.OR);
        assertThat(resolved.getSpentLimit()).isEqualTo(Duration.ofSeconds(10));
        assertThat(resolved.getUnimprovedSpentLimit()).isNull();
        assertThat(resolved.getStepCountLimit()).isNull();
        assertThat(resolved.getMoveCountLimit()).isNull();
        assertThat(resolved.getDiminishedReturnsConfig()).isNotNull();
        // No platform-level tuning anymore: solver-core defaults apply (both null on the config).
        assertThat(resolved.getDiminishedReturnsConfig().getSlidingWindowDuration()).isNull();
        assertThat(resolved.getDiminishedReturnsConfig().getMinimumImprovementRatio()).isNull();
    }

    @Test
    void perRequestDiminishedReturnsTuningIsForwarded() {
        SolverTerminationConfig input = new SolverTerminationConfig(
                Duration.ofMinutes(1), null, null, null, Duration.ofMinutes(5), 0.01);

        TerminationConfig resolved = service().resolveTerminationConfig(input);

        assertThat(resolved.getSpentLimit()).isEqualTo(Duration.ofMinutes(1));
        assertThat(resolved.getUnimprovedSpentLimit()).isNull();
        assertThat(resolved.getStepCountLimit()).isNull();
        assertThat(resolved.getMoveCountLimit()).isNull();
        assertThat(resolved.getDiminishedReturnsConfig()).isNotNull();
        assertThat(resolved.getDiminishedReturnsConfig().getSlidingWindowDuration())
                .isEqualTo(Duration.ofMinutes(5));
        assertThat(resolved.getDiminishedReturnsConfig().getMinimumImprovementRatio()).isEqualTo(0.01);
    }

    @Test
    void unimprovedSpentLimitDisablesDiminishedReturns() {
        SolverTerminationConfig input = new SolverTerminationConfig(
                Duration.ofMinutes(1), Duration.ofSeconds(30), null, null, Duration.ofMinutes(5), 0.01);

        TerminationConfig resolved = service().resolveTerminationConfig(input);

        assertThat(resolved.getUnimprovedSpentLimit()).isEqualTo(Duration.ofSeconds(30));
        assertThat(resolved.getStepCountLimit()).isNull();
        assertThat(resolved.getMoveCountLimit()).isNull();
        // diminished-returns tuning on the request is ignored when unimprovedSpentLimit is set.
        assertThat(resolved.getDiminishedReturnsConfig()).isNull();
    }

    @Test
    void stepCountLimitDisablesDiminishedReturns() {
        SolverTerminationConfig input = new SolverTerminationConfig(
                Duration.ofMinutes(1), null, 1000, null, Duration.ofMinutes(5), 0.01);

        TerminationConfig resolved = service().resolveTerminationConfig(input);

        assertThat(resolved.getStepCountLimit()).isEqualTo(1000);
        assertThat(resolved.getMoveCountLimit()).isNull();
        assertThat(resolved.getUnimprovedSpentLimit()).isNull();
        assertThat(resolved.getDiminishedReturnsConfig()).isNull();
    }

    @Test
    void moveCountLimitDisablesDiminishedReturns() {
        SolverTerminationConfig input = new SolverTerminationConfig(
                Duration.ofMinutes(1), null, null, 100_000L, Duration.ofMinutes(5), 0.01);

        TerminationConfig resolved = service().resolveTerminationConfig(input);

        assertThat(resolved.getMoveCountLimit()).isEqualTo(100_000L);
        assertThat(resolved.getStepCountLimit()).isNull();
        assertThat(resolved.getUnimprovedSpentLimit()).isNull();
        // diminished-returns tuning on the request is ignored when moveCountLimit is set.
        assertThat(resolved.getDiminishedReturnsConfig()).isNull();
    }

    @Test
    void stepCountLimitAndMoveCountLimitCombine() {
        SolverTerminationConfig input = new SolverTerminationConfig(
                Duration.ofMinutes(1), null, 1000, 100_000L, null, null);

        TerminationConfig resolved = service().resolveTerminationConfig(input);

        // Both hard limits are OR-composed: whichever is reached first terminates the solver.
        assertThat(resolved.getStepCountLimit()).isEqualTo(1000);
        assertThat(resolved.getMoveCountLimit()).isEqualTo(100_000L);
        assertThat(resolved.getUnimprovedSpentLimit()).isNull();
        assertThat(resolved.getDiminishedReturnsConfig()).isNull();
    }

    @Test
    void nullSpentLimitOnRequestFallsBackToPlatformSpentLimit() {
        SolverTerminationConfig input = new SolverTerminationConfig(null, null, null, null, null, null);

        TerminationConfig resolved = service().resolveTerminationConfig(input);

        assertThat(resolved.getSpentLimit()).isEqualTo(Duration.ofSeconds(10));
        assertThat(resolved.getDiminishedReturnsConfig()).isNotNull();
    }

    @Test
    void platformUnimprovedSpentLimitDisablesDiminishedReturnsWhenNoRequest() {
        TerminationService service =
                new TerminationService("PT10S", Optional.of("PT5S"), Optional.empty(), Optional.empty(), Optional.empty());

        TerminationConfig resolved = service.resolveTerminationConfig(null);

        assertThat(resolved.getUnimprovedSpentLimit()).isEqualTo(Duration.ofSeconds(5));
        assertThat(resolved.getDiminishedReturnsConfig()).isNull();
    }

    @Test
    void platformStepCountLimitDisablesDiminishedReturnsWhenNoRequest() {
        TerminationService service =
                new TerminationService("PT10S", Optional.empty(), Optional.empty(), Optional.of(50), Optional.empty());

        TerminationConfig resolved = service.resolveTerminationConfig(null);

        assertThat(resolved.getStepCountLimit()).isEqualTo(50);
        assertThat(resolved.getDiminishedReturnsConfig()).isNull();
    }

    @Test
    void platformMoveCountLimitDisablesDiminishedReturnsWhenNoRequest() {
        TerminationService service =
                new TerminationService("PT10S", Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(500_000L));

        TerminationConfig resolved = service.resolveTerminationConfig(null);

        assertThat(resolved.getMoveCountLimit()).isEqualTo(500_000L);
        assertThat(resolved.getDiminishedReturnsConfig()).isNull();
    }

    @Test
    void rejectsNegativePlatformStepCountLimit() {
        assertThatThrownBy(() -> new TerminationService("PT10S", Optional.empty(), Optional.empty(), Optional.of(-1),
                Optional.empty()))
                .isInstanceOf(TimefoldRuntimeException.class)
                .hasMessageContaining(TerminationConfigParams.TERMINATION_STEP_COUNT_LIMIT)
                .hasMessageContaining("cannot be negative");
    }

    @Test
    void rejectsNegativePlatformMoveCountLimit() {
        assertThatThrownBy(() -> new TerminationService("PT10S", Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(-1L)))
                .isInstanceOf(TimefoldRuntimeException.class)
                .hasMessageContaining(TerminationConfigParams.TERMINATION_MOVE_COUNT_LIMIT)
                .hasMessageContaining("cannot be negative");
    }

    @Test
    void acceptsZeroPlatformCountLimits() {
        TerminationService service =
                new TerminationService("PT10S", Optional.empty(), Optional.empty(), Optional.of(0), Optional.of(0L));

        TerminationConfig resolved = service.resolveTerminationConfig(null);

        assertThat(resolved.getStepCountLimit()).isZero();
        assertThat(resolved.getMoveCountLimit()).isZero();
        assertThat(resolved.getDiminishedReturnsConfig()).isNull();
    }

    @Test
    void rejectsPlatformUnimprovedSpentLimitCombinedWithStepCountLimit() {
        assertThatThrownBy(() -> new TerminationService("PT10S", Optional.of("PT5S"), Optional.empty(), Optional.of(1000),
                Optional.empty()))
                .isInstanceOf(TimefoldRuntimeException.class)
                .hasMessageContaining(TerminationConfigParams.TERMINATION_UNIMPROVED_SPENT_LIMIT)
                .hasMessageContaining(TerminationConfigParams.TERMINATION_STEP_COUNT_LIMIT);
    }

    @Test
    void rejectsPlatformUnimprovedSpentLimitCombinedWithMoveCountLimit() {
        assertThatThrownBy(() -> new TerminationService("PT10S", Optional.of("PT5S"), Optional.empty(), Optional.empty(),
                Optional.of(100_000L)))
                .isInstanceOf(TimefoldRuntimeException.class)
                .hasMessageContaining(TerminationConfigParams.TERMINATION_UNIMPROVED_SPENT_LIMIT)
                .hasMessageContaining(TerminationConfigParams.TERMINATION_MOVE_COUNT_LIMIT);
    }

    @Test
    void perRequestStepCountLimitIgnoresPlatformUnimprovedSpentLimit() {
        TerminationService service =
                new TerminationService("PT10S", Optional.of("PT5S"), Optional.empty(), Optional.empty(), Optional.empty());
        SolverTerminationConfig input = new SolverTerminationConfig(null, null, 1000);

        TerminationConfig resolved = service.resolveTerminationConfig(input);

        assertThat(resolved.getStepCountLimit()).isEqualTo(1000);
        assertThat(resolved.getUnimprovedSpentLimit()).isNull();
    }

    @Test
    void perRequestMoveCountLimitIgnoresPlatformUnimprovedSpentLimit() {
        TerminationService service =
                new TerminationService("PT10S", Optional.of("PT5S"), Optional.empty(), Optional.empty(), Optional.empty());
        SolverTerminationConfig input = new SolverTerminationConfig(null, null, null, 100_000L);

        TerminationConfig resolved = service.resolveTerminationConfig(input);

        assertThat(resolved.getMoveCountLimit()).isEqualTo(100_000L);
        assertThat(resolved.getUnimprovedSpentLimit()).isNull();
    }

    @Test
    void perRequestUnimprovedSpentLimitIgnoresPlatformStepCountAndMoveCountLimits() {
        TerminationService service =
                new TerminationService("PT10S", Optional.empty(), Optional.empty(), Optional.of(50), Optional.of(500_000L));
        SolverTerminationConfig input = new SolverTerminationConfig(null, Duration.ofSeconds(30));

        TerminationConfig resolved = service.resolveTerminationConfig(input);

        assertThat(resolved.getUnimprovedSpentLimit()).isEqualTo(Duration.ofSeconds(30));
        assertThat(resolved.getStepCountLimit()).isNull();
        assertThat(resolved.getMoveCountLimit()).isNull();
    }

    @Test
    void perRequestStepCountLimitStillInheritsPlatformMoveCountLimit() {
        TerminationService service =
                new TerminationService("PT10S", Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(500_000L));
        SolverTerminationConfig input = new SolverTerminationConfig(null, null, 1000);

        TerminationConfig resolved = service.resolveTerminationConfig(input);

        assertThat(resolved.getStepCountLimit()).isEqualTo(1000);
        assertThat(resolved.getMoveCountLimit()).isEqualTo(500_000L);
    }

    @Test
    void perRequestMoveCountLimitOverridesPlatformDefault() {
        TerminationService service =
                new TerminationService("PT10S", Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(500_000L));
        SolverTerminationConfig input = new SolverTerminationConfig(null, null, null, 100_000L);

        TerminationConfig resolved = service.resolveTerminationConfig(input);

        assertThat(resolved.getMoveCountLimit()).isEqualTo(100_000L);
    }
}
