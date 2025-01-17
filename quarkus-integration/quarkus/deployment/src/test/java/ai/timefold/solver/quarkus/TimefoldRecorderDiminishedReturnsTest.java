package ai.timefold.solver.quarkus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.quarkus.config.DiminishedReturnsRuntimeConfig;
import ai.timefold.solver.quarkus.config.SolverRuntimeConfig;
import ai.timefold.solver.quarkus.config.TerminationRuntimeConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TimefoldRecorderDiminishedReturnsTest {
    SolverConfig solverConfig;
    SolverRuntimeConfig solverRuntimeConfig;
    TerminationRuntimeConfig terminationRuntimeConfig;
    DiminishedReturnsRuntimeConfig diminishedReturnsRuntimeConfig;

    @BeforeEach
    void setUp() {
        solverConfig = new SolverConfig();
        solverRuntimeConfig = Mockito.mock(SolverRuntimeConfig.class);
        terminationRuntimeConfig = Mockito.mock(TerminationRuntimeConfig.class);
        diminishedReturnsRuntimeConfig = Mockito.mock(DiminishedReturnsRuntimeConfig.class);

        Mockito.when(solverRuntimeConfig.termination()).thenReturn(terminationRuntimeConfig);
        Mockito.when(terminationRuntimeConfig.diminishedReturns()).thenReturn(
                Optional.of(diminishedReturnsRuntimeConfig));
    }

    void assertNoDiminishedReturns(SolverConfig solverConfig) {
        assertThat(solverConfig.getTerminationConfig().getDiminishedReturnsConfig()).isNull();
        if (solverConfig.getPhaseConfigList() != null) {
            assertThat(solverConfig.getPhaseConfigList()).allMatch(
                    phaseConfig -> phaseConfig.getTerminationConfig() == null,
                    "has a null termination config");
        }
    }

    void assertDiminishedReturns(SolverConfig solverConfig,
            Duration slidingWindowDuration, Double minimumImprovementRatio) {
        assertThat(solverConfig.getTerminationConfig().getDiminishedReturnsConfig()).isNull();
        assertThat(solverConfig.getPhaseConfigList()).isNotNull();
        assertThat(solverConfig.getPhaseConfigList()).hasSize(2);
        assertThat(solverConfig.getPhaseConfigList()).element(0)
                .isInstanceOf(ConstructionHeuristicPhaseConfig.class)
                .extracting(PhaseConfig::getTerminationConfig)
                .isNull();

        assertThat(solverConfig.getPhaseConfigList()).element(1)
                .isInstanceOf(LocalSearchPhaseConfig.class)
                .extracting(PhaseConfig::getTerminationConfig)
                .isNotNull()
                .extracting(TerminationConfig::getDiminishedReturnsConfig)
                .isNotNull()
                .hasFieldOrPropertyWithValue("minimumImprovementRatio", minimumImprovementRatio)
                .hasFieldOrPropertyWithValue("slidingWindowDuration", slidingWindowDuration);
    }

    @Test
    void nothingSet() {
        TimefoldRecorder.updateSolverConfigWithRuntimeProperties(solverConfig, solverRuntimeConfig);
        assertNoDiminishedReturns(solverConfig);
    }

    @Test
    void onlyEnabledSet() {
        Mockito.when(diminishedReturnsRuntimeConfig.enabled()).thenReturn(Optional.of(true));
        TimefoldRecorder.updateSolverConfigWithRuntimeProperties(solverConfig, solverRuntimeConfig);
        assertDiminishedReturns(solverConfig, null, null);
    }

    @Test
    void onlySlidingWindowSet() {
        Mockito.when(diminishedReturnsRuntimeConfig.slidingWindowDuration()).thenReturn(
                Optional.ofNullable(Duration.ofMinutes(30)));
        TimefoldRecorder.updateSolverConfigWithRuntimeProperties(solverConfig, solverRuntimeConfig);
        assertDiminishedReturns(solverConfig, Duration.ofMinutes(30), null);
    }

    @Test
    void onlyMinimumImprovementRatioSet() {
        Mockito.when(diminishedReturnsRuntimeConfig.minimumImprovementRatio()).thenReturn(
                OptionalDouble.of(123.0));
        TimefoldRecorder.updateSolverConfigWithRuntimeProperties(solverConfig, solverRuntimeConfig);
        assertDiminishedReturns(solverConfig, null, 123.0);
    }

    @Test
    void minimumImprovementRatioAndSlidingWindowSet() {
        Mockito.when(diminishedReturnsRuntimeConfig.slidingWindowDuration()).thenReturn(
                Optional.ofNullable(Duration.ofMinutes(30)));
        Mockito.when(diminishedReturnsRuntimeConfig.minimumImprovementRatio()).thenReturn(
                OptionalDouble.of(123.0));
        TimefoldRecorder.updateSolverConfigWithRuntimeProperties(solverConfig, solverRuntimeConfig);
        assertDiminishedReturns(solverConfig, Duration.ofMinutes(30), 123.0);
    }

    @Test
    void disabledAndMinimumImprovementRatioAndSlidingWindowSet() {
        Mockito.when(diminishedReturnsRuntimeConfig.enabled()).thenReturn(Optional.of(false));
        Mockito.when(diminishedReturnsRuntimeConfig.slidingWindowDuration()).thenReturn(
                Optional.ofNullable(Duration.ofMinutes(30)));
        Mockito.when(diminishedReturnsRuntimeConfig.minimumImprovementRatio()).thenReturn(
                OptionalDouble.of(123.0));
        TimefoldRecorder.updateSolverConfigWithRuntimeProperties(solverConfig, solverRuntimeConfig);
        assertNoDiminishedReturns(solverConfig);
    }

    @Test
    void enabledAndPhasesConfigured() {
        solverConfig.setPhaseConfigList(List.of(
                new ConstructionHeuristicPhaseConfig(),
                new LocalSearchPhaseConfig()));
        Mockito.when(diminishedReturnsRuntimeConfig.enabled()).thenReturn(Optional.of(true));

        assertThatCode(() -> TimefoldRecorder.updateSolverConfigWithRuntimeProperties(solverConfig, solverRuntimeConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quarkus.timefold.solver.termination.diminished-returns")
                .hasMessageContaining("properties cannot be used when phases are configured");
    }

    @Test
    void disabledAndPhasesConfigured() {
        solverConfig.setPhaseConfigList(List.of(
                new ConstructionHeuristicPhaseConfig(),
                new LocalSearchPhaseConfig()));
        Mockito.when(diminishedReturnsRuntimeConfig.enabled()).thenReturn(Optional.of(false));

        TimefoldRecorder.updateSolverConfigWithRuntimeProperties(solverConfig, solverRuntimeConfig);
        assertNoDiminishedReturns(solverConfig);
    }
}
