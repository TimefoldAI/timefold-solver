package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class RuinRecreateConstructionHeuristicPhaseBuilderTest {

    @Test
    void buildSingleThreaded() {
        var solverConfigPolicy = new HeuristicConfigPolicy.Builder<TestdataSolution>()
                .withEnvironmentMode(EnvironmentMode.PHASE_ASSERT)
                .withSolutionDescriptor(TestdataSolution.buildSolutionDescriptor())
                .withInitializingScoreTrend(new InitializingScoreTrend(new InitializingScoreTrendLevel[] {
                        InitializingScoreTrendLevel.ANY, InitializingScoreTrendLevel.ANY, InitializingScoreTrendLevel.ANY }))
                .build();
        var constructionHeuristicConfig = mock(ConstructionHeuristicPhaseConfig.class);
        var builder = RuinRecreateConstructionHeuristicPhaseBuilder.create(solverConfigPolicy, constructionHeuristicConfig);
        var phase = builder.build();
        assertThat(phase.getEntityPlacer()).isSameAs(builder.getEntityPlacer());
    }

    @Test
    void nestedPhaseRunsInTheEnclosingPhaseEnvironmentMode() {
        // A ruin & recreate move selector is built from its enclosing phase's config policy, not the solver's,
        // so this policy stands for a local search phase which overrode the solver's environment mode.
        var phaseConfigPolicy = new HeuristicConfigPolicy.Builder<TestdataSolution>()
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withSolutionDescriptor(TestdataSolution.buildSolutionDescriptor())
                .withInitializingScoreTrend(new InitializingScoreTrend(new InitializingScoreTrendLevel[] {
                        InitializingScoreTrendLevel.ANY, InitializingScoreTrendLevel.ANY, InitializingScoreTrendLevel.ANY }))
                .build();
        var constructionHeuristicConfig = mock(ConstructionHeuristicPhaseConfig.class);
        var builder = RuinRecreateConstructionHeuristicPhaseBuilder.create(phaseConfigPolicy, constructionHeuristicConfig);
        // The nested construction heuristic is dragged along into the enclosing phase's mode.
        assertThat(builder.build().getEnvironmentMode()).isEqualTo(EnvironmentMode.FULL_ASSERT);
    }

    @Test
    void buildMultiThreaded() {
        var solverConfigPolicy = new HeuristicConfigPolicy.Builder<TestdataSolution>()
                .withEnvironmentMode(EnvironmentMode.PHASE_ASSERT)
                .withSolutionDescriptor(TestdataSolution.buildSolutionDescriptor())
                .withMoveThreadCount(2)
                .withInitializingScoreTrend(new InitializingScoreTrend(new InitializingScoreTrendLevel[] {
                        InitializingScoreTrendLevel.ANY, InitializingScoreTrendLevel.ANY, InitializingScoreTrendLevel.ANY }))
                .build();
        var constructionHeuristicConfig = mock(ConstructionHeuristicPhaseConfig.class);
        var builder = RuinRecreateConstructionHeuristicPhaseBuilder.create(solverConfigPolicy, constructionHeuristicConfig);
        var scoreDirector = mock(InnerScoreDirector.class);
        when(scoreDirector.isDerived()).thenReturn(true);
        var phase = builder
                .ensureThreadSafe(scoreDirector)
                .build();
        assertThat(phase.getEntityPlacer()).isNotSameAs(builder.getEntityPlacer());
    }
}
