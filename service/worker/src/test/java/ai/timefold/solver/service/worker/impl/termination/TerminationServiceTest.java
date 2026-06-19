package ai.timefold.solver.service.worker.impl.termination;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Optional;

import ai.timefold.solver.core.config.solver.termination.TerminationCompositionStyle;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.service.definition.api.termination.SolverTerminationConfig;

import org.junit.jupiter.api.Test;

class TerminationServiceTest {

    private static TerminationService service() {
        return new TerminationService("PT10S", Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Test
    void nullInputUsesPlatformSpentLimitAndDiminishedReturnsDefaults() {
        TerminationConfig resolved = service().resolveTerminationConfig(null);

        assertThat(resolved.getTerminationCompositionStyle()).isEqualTo(TerminationCompositionStyle.OR);
        assertThat(resolved.getSpentLimit()).isEqualTo(Duration.ofSeconds(10));
        assertThat(resolved.getUnimprovedSpentLimit()).isNull();
        assertThat(resolved.getStepCountLimit()).isNull();
        assertThat(resolved.getDiminishedReturnsConfig()).isNotNull();
        // No platform-level tuning anymore: solver-core defaults apply (both null on the config).
        assertThat(resolved.getDiminishedReturnsConfig().getSlidingWindowDuration()).isNull();
        assertThat(resolved.getDiminishedReturnsConfig().getMinimumImprovementRatio()).isNull();
    }

    @Test
    void perRequestDiminishedReturnsTuningIsForwarded() {
        SolverTerminationConfig input = new SolverTerminationConfig(
                Duration.ofMinutes(1), null, null, Duration.ofMinutes(5), 0.01);

        TerminationConfig resolved = service().resolveTerminationConfig(input);

        assertThat(resolved.getSpentLimit()).isEqualTo(Duration.ofMinutes(1));
        assertThat(resolved.getUnimprovedSpentLimit()).isNull();
        assertThat(resolved.getStepCountLimit()).isNull();
        assertThat(resolved.getDiminishedReturnsConfig()).isNotNull();
        assertThat(resolved.getDiminishedReturnsConfig().getSlidingWindowDuration())
                .isEqualTo(Duration.ofMinutes(5));
        assertThat(resolved.getDiminishedReturnsConfig().getMinimumImprovementRatio()).isEqualTo(0.01);
    }

    @Test
    void unimprovedSpentLimitDisablesDiminishedReturns() {
        SolverTerminationConfig input = new SolverTerminationConfig(
                Duration.ofMinutes(1), Duration.ofSeconds(30), null, Duration.ofMinutes(5), 0.01);

        TerminationConfig resolved = service().resolveTerminationConfig(input);

        assertThat(resolved.getUnimprovedSpentLimit()).isEqualTo(Duration.ofSeconds(30));
        assertThat(resolved.getStepCountLimit()).isNull();
        // diminished-returns tuning on the request is ignored when unimprovedSpentLimit is set.
        assertThat(resolved.getDiminishedReturnsConfig()).isNull();
    }

    @Test
    void stepCountLimitDisablesDiminishedReturns() {
        SolverTerminationConfig input = new SolverTerminationConfig(
                Duration.ofMinutes(1), null, 1000, Duration.ofMinutes(5), 0.01);

        TerminationConfig resolved = service().resolveTerminationConfig(input);

        assertThat(resolved.getStepCountLimit()).isEqualTo(1000);
        assertThat(resolved.getUnimprovedSpentLimit()).isNull();
        assertThat(resolved.getDiminishedReturnsConfig()).isNull();
    }

    @Test
    void nullSpentLimitOnRequestFallsBackToPlatformSpentLimit() {
        SolverTerminationConfig input = new SolverTerminationConfig(null, null, null, null, null);

        TerminationConfig resolved = service().resolveTerminationConfig(input);

        assertThat(resolved.getSpentLimit()).isEqualTo(Duration.ofSeconds(10));
        assertThat(resolved.getDiminishedReturnsConfig()).isNotNull();
    }

    @Test
    void platformUnimprovedSpentLimitDisablesDiminishedReturnsWhenNoRequest() {
        TerminationService service = new TerminationService("PT10S", Optional.of("PT5S"), Optional.empty(), Optional.empty());

        TerminationConfig resolved = service.resolveTerminationConfig(null);

        assertThat(resolved.getUnimprovedSpentLimit()).isEqualTo(Duration.ofSeconds(5));
        assertThat(resolved.getDiminishedReturnsConfig()).isNull();
    }

    @Test
    void platformStepCountLimitDisablesDiminishedReturnsWhenNoRequest() {
        TerminationService service = new TerminationService("PT10S", Optional.empty(), Optional.empty(), Optional.of(50));

        TerminationConfig resolved = service.resolveTerminationConfig(null);

        assertThat(resolved.getStepCountLimit()).isEqualTo(50);
        assertThat(resolved.getDiminishedReturnsConfig()).isNull();
    }
}
