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
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataListMultiVarSolution;
import ai.timefold.solver.core.testdomain.mixed.singleentity.unassignedvar.TestdataUnassignedListMultiVarSolution;

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
        // 1 = Generated Value 0 -> Entity 0[0] - Entity 0 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 2 = Generated Value 0 -> Entity 0[0] - Entity 0 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 3 = Generated Value 0 -> Entity 0[0] - Entity 0 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 4 = Generated Value 0 -> Entity 0[0] - Entity 0 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 5 = Generated Value 0 -> Entity 1[0] - Entity 0 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 6 = Generated Value 0 -> Entity 1[0] - Entity 0 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 7 = Generated Value 0 -> Entity 1[0] - Entity 0 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 8 = Generated Value 0 -> Entity 1[0] - Entity 0 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 9 = Generated Value 0 -> Entity 0[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 10 = Generated Value 0 -> Entity 0[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 11 = Generated Value 0 -> Entity 0[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 12 = Generated Value 0 -> Entity 0[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 13 = Generated Value 0 -> Entity 1[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 14 = Generated Value 0 -> Entity 1[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 15 = Generated Value 0 -> Entity 1[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 16 = Generated Value 0 -> Entity 1[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        assertThat(placerIterator.hasNext()).isTrue();
        var counter = new MutableInt();
        placerIterator.next().iterator().forEachRemaining(move -> counter.increment());
        assertThat(counter.intValue()).isEqualTo(16);

        // Accept the move -> 1 = Generated Value 0 -> Entity 0[0] - Entity 0 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        problem.getEntityList().get(0).setValueList(List.of(problem.getValueList().get(0)));
        problem.getEntityList().get(0).setBasicValue(problem.getOtherValueList().get(0));
        problem.getEntityList().get(0).setSecondBasicValue(problem.getOtherValueList().get(0));
        // Update all variables
        supplyManager.resetWorkingSolution();
        var stepScope = mock(AbstractStepScope.class);
        placer.stepEnded(stepScope);

        // Step 2
        // 1 = Generated Value 1 -> Entity 0[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 2 = Generated Value 1 -> Entity 0[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 3 = Generated Value 1 -> Entity 0[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 4 = Generated Value 1 -> Entity 0[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 5 = Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 6 = Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 7 = Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 8 = Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 9 = Generated Value 1 -> Entity 0[1] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 10 = Generated Value 1 -> Entity 0[1] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 11 = Generated Value 1 -> Entity 0[1] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 12 = Generated Value 1 -> Entity 0[1] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        counter.setValue(0);
        assertThat(placerIterator.hasNext()).isTrue();
        placerIterator.next().iterator().forEachRemaining(move -> counter.increment());
        assertThat(counter.intValue()).isEqualTo(12);

        // Accept the move = 5 = Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        problem.getEntityList().get(1).setValueList(List.of(problem.getValueList().get(1)));
        problem.getEntityList().get(1).setBasicValue(problem.getOtherValueList().get(0));
        problem.getEntityList().get(1).setSecondBasicValue(problem.getOtherValueList().get(0));
        // Update all variables
        supplyManager.resetWorkingSolution();
        placer.stepEnded(stepScope);
        // No more placements
        assertThat(placerIterator.hasNext()).isFalse();
    }

    @Test
    void testPinnedPlacersForConstructionHeuristic() {
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
        // Pin the first entity
        problem.getEntityList().get(0).setPinned(true);
        problem.getEntityList().get(0).setPinnedIndex(2);
        problem.getEntityList().get(0).setBasicValue(problem.getOtherValueList().get(0));
        problem.getEntityList().get(0).setSecondBasicValue(problem.getOtherValueList().get(0));
        problem.getEntityList().get(0).setValueList(List.of(problem.getValueList().get(0)));

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
        // 1 = Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 2 = Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 3 = Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 4 = Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        assertThat(placerIterator.hasNext()).isTrue();
        var counter = new MutableInt();
        placerIterator.next().iterator().forEachRemaining(move -> counter.increment());
        assertThat(counter.intValue()).isEqualTo(4);

        // Accept the move -> 1 = Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        problem.getEntityList().get(1).setValueList(List.of(problem.getValueList().get(1)));
        problem.getEntityList().get(1).setBasicValue(problem.getOtherValueList().get(0));
        problem.getEntityList().get(1).setSecondBasicValue(problem.getOtherValueList().get(0));
        // Update all variables
        supplyManager.resetWorkingSolution();
        var stepScope = mock(AbstractStepScope.class);
        placer.stepEnded(stepScope);
        assertThat(placerIterator.hasNext()).isFalse();
    }

    @Test
    void testUnassignedPlacersForConstructionHeuristic() {
        var solutionDescriptor = TestdataUnassignedListMultiVarSolution.buildSolutionDescriptor();
        var configPolicy = new HeuristicConfigPolicy.Builder<TestdataUnassignedListMultiVarSolution>()
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
        var placer = EntityPlacerFactory.<TestdataUnassignedListMultiVarSolution> create(placerConfig)
                .buildEntityPlacer(configPolicy);

        var problem = TestdataUnassignedListMultiVarSolution.generateUninitializedSolution(2, 2, 2);
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
        // 1 = Generated Value 0 -> Entity 0[0] - Entity 0 - null -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 2 = Generated Value 0 -> Entity 0[0] - Entity 0 - null -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 3 = Generated Value 0 -> Entity 0[0] - Entity 0 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 4 = Generated Value 0 -> Entity 0[0] - Entity 0 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 5 = Generated Value 0 -> Entity 0[0] - Entity 0 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 6 = Generated Value 0 -> Entity 0[0] - Entity 0 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 7 = Generated Value 0 -> Entity 0[0] - Entity 1 - null -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 8 = Generated Value 0 -> Entity 0[0] - Entity 1 - null -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 9 = Generated Value 0 -> Entity 0[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 10 = Generated Value 0 -> Entity 0[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 11 = Generated Value 0 -> Entity 0[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 12 = Generated Value 0 -> Entity 0[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 13 = Generated Value 0 -> Entity 1[0] - Entity 0 - null -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 14 = Generated Value 0 -> Entity 1[0] - Entity 0 - null -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 15 = Generated Value 0 -> Entity 1[0] - Entity 0 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 16 = Generated Value 0 -> Entity 1[0] - Entity 0 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 17 = Generated Value 0 -> Entity 1[0] - Entity 0 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 18 = Generated Value 0 -> Entity 1[0] - Entity 0 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 19 = Generated Value 0 -> Entity 1[0] - Entity 1 - null -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 20 = Generated Value 0 -> Entity 1[0] - Entity 1 - null -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 21 = Generated Value 0 -> Entity 1[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 22 = Generated Value 0 -> Entity 1[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 23 = Generated Value 0 -> Entity 1[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 24 = Generated Value 0 -> Entity 1[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 25 = NoChange - null -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 26 = NoChange - null -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 27 = NoChange - Entity 0 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 28 = NoChange - Entity 0 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 29 = NoChange - Entity 0 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 30 = NoChange - Entity 0 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 31 = NoChange - Entity 1 - null -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 32 = NoChange - Entity 1 - null -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 33 = NoChange - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 34 = NoChange - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 35 = NoChange - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 36 = NoChange - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        assertThat(placerIterator.hasNext()).isTrue();
        var counter = new MutableInt();
        placerIterator.next().iterator().forEachRemaining(move -> counter.increment());
        assertThat(counter.intValue()).isEqualTo(36);

        // Accept the move - 31 = NoChange - Entity 1 - null -> basicValue - Generated Other Value 0 -> secondBasicValue
        problem.getEntityList().get(0).setSecondBasicValue(problem.getOtherValueList().get(0));
        // Update all variables
        supplyManager.resetWorkingSolution();
        var stepScope = mock(AbstractStepScope.class);
        placer.stepEnded(stepScope);

        // Step 2
        // 1 = Generated Value 1 -> Entity 0[0] - Entity 0 - null -> basicValue
        // 2 = Generated Value 1 -> Entity 0[0] - Entity 0 - Generated Other Value 0 -> basicValue
        // 3 = Generated Value 1 -> Entity 0[0] - Entity 0 - Generated Other Value 1 -> basicValue
        // 4 = Generated Value 1 -> Entity 0[0] - Entity 1 - null -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 5 = Generated Value 1 -> Entity 0[0] - Entity 1 - null -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 6 = Generated Value 1 -> Entity 0[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 7 = Generated Value 1 -> Entity 0[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 8 = Generated Value 1 -> Entity 0[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 9 = Generated Value 1 -> Entity 0[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 10 = Generated Value 1 -> Entity 1[0] - Entity 0 - null -> basicValue
        // 11 = Generated Value 1 -> Entity 1[0] - Entity 0 - Generated Other Value 0 -> basicValue
        // 12 = Generated Value 1 -> Entity 1[0] - Entity 0 - Generated Other Value 1 -> basicValue
        // 13 = Generated Value 1 -> Entity 1[0] - Entity 1 - null -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 14 = Generated Value 1 -> Entity 1[0] - Entity 1 - null -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 15 = Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 16 = Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 17 = Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 18 = Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 19 = NoChange - Entity 0 - null -> basicValue
        // 20 = NoChange - Entity 0 - Generated Other Value 0 -> basicValue
        // 21 = NoChange - Entity 0 - Generated Other Value 1 -> basicValue
        // 22 = NoChange - Entity 1 - null -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 23 = NoChange - Entity 1 - null -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 24 = NoChange - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 25 = NoChange - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 26 = NoChange - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 27 = NoChange - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        counter.setValue(0);
        assertThat(placerIterator.hasNext()).isTrue();
        placerIterator.next().iterator().forEachRemaining(move -> counter.increment());
        assertThat(counter.intValue()).isEqualTo(27);

        // Accept the move - 22 = NoChange - Entity 1 - null -> basicValue - Generated Other Value 0 -> secondBasicValue
        problem.getEntityList().get(1).setSecondBasicValue(problem.getOtherValueList().get(0));
        // Update all variables
        supplyManager.resetWorkingSolution();
        placer.stepEnded(stepScope);
        // 1 = Generated Value 0 -> Entity 0[0] - Entity 0 - null -> basicValue
        // 2 = Generated Value 0 -> Entity 0[0] - Entity 0 - Generated Other Value 0 -> basicValue
        // 3 = Generated Value 0 -> Entity 0[0] - Entity 0 - Generated Other Value 1 -> basicValue
        // 4 = Generated Value 0 -> Entity 0[0] - Entity 1 - null -> basicValue
        // 5 = Generated Value 0 -> Entity 0[0] - Entity 1 - Generated Other Value 0 -> basicValue
        // 6 = Generated Value 0 -> Entity 0[0] - Entity 1 - Generated Other Value 1 -> basicValue
        // 7 = Generated Value 0 -> Entity 1[0] - Entity 0 - null -> basicValue
        // 8 = Generated Value 0 -> Entity 1[0] - Entity 0 - Generated Other Value 0 -> basicValue
        // 9 = Generated Value 0 -> Entity 1[0] - Entity 0 - Generated Other Value 1 -> basicValue
        // 10 = Generated Value 0 -> Entity 1[0] - Entity 1 - null -> basicValue
        // 11 = Generated Value 0 -> Entity 1[0] - Entity 1 - Generated Other Value 0 -> basicValue
        // 12 = Generated Value 0 -> Entity 1[0] - Entity 1 - Generated Other Value 1 -> basicValue
        // 13 = NoChange - Entity 0 - null -> basicValue
        // 14 = NoChange - Entity 0 - Generated Other Value 0 -> basicValue
        // 15 = NoChange - Entity 0 - Generated Other Value 1 -> basicValue
        // 16 = NoChange - Entity 1 - null -> basicValue
        // 17 = NoChange - Entity 1 - Generated Other Value 0 -> basicValue
        // 18 = NoChange - Entity 1 - Generated Other Value 1 -> basicValue
        counter.setValue(0);
        placerIterator.next().iterator().forEachRemaining(move -> counter.increment());
        assertThat(counter.intValue()).isEqualTo(18);
    }

    @Test
    void testPinnedUnassignedPlacersForConstructionHeuristic() {
        var solutionDescriptor = TestdataUnassignedListMultiVarSolution.buildSolutionDescriptor();
        var configPolicy = new HeuristicConfigPolicy.Builder<TestdataUnassignedListMultiVarSolution>()
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
        var placer = EntityPlacerFactory.<TestdataUnassignedListMultiVarSolution> create(placerConfig)
                .buildEntityPlacer(configPolicy);

        var problem = TestdataUnassignedListMultiVarSolution.generateUninitializedSolution(2, 2, 2);
        // Pin the first entity
        problem.getEntityList().get(0).setPinned(true);
        problem.getEntityList().get(0).setPinnedIndex(2);
        problem.getEntityList().get(0).setBasicValue(problem.getOtherValueList().get(0));
        problem.getEntityList().get(0).setSecondBasicValue(problem.getOtherValueList().get(0));
        problem.getEntityList().get(0).setValueList(List.of(problem.getValueList().get(0)));

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
        // 1 = Generated Value 1 -> Entity 1[0] - Entity 1 - null -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 2 = Generated Value 1 -> Entity 1[0] - Entity 1 - null -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 3 = Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 4 = Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 5 = Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 6 = Generated Value 1 -> Entity 1[0] - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 7 = NoChange - Entity 1 - null -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 8 = NoChange - Entity 1 - null -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 9 = NoChange - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 10 = NoChange - Entity 1 - Generated Other Value 0 -> basicValue - Generated Other Value 1 -> secondBasicValue
        // 11 = NoChange - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 0 -> secondBasicValue
        // 12 = NoChange - Entity 1 - Generated Other Value 1 -> basicValue - Generated Other Value 1 -> secondBasicValue
        assertThat(placerIterator.hasNext()).isTrue();
        var counter = new MutableInt();
        placerIterator.next().iterator().forEachRemaining(move -> counter.increment());
        assertThat(counter.intValue()).isEqualTo(12);

        // Accept the move - 7 = NoChange - Entity 1 - null -> basicValue - Generated Other Value 0 -> secondBasicValue
        problem.getEntityList().get(1).setSecondBasicValue(problem.getOtherValueList().get(0));
        // Update all variables
        supplyManager.resetWorkingSolution();
        var stepScope = mock(AbstractStepScope.class);
        placer.stepEnded(stepScope);
        counter.setValue(0);
        // 1 = Generated Value 1 -> Entity 1[0] - null -> secondBasicValue
        // 2 = Generated Value 1 -> Entity 1[0] - Generated Other Value 0 -> secondBasicValue
        // 3 = Generated Value 1 -> Entity 1[0] - Generated Other Value 1 -> secondBasicValue
        // 4 = NoChange - null -> secondBasicValue
        // 5 = NoChange - Generated Other Value 0 -> secondBasicValue
        // 6 = NoChange - Generated Other Value 1 -> secondBasicValue
        placerIterator.next().iterator().forEachRemaining(move -> counter.increment());
        assertThat(counter.intValue()).isEqualTo(6);
    }
}
