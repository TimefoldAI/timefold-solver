package ai.timefold.solver.core.impl.constructionheuristic.placer.entity;

import static ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner.DECREASING_DIFFICULTY_IF_AVAILABLE;
import static ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner.INCREASING_STRENGTH_IF_AVAILABLE;
import static ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel.ANY;
import static ai.timefold.solver.core.config.solver.EnvironmentMode.PHASE_ASSERT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedEntityPlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedValuePlacerConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacerFactory;
import ai.timefold.solver.core.impl.constructionheuristic.placer.internal.QueuedMultiplePlacerConfig;
import ai.timefold.solver.core.impl.domain.variable.listener.support.VariableListenerSupport;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.util.MutableInt;
import ai.timefold.solver.core.testdomain.multivar.list.TestdataListMultiVarSolution;

import org.junit.jupiter.api.Test;

class QueuedMultiplePlacerFactoryTest {

    @Test
    void testPlacersForConstructionHeuristic() {
        var solutionDescriptor = TestdataListMultiVarSolution.buildSolutionDescriptor();
        var configPolicy = new HeuristicConfigPolicy.Builder<TestdataListMultiVarSolution>()
                .withEnvironmentMode(PHASE_ASSERT)
                .withInitializingScoreTrend(new InitializingScoreTrend(new InitializingScoreTrendLevel[] { ANY }))
                .withSolutionDescriptor(solutionDescriptor)
                .withEntitySorterManner(DECREASING_DIFFICULTY_IF_AVAILABLE)
                .withValueSorterManner(INCREASING_STRENGTH_IF_AVAILABLE)
                .withReinitializeVariableFilterEnabled(true)
                .withInitializedChainedValueFilterEnabled(true)
                .withUnassignedValuesAllowed(true)
                .withRandom(new Random(0))
                .build();
        var valueSelectorConfig = new ValueSelectorConfig("valueList")
                .withId("valueList");
        var mimicReplayingValueSelectorConfig = new ValueSelectorConfig()
                .withMimicSelectorRef("valueList")
                .withVariableName("valueList");
        var valuePlacerConfig = new QueuedValuePlacerConfig()
                .withValueSelectorConfig(valueSelectorConfig)
                .withMoveSelectorConfig(new ListChangeMoveSelectorConfig()
                        .withValueSelectorConfig(mimicReplayingValueSelectorConfig));
        var entityPlacerConfig = new QueuedEntityPlacerConfig();
        var placerConfig = new QueuedMultiplePlacerConfig()
                .withPlacerConfigList(List.of(valuePlacerConfig, entityPlacerConfig));
        var placer = EntityPlacerFactory.<TestdataListMultiVarSolution> create(placerConfig).buildEntityPlacer(configPolicy);

        var problem = TestdataListMultiVarSolution.generateUninitializedSolution(2, 2, 2);
        var solverScope = mock(SolverScope.class);
        var scoreDirector = mock(InnerScoreDirector.class);
        var random = new Random(0L);
        when(solverScope.getScoreDirector()).thenReturn(scoreDirector);
        when(solverScope.getWorkingRandom()).thenReturn(random);
        when(scoreDirector.getWorkingSolution()).thenReturn(problem);
        when(scoreDirector.getWorkingSolution()).thenReturn(problem);
        when(scoreDirector.getSolutionDescriptor()).thenReturn(solutionDescriptor);

        var supplyManager = VariableListenerSupport.create(scoreDirector);
        when(scoreDirector.getSupplyManager()).thenReturn(supplyManager);
        supplyManager.linkVariableListeners();
        supplyManager.resetWorkingSolution();

        placer.solvingStarted(solverScope);
        var phaseScope = mock(AbstractPhaseScope.class);
        when(phaseScope.getScoreDirector()).thenReturn(scoreDirector);
        placer.phaseStarted(phaseScope);

        var placerIterator = placer.iterator();

        // Step 1
        //   Generated Value 0 -> Entity 0[0] - Entity 0 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        //   Generated Value 0 -> Entity 0[0] - Entity 0 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        //   Generated Value 0 -> Entity 0[0] - Entity 0 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        //   Generated Value 0 -> Entity 0[0] - Entity 0 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        //   Generated Value 0 -> Entity 1[0] - Entity 0 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        //   Generated Value 0 -> Entity 1[0] - Entity 0 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        //   Generated Value 0 -> Entity 1[0] - Entity 0 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        //   Generated Value 0 -> Entity 1[0] - Entity 0 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        assertThat(placerIterator.hasNext()).isTrue();
        var counter = new MutableInt();
        placerIterator.next().iterator().forEachRemaining(move -> counter.increment());
        assertThat(counter.intValue()).isEqualTo(8);

        // Accept the move - Generated Value 0 -> Entity 0[0] - Entity 0 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        problem.getEntityList().get(0).setValueList(List.of(problem.getValueList().get(0)));
        problem.getEntityList().get(0).setBasicValue(problem.getOtherValueList().get(0));
        problem.getEntityList().get(0).setSecondBasicValue(problem.getOtherValueList().get(0));
        // Update all variables
        supplyManager.resetWorkingSolution();
        var stepScope = mock(AbstractStepScope.class);
        placer.stepEnded(stepScope);

        // Step 2
        //   Generated Value 1 -> Entity 0[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        //   Generated Value 1 -> Entity 0[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        //   Generated Value 1 -> Entity 0[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        //   Generated Value 1 -> Entity 0[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        //   Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        //   Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        //   Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        //   Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        //   Generated Value 1 -> Entity 1[1] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        //   Generated Value 1 -> Entity 1[1] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        //   Generated Value 1 -> Entity 1[1] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        //   Generated Value 1 -> Entity 1[1] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        assertThat(placerIterator.hasNext()).isTrue();
        placerIterator.next().iterator().forEachRemaining(move -> counter.increment());
        assertThat(counter.intValue()).isEqualTo(20);

        // Accept the move - Generated Value 1 -> Entity 0[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValu
        problem.getEntityList().get(1).setValueList(List.of(problem.getValueList().get(1)));
        problem.getEntityList().get(1).setBasicValue(problem.getOtherValueList().get(0));
        problem.getEntityList().get(1).setSecondBasicValue(problem.getOtherValueList().get(0));
        // Update all variables
        supplyManager.resetWorkingSolution();
        placer.stepEnded(stepScope);
        // No more placements
        assertThat(placerIterator.hasNext()).isFalse();
    }
}
