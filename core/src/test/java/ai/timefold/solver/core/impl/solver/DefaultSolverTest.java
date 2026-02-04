package ai.timefold.solver.core.impl.solver;

import static ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner.DECREASING_DIFFICULTY;
import static ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner.DECREASING_DIFFICULTY_IF_AVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.ConstraintMatchAwareIncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedEntityPlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedValuePlacerConfig;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner;
import ai.timefold.solver.core.config.heuristic.selector.entity.pillar.PillarSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.SubListSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.factory.MoveIteratorFactoryConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.RuinRecreateMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.SwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.TailChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListRuinRecreateMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.kopt.KOptListMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSorterManner;
import ai.timefold.solver.core.config.heuristic.selector.value.chained.SubChainSelectorConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveIteratorFactory;
import ai.timefold.solver.core.impl.score.DummySimpleScoreEasyScoreCalculator;
import ai.timefold.solver.core.impl.score.constraint.DefaultConstraintMatchTotal;
import ai.timefold.solver.core.impl.score.constraint.DefaultIndictment;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.preview.api.neighborhood.Neighborhood;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodBuilder;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodProvider;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedAnchor;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedEntity;
import ai.timefold.solver.core.testdomain.chained.TestdataChainedSolution;
import ai.timefold.solver.core.testdomain.chained.multientity.TestdataChainedBrownEntity;
import ai.timefold.solver.core.testdomain.chained.multientity.TestdataChainedGreenEntity;
import ai.timefold.solver.core.testdomain.chained.multientity.TestdataChainedMultiEntityAnchor;
import ai.timefold.solver.core.testdomain.chained.multientity.TestdataChainedMultiEntitySolution;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.TestdataListVarEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.list.pinned.TestdataPinnedListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.TestdataPinnedListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.TestdataPinnedListValue;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.TestdataListUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.TestdataListUnassignedEntityProvidingScoreCalculator;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.TestdataListUnassignedEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.mixed.multientity.TestdataMixedEntityEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.mixed.multientity.TestdataMixedMultiEntityFirstEntity;
import ai.timefold.solver.core.testdomain.mixed.multientity.TestdataMixedMultiEntityFirstValue;
import ai.timefold.solver.core.testdomain.mixed.multientity.TestdataMixedMultiEntitySecondEntity;
import ai.timefold.solver.core.testdomain.mixed.multientity.TestdataMixedMultiEntitySecondValue;
import ai.timefold.solver.core.testdomain.mixed.multientity.TestdataMixedMultiEntitySolution;
import ai.timefold.solver.core.testdomain.mixed.singleentity.MixedCustomMoveIteratorFactory;
import ai.timefold.solver.core.testdomain.mixed.singleentity.MixedCustomPhaseCommand;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedEntity;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedOtherValue;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedSolution;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedValue;
import ai.timefold.solver.core.testdomain.mixed.singleentity.unassignedvar.TestdataUnassignedMixedEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.mixed.singleentity.unassignedvar.TestdataUnassignedMixedEntity;
import ai.timefold.solver.core.testdomain.mixed.singleentity.unassignedvar.TestdataUnassignedMixedSolution;
import ai.timefold.solver.core.testdomain.multientity.TestdataHerdEntity;
import ai.timefold.solver.core.testdomain.multientity.TestdataLeadEntity;
import ai.timefold.solver.core.testdomain.multientity.TestdataMultiEntitySolution;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarEntity;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarSolution;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedEntity;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedSolution;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentConstraintProvider;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentEntity;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentSolution;
import ai.timefold.solver.core.testdomain.shadow.concurrent.TestdataConcurrentValue;
import ai.timefold.solver.core.testdomain.shadow.inverserelation.TestdataInverseRelationConstraintProvider;
import ai.timefold.solver.core.testdomain.shadow.inverserelation.TestdataInverseRelationEntity;
import ai.timefold.solver.core.testdomain.shadow.inverserelation.TestdataInverseRelationSolution;
import ai.timefold.solver.core.testdomain.shadow.inverserelation.TestdataInverseRelationValue;
import ai.timefold.solver.core.testdomain.sort.comparator.OneValuePerEntityComparatorEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.sort.comparator.TestdataComparatorSortableEntity;
import ai.timefold.solver.core.testdomain.sort.comparator.TestdataComparatorSortableSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.multivar.TestdataAllowsUnassignedMultiVarEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.multivar.TestdataAllowsUnassignedMultiVarEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingScoreCalculator;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingSolution;
import ai.timefold.solver.core.testutil.BestScoreChangedEvent;
import ai.timefold.solver.core.testutil.NoChangeCustomPhaseCommand;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(SoftAssertionsExtension.class)
@Execution(ExecutionMode.CONCURRENT)
class DefaultSolverTest {

    @Test
    void solve() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        solution = PlannerTestUtils.solveAssertingEvents(solverConfig, solution,
                BestScoreChangedEvent.constructionHeuristic(SimpleScore.ZERO, 0));
        assertThat(solution).isNotNull();
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getValue() == null)).isEmpty();
    }

    @Test
    void solveWithNeighborhoods() {
        var solverConfig = new SolverConfig()
                .withPreviewFeature(PreviewFeature.NEIGHBORHOODS)
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(DummyEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig()
                        .withBestScoreLimit("0")); // Should get there quickly.

        var solution = TestdataSolution.generateSolution(3, 2);

        solution = PlannerTestUtils.solve(solverConfig, solution, true);
        assertThat(solution).isNotNull();
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getValue() == null)).isEmpty();
    }

    @Test
    void solveWithDefaultNeighborhoodProviderListVar() {
        var solverConfig = new SolverConfig()
                .withPreviewFeature(PreviewFeature.NEIGHBORHOODS)
                .withSolutionClass(TestdataListSolution.class)
                .withEntityClasses(TestdataListEntity.class, TestdataListValue.class)
                .withEasyScoreCalculatorClass(TestingListEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig()
                        .withBestScoreLimit("0")); // Should get there quickly.

        // Both values are on the same entity; the goal of the solver is to move one of them to the other entity.
        var solution = TestdataListSolution.generateUninitializedSolution(2, 2);
        var v1 = solution.getValueList().get(0);
        var v2 = solution.getValueList().get(1);
        var e1 = solution.getEntityList().get(0);
        e1.addValue(v1);
        e1.addValue(v2);
        SolutionManager.updateShadowVariables(solution);

        solution = PlannerTestUtils.solve(solverConfig, solution, true);
        assertThat(solution).isNotNull();
    }

    @Test
    void solveWithDefaultNeighborhoodProviderMixedModel() {
        var solverConfig = new SolverConfig()
                .withPreviewFeature(PreviewFeature.NEIGHBORHOODS)
                .withSolutionClass(TestdataMixedSolution.class)
                .withEntityClasses(TestdataMixedEntity.class, TestdataMixedValue.class, TestdataMixedOtherValue.class)
                .withEasyScoreCalculatorClass(TestingMixedEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig()
                        .withBestScoreLimit("0")); // Should get there quickly.

        var solution = TestdataMixedSolution.generateUninitializedSolution(2, 2, 2);
        // Values are assigned in reverse; the solver needs to swap them.
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        e1.setBasicValue(solution.getOtherValueList().get(1));
        e2.setBasicValue(solution.getOtherValueList().get(0));
        // Both values are on the same entity; the goal of the solver is to move one of them to the other entity.
        var v1 = solution.getValueList().get(0);
        var v2 = solution.getValueList().get(1);
        e1.setValueList(new ArrayList<>(List.of(v1, v2)));
        SolutionManager.updateShadowVariables(solution);

        solution = PlannerTestUtils.solve(solverConfig, solution, true);
        assertThat(solution).isNotNull();
    }

    @Test
    void solveWithNeighborhoodsNotEnabled() {
        var solverConfig = new SolverConfig() // Preview feature not enabled.
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(DummyEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig()
                        .withBestScoreLimit("0")) // Should get there quickly.
                .withPhases(new LocalSearchPhaseConfig()
                        .withMoveProviderClass(TestingNeighborhoodProvider.class));

        var solution = TestdataSolution.generateSolution(3, 2);
        Assertions.assertThatThrownBy(() -> PlannerTestUtils.solve(solverConfig, solution))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("NEIGHBORHOODS");
    }

    @Test
    void solveWithNeighborhoodsAndMoveSelectors() {
        var solverConfig = new SolverConfig()
                .withPreviewFeature(PreviewFeature.NEIGHBORHOODS)
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(DummyEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig()
                        .withBestScoreLimit("0")) // Should get there quickly.
                // Swaps are coming from move selectors, other moves are coming from default Neighborhood provider.
                .withPhases(new LocalSearchPhaseConfig()
                        .withMoveSelectorConfig(new SwapMoveSelectorConfig()));

        var solution = TestdataSolution.generateSolution(3, 2);
        var result = PlannerTestUtils.solve(solverConfig, solution);
        Assertions.assertThat(result).isNotNull();
    }

    @Test
    void solveWithDefaultNeighborhoodProvider() {
        var solverConfig = new SolverConfig()
                .withPreviewFeature(PreviewFeature.NEIGHBORHOODS)
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(DummyEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig()
                        .withBestScoreLimit("0")); // Should get there quickly.

        var solution = TestdataSolution.generateSolution(3, 2);
        var result = PlannerTestUtils.solve(solverConfig, solution);
        Assertions.assertThat(result).isNotNull();
    }

    @Test
    void solveWithDefaultNeighborhoodProviderMultiVar() {
        var solverConfig = new SolverConfig()
                .withPreviewFeature(PreviewFeature.NEIGHBORHOODS)
                .withSolutionClass(TestdataMultiVarSolution.class)
                .withEntityClasses(TestdataMultiVarEntity.class)
                .withEasyScoreCalculatorClass(DummyMultiVarEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig()
                        .withBestScoreLimit("0")); // Should get there quickly.

        var solution = TestdataMultiVarSolution.generateSolution(2, 3, 3);
        var result = PlannerTestUtils.solve(solverConfig, solution);
        Assertions.assertThat(result).isNotNull();
    }

    @Test
    void solveWithDefaultNeighborhoodProviderMultiEntity() {
        var solverConfig = new SolverConfig()
                .withPreviewFeature(PreviewFeature.NEIGHBORHOODS)
                .withSolutionClass(TestdataMultiEntitySolution.class)
                .withEntityClasses(TestdataLeadEntity.class, TestdataHerdEntity.class)
                .withEasyScoreCalculatorClass(DummyMultiEntityEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig()
                        .withBestScoreLimit("0")); // Should get there quickly.

        // Set each value to be the same, so that the solver has to split them.
        var solution = TestdataMultiEntitySolution.generateUninitializedSolution(2, 2);
        solution.getLeadEntityList().forEach(e -> e.setValue(solution.getValueList().get(0)));
        solution.getHerdEntityList().forEach(e -> e.setLeadEntity(solution.getLeadEntityList().get(0)));

        // Zero result means each value has different value.
        var result = PlannerTestUtils.solve(solverConfig, solution);
        Assertions.assertThat(result).isNotNull();
    }

    @Test
    void solveCorruptedEasyPhaseAsserted() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withEnvironmentMode(EnvironmentMode.PHASE_ASSERT)
                .withEasyScoreCalculatorClass(CorruptedEasyScoreCalculator.class);

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        Assertions.assertThatThrownBy(() -> PlannerTestUtils.solve(solverConfig, solution, false))
                .hasMessageContaining("corruption")
                .hasMessageContaining(EnvironmentMode.FULL_ASSERT.name())
                .hasMessageContaining(EnvironmentMode.NO_ASSERT.name());
    }

    @Test
    void solveCorruptedEasyUnasserted() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withEnvironmentMode(EnvironmentMode.NO_ASSERT)
                .withEasyScoreCalculatorClass(CorruptedEasyScoreCalculator.class);

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        Assertions.assertThatNoException()
                .isThrownBy(() -> PlannerTestUtils.solve(solverConfig, solution, true));
    }

    @Test
    void solveCorruptedEasyUninitialized() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withEasyScoreCalculatorClass(CorruptedEasyScoreCalculator.class);

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        Assertions.assertThatThrownBy(() -> PlannerTestUtils.solve(solverConfig, solution, false))
                .hasMessageContaining("Score corruption")
                .hasMessageContaining("workingScore")
                .hasMessageContaining("uncorruptedScore")
                .hasMessageContaining("Score corruption analysis could not be generated");
    }

    @Test
    void solveCorruptedEasyInitialized() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withEasyScoreCalculatorClass(CorruptedEasyScoreCalculator.class);

        var solution = new TestdataSolution("s1");
        var value1 = new TestdataValue("v1");
        var value2 = new TestdataValue("v2");
        solution.setValueList(List.of(value1, value2));
        var entity1 = new TestdataEntity("e1");
        entity1.setValue(value1);
        var entity2 = new TestdataEntity("e2");
        entity2.setValue(value2);
        solution.setEntityList(List.of(entity1, entity2));

        Assertions.assertThatThrownBy(() -> PlannerTestUtils.solve(solverConfig, solution, false))
                .hasMessageContaining("Score corruption")
                .hasMessageContaining("workingScore")
                .hasMessageContaining("uncorruptedScore")
                .hasMessageContaining("Score corruption analysis could not be generated");
    }

    @Test
    void solveCorruptedIncrementalUninitialized() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withIncrementalScoreCalculatorClass(CorruptedIncrementalScoreCalculator.class));

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        Assertions.assertThatThrownBy(() -> PlannerTestUtils.solve(solverConfig, solution, false))
                .hasMessageContaining("Score corruption")
                .hasMessageContaining("workingScore")
                .hasMessageContaining("uncorruptedScore")
                .hasMessageContaining("Score corruption analysis:");
    }

    @Test
    void solveCorruptedIncrementalInitialized() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withIncrementalScoreCalculatorClass(CorruptedIncrementalScoreCalculator.class));
        var solverFactory = SolverFactory.<TestdataSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();

        var solution = new TestdataSolution("s1");
        var value1 = new TestdataValue("v1");
        var value2 = new TestdataValue("v2");
        solution.setValueList(List.of(value1, value2));
        var entity1 = new TestdataEntity("e1");
        entity1.setValue(value1);
        var entity2 = new TestdataEntity("e2");
        entity2.setValue(value2);
        solution.setEntityList(List.of(entity1, entity2));

        Assertions.assertThatThrownBy(() -> solver.solve(solution))
                .hasMessageContaining("Score corruption")
                .hasMessageContaining("workingScore")
                .hasMessageContaining("uncorruptedScore")
                .hasMessageContaining("Score corruption analysis:");
    }

    @Test
    void solveEmptyEntityList() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new CustomPhaseConfig()
                        .withCustomPhaseCommands(new FailCommand()));

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Collections.emptyList());

        solution = PlannerTestUtils.solveAssertingEvents(solverConfig, solution,
                BestScoreChangedEvent.solvingStarted(SimpleScore.ZERO));
        assertThat(solution).isNotNull();
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getValue() == null)).isEmpty();
    }

    private static final class FailCommand implements PhaseCommand<Object> {

        @Override
        public void changeWorkingSolution(ScoreDirector<Object> scoreDirector, BooleanSupplier isPhaseTerminated) {
            fail("All phases should be skipped because there are no movable entities.");
        }
    }

    @Test
    void solveChainedEmptyEntityList() {
        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataChainedSolution.class, TestdataChainedEntity.class)
                .withPhases(new CustomPhaseConfig()
                        .withCustomPhaseCommands(new FailCommand()));

        var solution = new TestdataChainedSolution("s1");
        solution.setChainedAnchorList(Arrays.asList(new TestdataChainedAnchor("v1"), new TestdataChainedAnchor("v2")));
        solution.setChainedEntityList(Collections.emptyList());

        solution = PlannerTestUtils.solveAssertingEvents(solverConfig, solution,
                BestScoreChangedEvent.solvingStarted(SimpleScore.ZERO));
        assertThat(solution).isNotNull();
        assertThat(solution.getScore()).isNotNull();
    }

    @Test
    void solveEmptyEntityListAndEmptyValueList() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new CustomPhaseConfig()
                        .withCustomPhaseCommands(new FailCommand()));

        var solution = new TestdataSolution("s1");
        solution.setValueList(Collections.emptyList());
        solution.setEntityList(Collections.emptyList());

        solution = PlannerTestUtils.solve(solverConfig, solution, true);
        assertThat(solution).isNotNull();
        assertThat(solution.getScore()).isNotNull();
    }

    @Test
    void solvePinnedEntityList() {
        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataPinnedSolution.class, TestdataPinnedEntity.class)
                .withPhases(new CustomPhaseConfig()
                        .withCustomPhaseCommands(new FailCommand()));

        var solution = new TestdataPinnedSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        solution.setValueList(Arrays.asList(v1, v2));
        solution.setEntityList(Arrays.asList(new TestdataPinnedEntity("e1", v1, true, false),
                new TestdataPinnedEntity("e2", v2, false, true)));

        solution = PlannerTestUtils.solve(solverConfig, solution, true);
        assertThat(solution).isNotNull();
        assertThat(solution.getScore()).isEqualTo(SimpleScore.ZERO);
    }

    @Test
    void solveStopsWhenUninitialized() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new CustomPhaseConfig()
                        .withCustomPhaseCommandClassList(Collections.singletonList(NoChangeCustomPhaseCommand.class)));

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"),
                new TestdataEntity("e3"), new TestdataEntity("e4"), new TestdataEntity("e5")));

        solution = PlannerTestUtils.solve(solverConfig, solution, false);
        assertThat(solution).isNotNull();
        assertThat(solution.getScore()).isNotNull();
    }

    @Test
    void solveStopsWhenPartiallyInitialized() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig()
                        // Run only 2 steps, although 5 are needed to initialize all entities
                        .withTerminationConfig(new TerminationConfig().withStepCountLimit(2)));

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"),
                new TestdataEntity("e3"), new TestdataEntity("e4"), new TestdataEntity("e5")));

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution.getEntityList().stream().filter(e -> e.getValue() == null))
                .isNotEmpty();
    }

    @Test
    @Timeout(60)
    void solveWithProblemChange() throws InterruptedException {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        solverConfig.setDaemon(true); // Avoid terminating the solver too quickly.
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
        var solver = solverFactory.buildSolver();
        final var valueCount = 4;
        var solution = TestdataSolution.generateSolution(valueCount, valueCount);

        var bestSolution = new AtomicReference<TestdataSolution>();
        var solutionWithProblemChangeReceived = new CountDownLatch(1);
        solver.addEventListener(bestSolutionChangedEvent -> {
            if (bestSolutionChangedEvent.isEveryProblemChangeProcessed()) {
                var newBestSolution = bestSolutionChangedEvent.getNewBestSolution();
                if (newBestSolution.getValueList().size() == valueCount + 1) {
                    bestSolution.set(newBestSolution);
                    solutionWithProblemChangeReceived.countDown();
                }
            }
        });

        var executorService = Executors.newSingleThreadExecutor();
        try {
            executorService.submit(() -> {
                solver.solve(solution);
            });

            solver.addProblemChange((workingSolution, problemChangeDirector) -> problemChangeDirector
                    .addProblemFact(new TestdataValue("added value"), solution.getValueList()::add));

            solutionWithProblemChangeReceived.await();
            assertThat(bestSolution.get().getValueList()).hasSize(valueCount + 1);
            solver.terminateEarly();
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void solveRepeatedlyBasicVariable(SoftAssertions softly) {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        var phaseConfig = new ConstructionHeuristicPhaseConfig();
        // Run only 2 steps at a time, although 5 are needed to initialize all entities.
        var stepCountLimit = 2;
        phaseConfig.setTerminationConfig(new TerminationConfig().withStepCountLimit(stepCountLimit));
        solverConfig.setPhaseConfigList(Collections.singletonList(phaseConfig));
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
        var solver = solverFactory.buildSolver();

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        final var entityCount = 5;
        solution.setEntityList(IntStream.rangeClosed(1, entityCount)
                .mapToObj(id -> new TestdataEntity("e" + id))
                .toList());

        var score = SolutionManager.create(solverFactory).update(solution);
        assertThat(score).isNotNull();

        // Keep restarting the solver until the solution is initialized.
        for (var initScore = -entityCount; initScore < 0; initScore += stepCountLimit) {
            var uninitializedCount = solution.getEntityList().stream().filter(e -> e.getValue() == null)
                    .count();
            softly.assertThat(uninitializedCount).isEqualTo(-initScore);
            solution = solver.solve(solution);
        }

        // Finally, the solution is initialized.
        softly.assertThat(solution.getEntityList().stream().filter(e -> e.getValue() == null)).isEmpty();
    }

    @Test
    void solveRepeatedlyListVariable(SoftAssertions softly) {
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class);

        // Run only 7 steps at a time, although the total number of steps needed to complete CH is equal to maximumValueRangeSize.
        final var stepCountLimit = 7;
        var phaseConfig = new ConstructionHeuristicPhaseConfig();
        phaseConfig.setTerminationConfig(new TerminationConfig().withStepCountLimit(stepCountLimit));
        solverConfig.setPhaseConfigList(Collections.singletonList(phaseConfig));
        SolverFactory<TestdataListSolution> solverFactory = SolverFactory.create(solverConfig);
        var solver = solverFactory.buildSolver();

        final var valueCount = 24;
        var solution = TestdataListSolution.generateUninitializedSolution(valueCount, 8);

        var score = SolutionManager.create(solverFactory).update(solution);
        assertThat(score).isNotNull();

        // Keep restarting the solver until the solution is initialized.
        for (var initScore = -valueCount; initScore < 0; initScore += stepCountLimit) {
            var initializedCount = solution.getEntityList().stream()
                    .map(TestdataListEntity::getValueList)
                    .mapToInt(List::size)
                    .sum();
            softly.assertThat(initializedCount).isLessThan(valueCount);
            solution = solver.solve(solution);
        }

        // Finally, the solution is initialized.
        var initializedCount = solution.getEntityList().stream()
                .map(TestdataListEntity::getValueList)
                .mapToInt(List::size)
                .sum();
        softly.assertThat(initializedCount).isEqualTo(valueCount);
    }

    @Test
    void solveWithAllowsUnassignedValuesListVariable() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataAllowsUnassignedValuesListSolution.class,
                TestdataAllowsUnassignedValuesListEntity.class, TestdataAllowsUnassignedValuesListValue.class)
                .withEasyScoreCalculatorClass(TestdataAllowsUnassignedValuesListEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig().withBestScoreLimit("0"))
                .withPhases();

        var value1 = new TestdataAllowsUnassignedValuesListValue("v1");
        var value2 = new TestdataAllowsUnassignedValuesListValue("v2");
        var value3 = new TestdataAllowsUnassignedValuesListValue("v3");
        var value4 = new TestdataAllowsUnassignedValuesListValue("v4");
        var entity = new TestdataAllowsUnassignedValuesListEntity("e1", value1, value2);

        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(Arrays.asList(value1, value2, value3, value4));
        SolutionManager.updateShadowVariables(solution);

        var bestSolution = PlannerTestUtils.solve(solverConfig, solution);
        assertSoftly(softly -> {
            softly.assertThat(bestSolution.getScore()).isEqualTo(SimpleScore.of(0)); // Nothing is assigned.
            var firstEntity = bestSolution.getEntityList().get(0);
            softly.assertThat(firstEntity.getValueList()).isEmpty();
        });

    }

    @Test
    void solveWithAllowsUnassignedValuesListVariableAndOnlyDown() {
        var scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withInitializingScoreTrend("ONLY_DOWN")
                .withEasyScoreCalculatorClass(MinimizeUnassignedEntitiesEasyScoreCalculator.class);
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataAllowsUnassignedValuesListSolution.class,
                TestdataAllowsUnassignedValuesListEntity.class, TestdataAllowsUnassignedValuesListValue.class)
                .withScoreDirectorFactory(scoreDirectorFactoryConfig)
                .withPhases(new ConstructionHeuristicPhaseConfig());

        var value1 = new TestdataAllowsUnassignedValuesListValue("v1");
        var value2 = new TestdataAllowsUnassignedValuesListValue("v2");
        var value3 = new TestdataAllowsUnassignedValuesListValue("v3");
        var value4 = new TestdataAllowsUnassignedValuesListValue("v4");
        var entity = new TestdataAllowsUnassignedValuesListEntity("e1", value1, value2);

        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(Arrays.asList(value1, value2, value3, value4));
        SolutionManager.updateShadowVariables(solution);

        var bestSolution = PlannerTestUtils.solve(solverConfig, solution);
        assertSoftly(softly -> {
            // Everything is assigned, even though ONLY_DOWN caused the CH to pick the first selected move.
            // Checks for a bug where NoChangeMove would be generated first, meaning nothing would get assigned.
            softly.assertThat(bestSolution.getScore()).isEqualTo(SimpleScore.of(4));
            var firstEntity = bestSolution.getEntityList().get(0);
            softly.assertThat(firstEntity.getValueList()).hasSize(4);
        });

    }

    /**
     * This test is to verify that the CH step is only picked if all moves of the step have been processed.
     * If the CH is terminated before all moves have been processed,
     * the solution should use the result of the previous fully completed step.
     * 
     * @see <a href="https://github.com/TimefoldAI/timefold-solver/issues/1130">GitHub issue 1130</a>.
     */
    @Test
    void solveWithCHAllowsUnassignedValuesListVariableAndTerminateInStep() {
        // This test relies on the implementation detail (!) 
        // that the move to keep the entity unassigned is the last move.
        // Therefore, when our termination kills the solver after the first move in the first step,
        // the move to keep the entity unassigned won't have happened yet.
        // This means the final best solution will have nothing assigned.
        var scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withEasyScoreCalculatorClass(MaximizeUnusedEntitiesEasyScoreCalculator.class);
        var constructionHeuristicConfig = new ConstructionHeuristicPhaseConfig()
                .withTerminationConfig(new TerminationConfig()
                        .withMoveCountLimit(1L));
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataAllowsUnassignedValuesListSolution.class,
                TestdataAllowsUnassignedValuesListEntity.class, TestdataAllowsUnassignedValuesListValue.class)
                .withScoreDirectorFactory(scoreDirectorFactoryConfig)
                .withPhases(constructionHeuristicConfig);

        var value1 = new TestdataAllowsUnassignedValuesListValue("v1");
        var entity = new TestdataAllowsUnassignedValuesListEntity("e1");

        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(value1));
        SolutionManager.updateShadowVariables(solution);

        var bestSolution = PlannerTestUtils.solve(solverConfig, solution, true);
        assertThat(bestSolution.getScore()).isEqualTo(SimpleScore.of(1));
    }

    /**
     * Verifies <a href="https://issues.redhat.com/browse/PLANNER-2798">PLANNER-2798</a>.
     */
    @Test
    void solveWithMultipleChainedPlanningEntities() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataChainedMultiEntitySolution.class)
                .withEntityClasses(TestdataChainedBrownEntity.class, TestdataChainedGreenEntity.class)
                .withEasyScoreCalculatorClass(DummySimpleScoreEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig().withBestScoreLimit("0"))
                .withPhases(
                        // Each planning entity class needs a separate CH phase.
                        new ConstructionHeuristicPhaseConfig().withEntityPlacerConfig(new QueuedEntityPlacerConfig()
                                .withEntitySelectorConfig(new EntitySelectorConfig(TestdataChainedBrownEntity.class))),
                        new ConstructionHeuristicPhaseConfig().withEntityPlacerConfig(new QueuedEntityPlacerConfig()
                                .withEntitySelectorConfig(new EntitySelectorConfig(TestdataChainedGreenEntity.class))),
                        new LocalSearchPhaseConfig().withMoveSelectorConfig(new UnionMoveSelectorConfig().withMoveSelectors(
                                new ChangeMoveSelectorConfig(),
                                new SwapMoveSelectorConfig(),
                                // Include TailChainSwapMoveSelector, which uses ExternalizedAnchorVariableSupply.
                                new TailChainSwapMoveSelectorConfig().withEntitySelectorConfig(
                                        new EntitySelectorConfig(TestdataChainedBrownEntity.class)),
                                new TailChainSwapMoveSelectorConfig().withEntitySelectorConfig(
                                        new EntitySelectorConfig(TestdataChainedGreenEntity.class)))));
        SolverFactory<TestdataChainedMultiEntitySolution> solverFactory = SolverFactory.create(solverConfig);
        var solver = solverFactory.buildSolver();

        var anchors = List.of(
                new TestdataChainedMultiEntityAnchor("a1"),
                new TestdataChainedMultiEntityAnchor("a2"),
                new TestdataChainedMultiEntityAnchor("a3"));
        var brownEntities = List.of(
                new TestdataChainedBrownEntity("b1"),
                new TestdataChainedBrownEntity("b2"));
        var greenEntities = List.of(
                new TestdataChainedGreenEntity("g1"),
                new TestdataChainedGreenEntity("g2"),
                new TestdataChainedGreenEntity("g3"));
        var solution =
                new TestdataChainedMultiEntitySolution(brownEntities, greenEntities, anchors);

        solution = solver.solve(solution);
        assertThat(solution).isNotNull();
        assertThat(solution.getScore()).isNotNull();
    }

    @Test
    void solveWithPlanningListVariableEntityPinFair() {
        var expectedValueCount = 4;
        var solution = TestdataPinnedListSolution.generateUninitializedSolution(expectedValueCount, 3);
        var pinnedEntity = solution.getEntityList().get(0);
        var pinnedList = pinnedEntity.getValueList();
        var pinnedValue = solution.getValueList().get(0);
        pinnedList.add(pinnedValue);
        pinnedEntity.setPinned(true);

        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataPinnedListSolution.class, TestdataPinnedListEntity.class,
                        TestdataPinnedListValue.class)
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withEasyScoreCalculatorClass(MinimizeUnusedEntitiesEasyScoreCalculator.class);
        var solverFactory = SolverFactory.<TestdataPinnedListSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();
        solution = solver.solve(updateSolution(solverFactory, solution));

        assertThat(solution).isNotNull();
        assertThat(solution.getScore()).isEqualTo(SimpleScore.ZERO); // No unused entities.
        assertThat(solution.getEntityList().get(0).getValueList())
                .containsExactly(solution.getValueList().get(0));
        var actualValueCount = solution.getEntityList().stream()
                .mapToInt(e -> e.getValueList().size())
                .sum();
        assertThat(actualValueCount).isEqualTo(expectedValueCount);
    }

    private static <Solution_> Solution_ updateSolution(SolverFactory<Solution_> solverFactory, Solution_ solution) {
        SolutionManager<Solution_, ?> solutionManager = SolutionManager.create(solverFactory);
        solutionManager.update(solution);
        return solution;
    }

    @Test
    void solveWithPlanningListVariableEntityPinUnfair() {
        var expectedValueCount = 4;
        var solution = TestdataPinnedListSolution.generateUninitializedSolution(expectedValueCount, 3);
        var pinnedEntity = solution.getEntityList().get(0);
        var pinnedList = pinnedEntity.getValueList();
        var pinnedValue = solution.getValueList().get(0);
        pinnedList.add(pinnedValue);
        pinnedEntity.setPinned(true);

        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataPinnedListSolution.class, TestdataPinnedListEntity.class,
                        TestdataPinnedListValue.class)
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withEasyScoreCalculatorClass(MaximizeUnusedEntitiesEasyScoreCalculator.class);
        var solverFactory = SolverFactory.<TestdataPinnedListSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();
        solution = solver.solve(updateSolution(solverFactory, solution));

        assertThat(solution).isNotNull();
        // 1 unused entity; out of 3 total, one is pinned and the other gets all the values.
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(1));
        assertThat(solution.getEntityList().get(0).getValueList())
                .containsExactly(solution.getValueList().get(0));
        var actualValueCount = solution.getEntityList().stream()
                .mapToInt(e -> e.getValueList().size())
                .sum();
        assertThat(actualValueCount).isEqualTo(expectedValueCount);
    }

    @Test
    void solveWithPlanningListVariablePinIndexFair() {
        var expectedValueCount = 4;
        var solution = TestdataPinnedWithIndexListSolution.generateUninitializedSolution(expectedValueCount, 3);
        // Pin the first list entirely.
        var pinnedEntity = solution.getEntityList().get(0);
        var pinnedList = pinnedEntity.getValueList();
        var pinnedValue = solution.getValueList().get(0);
        pinnedList.add(pinnedValue);
        pinnedEntity.setPinned(true);
        // In the second list, pin only the first value.
        var partiallyPinnedEntity = solution.getEntityList().get(1);
        var partiallyPinnedList = partiallyPinnedEntity.getValueList();
        var partiallyPinnedValue1 = solution.getValueList().get(1);
        var partiallyPinnedValue2 = solution.getValueList().get(2);
        partiallyPinnedList.add(partiallyPinnedValue1);
        partiallyPinnedList.add(partiallyPinnedValue2);
        partiallyPinnedEntity.setPinIndex(1); // The first value is pinned.
        partiallyPinnedEntity.setPinned(false); // The list isn't pinned overall.

        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataPinnedWithIndexListSolution.class, TestdataPinnedWithIndexListEntity.class,
                        TestdataPinnedWithIndexListValue.class)
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withEasyScoreCalculatorClass(MinimizeUnusedEntitiesEasyScoreCalculator.class);
        var solverFactory = SolverFactory.<TestdataPinnedWithIndexListSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();
        solution = solver.solve(updateSolution(solverFactory, solution));

        assertThat(solution).isNotNull();
        assertThat(solution.getScore()).isEqualTo(SimpleScore.ZERO); // No unused entities.
        assertThat(solution.getEntityList().get(0).getValueList()).containsExactly(solution.getValueList().get(0));
        assertThat(solution.getEntityList().get(1).getValueList())
                .first()
                .isEqualTo(solution.getValueList().get(1));
        var actualValueCount = solution.getEntityList().stream()
                .mapToInt(e -> e.getValueList().size())
                .sum();
        assertThat(actualValueCount).isEqualTo(expectedValueCount);
    }

    @Test
    void solveWithPlanningListVariablePinIndexUnfair() {
        var expectedValueCount = 4;
        var solution = TestdataPinnedWithIndexListSolution.generateUninitializedSolution(expectedValueCount, 3);
        // Pin the first list entirely.
        var pinnedEntity = solution.getEntityList().get(0);
        var pinnedList = pinnedEntity.getValueList();
        var pinnedValue = solution.getValueList().get(0);
        pinnedList.add(pinnedValue);
        pinnedEntity.setPinned(true);
        // In the second list, pin only the first value.
        var partiallyPinnedEntity = solution.getEntityList().get(1);
        var partiallyPinnedList = partiallyPinnedEntity.getValueList();
        var partiallyPinnedValue1 = solution.getValueList().get(1);
        var partiallyPinnedValue2 = solution.getValueList().get(2);
        partiallyPinnedList.add(partiallyPinnedValue1);
        partiallyPinnedList.add(partiallyPinnedValue2);
        partiallyPinnedEntity.setPinIndex(1); // The first value is pinned.
        partiallyPinnedEntity.setPinned(false); // The list isn't pinned overall.

        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataPinnedWithIndexListSolution.class, TestdataPinnedWithIndexListEntity.class,
                        TestdataPinnedWithIndexListValue.class)
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withEasyScoreCalculatorClass(MaximizeUnusedEntitiesEasyScoreCalculator.class);
        var solverFactory = SolverFactory.<TestdataPinnedWithIndexListSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();
        solution = solver.solve(updateSolution(solverFactory, solution));

        assertThat(solution).isNotNull();
        // 1 unused entity; out of 3 total, one is pinned and the other gets all the values.
        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(1));
        assertThat(solution.getEntityList().get(0).getValueList()).containsExactly(solution.getValueList().get(0));
        assertThat(solution.getEntityList().get(1).getValueList())
                .containsExactlyInAnyOrder(solution.getValueList().get(1),
                        solution.getValueList().get(2),
                        solution.getValueList().get(3));
        assertThat(solution.getEntityList().get(2).getValueList())
                .isEmpty();
    }

    @Test
    void solveCustomConfigListVariable() {
        var valueSelectorConfig = new ValueSelectorConfig("valueList")
                .withId("valueList");
        var mimicReplayingValueSelectorConfig = new ValueSelectorConfig()
                .withMimicSelectorRef("valueList")
                .withVariableName("valueList");
        var valuePlacerConfig = new QueuedValuePlacerConfig()
                .withValueSelectorConfig(valueSelectorConfig)
                .withMoveSelectorConfig(new ListChangeMoveSelectorConfig()
                        .withValueSelectorConfig(mimicReplayingValueSelectorConfig));

        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class)
                .withPhases(new ConstructionHeuristicPhaseConfig()
                        .withEntityPlacerConfig(valuePlacerConfig),
                        new LocalSearchPhaseConfig().withTerminationConfig(new TerminationConfig().withStepCountLimit(16)))
                .withEasyScoreCalculatorClass(TestdataListVarEasyScoreCalculator.class);

        var problem = TestdataListSolution.generateUninitializedSolution(2, 2);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getValueList().isEmpty()))
                .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("generateMovesForSingleVar")
    void solveSingleVarMoveConfig(MoveSelectorConfig moveSelectionConfig) {
        // Local search
        var localSearchConfig =
                new LocalSearchPhaseConfig()
                        .withMoveSelectorConfig(moveSelectionConfig)
                        .withTerminationConfig(new TerminationConfig().withMoveCountLimit(40L));
        // Solver config
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig(), localSearchConfig)
                .withEasyScoreCalculatorClass(DummySimpleScoreEasyScoreCalculator.class);

        var problem = TestdataSolution.generateUninitializedSolution(2, 2);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .doesNotThrowAnyException();
    }

    private static List<MoveSelectorConfig> generateMovesForSingleVar() {
        var allMoveSelectionConfigList = new ArrayList<MoveSelectorConfig>();
        // Change - basic
        allMoveSelectionConfigList.add(new ChangeMoveSelectorConfig());
        // Swap - basic
        allMoveSelectionConfigList.add(new SwapMoveSelectorConfig());
        // Pillar change - basic
        allMoveSelectionConfigList.add(new PillarChangeMoveSelectorConfig());
        // Pilar swap - basic
        allMoveSelectionConfigList.add(new PillarSwapMoveSelectorConfig());
        // R&R - basic
        allMoveSelectionConfigList.add(new RuinRecreateMoveSelectorConfig());
        // Union of all moves
        allMoveSelectionConfigList.add(new UnionMoveSelectorConfig(List.copyOf(allMoveSelectionConfigList)));
        return allMoveSelectionConfigList;
    }

    @ParameterizedTest
    @MethodSource("generateMovesForChainedVar")
    void solveChainedVarMoveConfig(MoveSelectorConfig moveSelectionConfig) {
        // Local search
        var localSearchConfig =
                new LocalSearchPhaseConfig()
                        .withMoveSelectorConfig(moveSelectionConfig)
                        .withTerminationConfig(new TerminationConfig().withMoveCountLimit(40L));
        // Solver config
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataChainedSolution.class, TestdataChainedEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig(), localSearchConfig)
                .withEasyScoreCalculatorClass(DummySimpleScoreEasyScoreCalculator.class);

        var problem = TestdataChainedSolution.generateUninitializedSolution(2, 2);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .doesNotThrowAnyException();
    }

    private static List<MoveSelectorConfig> generateMovesForChainedVar() {
        var allMoveSelectionConfigList = new ArrayList<MoveSelectorConfig>();
        // Change - chained
        allMoveSelectionConfigList.add(new ChangeMoveSelectorConfig());
        // Swap - chained
        allMoveSelectionConfigList.add(new SwapMoveSelectorConfig());
        // Tail Chain - chained
        allMoveSelectionConfigList.add(new TailChainSwapMoveSelectorConfig()
                .withValueSelectorConfig(new ValueSelectorConfig().withVariableName("chainedObject")));
        // Subchain chain - chained
        allMoveSelectionConfigList
                .add(new SubChainChangeMoveSelectorConfig().withSubChainSelectorConfig(new SubChainSelectorConfig()
                        .withValueSelectorConfig(new ValueSelectorConfig().withVariableName("chainedObject")))
                        .withValueSelectorConfig(new ValueSelectorConfig().withVariableName("chainedObject")));
        // Subchain swap - chained
        allMoveSelectionConfigList
                .add(new SubChainSwapMoveSelectorConfig().withSubChainSelectorConfig(new SubChainSelectorConfig()
                        .withValueSelectorConfig(new ValueSelectorConfig().withVariableName("chainedObject"))));
        // Union of all moves
        allMoveSelectionConfigList.add(new UnionMoveSelectorConfig(List.copyOf(allMoveSelectionConfigList)));
        return allMoveSelectionConfigList;
    }

    @ParameterizedTest
    @MethodSource("generateMovesForListVar")
    void solveListVarMoveConfig(MoveSelectorConfig moveSelectionConfig) {
        // Local search
        var localSearchConfig =
                new LocalSearchPhaseConfig()
                        .withMoveSelectorConfig(moveSelectionConfig)
                        .withTerminationConfig(new TerminationConfig().withMoveCountLimit(40L));
        // Solver config
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class)
                .withPhases(new ConstructionHeuristicPhaseConfig(), localSearchConfig)
                .withEasyScoreCalculatorClass(DummySimpleScoreEasyScoreCalculator.class);

        var problem = TestdataListSolution.generateUninitializedSolution(2, 2);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .doesNotThrowAnyException();
    }

    private static List<MoveSelectorConfig> generateMovesForListVar() {
        var allMoveSelectionConfigList = new ArrayList<MoveSelectorConfig>();
        // Change - basic
        allMoveSelectionConfigList.add(new ListChangeMoveSelectorConfig());
        // Swap - basic
        allMoveSelectionConfigList.add(new ListSwapMoveSelectorConfig());
        // Pillar change - basic
        allMoveSelectionConfigList.add(new SubListChangeMoveSelectorConfig());
        // Pilar swap - basic
        allMoveSelectionConfigList.add(new SubListSwapMoveSelectorConfig());
        // R&R - basic
        allMoveSelectionConfigList.add(new ListRuinRecreateMoveSelectorConfig());
        // Union of all moves
        allMoveSelectionConfigList.add(new UnionMoveSelectorConfig(List.copyOf(allMoveSelectionConfigList)));
        return allMoveSelectionConfigList;
    }

    @ParameterizedTest
    @MethodSource("generateMovesForMultiVar")
    void solveMultiVarMoveConfig(MoveSelectorConfig moveSelectionConfig) {
        // Local search
        var localSearchConfig =
                new LocalSearchPhaseConfig()
                        .withMoveSelectorConfig(moveSelectionConfig)
                        .withTerminationConfig(new TerminationConfig().withMoveCountLimit(40L));
        // Solver config
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataMultiVarSolution.class, TestdataMultiVarEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig(), localSearchConfig)
                .withEasyScoreCalculatorClass(DummySimpleScoreEasyScoreCalculator.class);

        var problem = TestdataMultiVarSolution.generateUninitializedSolution(2, 2);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .doesNotThrowAnyException();
    }

    private static List<MoveSelectorConfig> generateMovesForMultiVar() {
        var allMoveSelectionConfigList = new ArrayList<MoveSelectorConfig>();
        // Change - basic
        allMoveSelectionConfigList.add(new ChangeMoveSelectorConfig());
        // Swap - basic
        allMoveSelectionConfigList.add(new SwapMoveSelectorConfig());
        // Pillar change - basic
        var pillarChangeMoveSelectorConfig = new PillarChangeMoveSelectorConfig();
        var pillarChangeEntitySelectorConfig =
                new EntitySelectorConfig().withEntityClass(TestdataMultiVarEntity.class);
        var pillarChangeValueSelectorConfig = new ValueSelectorConfig().withVariableName("primaryValue");
        pillarChangeMoveSelectorConfig
                .withPillarSelectorConfig(new PillarSelectorConfig().withEntitySelectorConfig(pillarChangeEntitySelectorConfig))
                .withValueSelectorConfig(pillarChangeValueSelectorConfig);
        allMoveSelectionConfigList.add(pillarChangeMoveSelectorConfig);
        // Pilar swap - basic
        allMoveSelectionConfigList.add(new PillarSwapMoveSelectorConfig().withPillarSelectorConfig(
                new PillarSelectorConfig().withEntitySelectorConfig(pillarChangeEntitySelectorConfig)));
        // Union of all moves
        allMoveSelectionConfigList.add(new UnionMoveSelectorConfig(List.copyOf(allMoveSelectionConfigList)));
        return allMoveSelectionConfigList;
    }

    @Test
    void solveMultiEntity() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataMultiEntitySolution.class, TestdataLeadEntity.class, TestdataHerdEntity.class)
                .withEasyScoreCalculatorClass(DummySimpleScoreEasyScoreCalculator.class)
                .withPhaseList(Collections.emptyList())
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(40L));

        var problem = TestdataMultiEntitySolution.generateUninitializedSolution(3, 2);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        assertThat(solution).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("generateMovesForMultiEntity")
    void solveMultiEntityMoveConfig(MoveSelectorConfig moveSelectionConfig) {
        // Construction Heuristic
        var leadConstructionHeuristicConfig = new ConstructionHeuristicPhaseConfig()
                .withEntityPlacerConfig(new QueuedEntityPlacerConfig()
                        .withEntitySelectorConfig(new EntitySelectorConfig()
                                .withId(TestdataLeadEntity.class.getName())
                                .withEntityClass(TestdataLeadEntity.class)));
        var herdConstructionHeuristicConfig = new ConstructionHeuristicPhaseConfig()
                .withEntityPlacerConfig(new QueuedEntityPlacerConfig()
                        .withEntitySelectorConfig(new EntitySelectorConfig()
                                .withId(TestdataHerdEntity.class.getName())
                                .withEntityClass(TestdataHerdEntity.class)));
        // Local search
        var localSearchConfig =
                new LocalSearchPhaseConfig()
                        .withMoveSelectorConfig(moveSelectionConfig)
                        .withTerminationConfig(new TerminationConfig().withMoveCountLimit(40L));
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataMultiEntitySolution.class, TestdataLeadEntity.class, TestdataHerdEntity.class)
                .withPhases(leadConstructionHeuristicConfig, herdConstructionHeuristicConfig, localSearchConfig)
                .withEasyScoreCalculatorClass(DummySimpleScoreEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig().withBestScoreLimit("0"));

        var problem = TestdataMultiEntitySolution.generateUninitializedSolution(2, 2);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        assertThat(solution).isNotNull();
    }

    private static List<MoveSelectorConfig> generateMovesForMultiEntity() {
        var allMoveSelectionConfigList = new ArrayList<MoveSelectorConfig>();
        // Change - basic
        allMoveSelectionConfigList.add(new ChangeMoveSelectorConfig());
        // Swap - basic
        allMoveSelectionConfigList.add(new SwapMoveSelectorConfig());
        // Pillar change - basic
        var pillarChangeMoveSelectorConfig = new PillarChangeMoveSelectorConfig();
        var pillarChangeEntitySelectorConfig =
                new EntitySelectorConfig().withEntityClass(TestdataLeadEntity.class);
        var pillarChangeValueSelectorConfig = new ValueSelectorConfig().withVariableName("value");
        pillarChangeMoveSelectorConfig
                .withPillarSelectorConfig(new PillarSelectorConfig().withEntitySelectorConfig(pillarChangeEntitySelectorConfig))
                .withValueSelectorConfig(pillarChangeValueSelectorConfig);
        allMoveSelectionConfigList.add(pillarChangeMoveSelectorConfig);
        // Pilar swap - basic
        allMoveSelectionConfigList.add(new PillarSwapMoveSelectorConfig().withPillarSelectorConfig(
                new PillarSelectorConfig().withEntitySelectorConfig(pillarChangeEntitySelectorConfig)));
        // Union of all moves
        allMoveSelectionConfigList.add(new UnionMoveSelectorConfig(List.copyOf(allMoveSelectionConfigList)));
        return allMoveSelectionConfigList;
    }

    @Test
    void solveMixedModel() {
        // Same size for both list and basic variables
        executeSolveMixedModel(2, 2, 2);
        // Bigger list planning values size
        executeSolveMixedModel(2, 3, 2);
        executeSolveMixedModel(2, 2, 3);
        // Bigger basic planning values size
        executeSolveMixedModel(3, 2, 2);
    }

    @Test
    void solveMultiEntityMixedModel() {
        // Same size for both list and basic variables
        executeSolveMultiEntityMixedModel(2, 2, 2);
        // Bigger list planning values size
        executeSolveMultiEntityMixedModel(2, 3, 2);
        executeSolveMultiEntityMixedModel(2, 2, 3);
        // Bigger basic planning values size
        executeSolveMultiEntityMixedModel(3, 2, 2);
    }

    private void executeSolveMixedModel(int entitySize, int valueSize, int otherValueSize) {
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataMixedSolution.class, TestdataMixedEntity.class, TestdataMixedValue.class,
                TestdataMixedOtherValue.class)
                .withPhaseList(Collections.emptyList())
                .withTerminationConfig(new TerminationConfig().withStepCountLimit(16))
                .withEasyScoreCalculatorClass(TestdataMixedEasyScoreCalculator.class);

        var problem = TestdataMixedSolution.generateUninitializedSolution(entitySize, valueSize, otherValueSize);
        var solution = PlannerTestUtils.solve(solverConfig, problem);

        // Check the solution
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getBasicValue() == null || e.getSecondBasicValue() == null))
                .isEmpty();

        var expectedSize = Math.max(entitySize - valueSize, 0L);
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getValueList().isEmpty())
                .count()).isEqualTo(expectedSize);

        // Check custom listener execution
        assertThat(solution.getValueList().stream().allMatch(v -> v.getShadowVariableListenerValue().equals(v.getIndex())))
                .isTrue();

        // Check cascading shadow variable
        assertThat(solution.getValueList().stream().allMatch(v -> v.getCascadingShadowVariableValue().equals(v.getIndex() + 1)))
                .isTrue();

        // Check declarative shadow variable from basic variable - genuine entity
        assertThat(solution.getEntityList().stream()
                .allMatch(v -> v.getDeclarativeShadowVariableValue().equals(v.getBasicValue().getStrength())))
                .isTrue();

        // Check declarative shadow variable from basic variable - shadow entity
        assertThat(solution.getOtherValueList().stream()
                .allMatch(v -> v.getDeclarativeShadowVariableValue().equals(v.getEntityList().size() + 2)))
                .isTrue();

        // Check declarative shadow variable from list variable - shadow entity
        assertThat(
                solution.getValueList().stream().allMatch(v -> v.getDeclarativeShadowVariableValue().equals(v.getIndex() + 2)))
                .isTrue();
    }

    void executeSolveMultiEntityMixedModel(int entitySize, int valueSize, int otherValueSize) {
        // Solver Config
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataMixedMultiEntitySolution.class, TestdataMixedMultiEntityFirstEntity.class,
                TestdataMixedMultiEntitySecondEntity.class)
                .withPhaseList(Collections.emptyList())
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(40L))
                .withEasyScoreCalculatorClass(TestdataMixedEntityEasyScoreCalculator.class);

        var problem = TestdataMixedMultiEntitySolution.generateUninitializedSolution(entitySize, valueSize, otherValueSize);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        var expectedSize = Math.max(entitySize - valueSize, 0L);
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getValueList().isEmpty())
                .count()).isEqualTo(expectedSize);
        assertThat(solution.getOtherEntityList().stream()
                .filter(e -> e.getBasicValue() == null || e.getSecondBasicValue() == null))
                .isEmpty();
        for (var entity : solution.getOtherEntityList()) {
            // The strength comparator will cause the basic variables to differ during the CH phase,
            // and LS will find no improvement
            assertThat(entity.getBasicValue()).isNotSameAs(entity.getSecondBasicValue());
        }
    }

    @Test
    void solveMixedModelCustomMove() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataMixedSolution.class, TestdataMixedEntity.class, TestdataMixedValue.class,
                TestdataMixedOtherValue.class)
                .withPhases(new ConstructionHeuristicPhaseConfig().withEntityPlacerConfig(new QueuedEntityPlacerConfig()),
                        new ConstructionHeuristicPhaseConfig().withEntityPlacerConfig(new QueuedValuePlacerConfig()
                                .withValueSelectorConfig(new ValueSelectorConfig().withVariableName("valueList"))),
                        new LocalSearchPhaseConfig()
                                .withMoveSelectorConfig(new MoveIteratorFactoryConfig()
                                        .withMoveIteratorFactoryClass(MixedCustomMoveIteratorFactory.class))
                                .withTerminationConfig(new TerminationConfig().withStepCountLimit(16)))
                .withEasyScoreCalculatorClass(TestdataMixedEasyScoreCalculator.class);

        var problem = TestdataMixedSolution.generateUninitializedSolution(2, 2, 2);
        var solution = PlannerTestUtils.solve(solverConfig, problem);

        // Check the solution
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getBasicValue() == null || e.getSecondBasicValue() == null || e.getValueList().isEmpty()))
                .isEmpty();
    }

    @Test
    void solveMixedModelCustomPhase() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataMixedSolution.class, TestdataMixedEntity.class, TestdataMixedValue.class,
                TestdataMixedOtherValue.class)
                .withPhases(new ConstructionHeuristicPhaseConfig().withEntityPlacerConfig(new QueuedEntityPlacerConfig()),
                        new ConstructionHeuristicPhaseConfig().withEntityPlacerConfig(new QueuedValuePlacerConfig()
                                .withValueSelectorConfig(new ValueSelectorConfig().withVariableName("valueList"))),
                        new CustomPhaseConfig()
                                .withCustomPhaseCommands(new MixedCustomPhaseCommand())
                                .withTerminationConfig(new TerminationConfig().withStepCountLimit(16)))
                .withEasyScoreCalculatorClass(TestdataMixedEasyScoreCalculator.class);

        var problem = TestdataMixedSolution.generateUninitializedSolution(2, 2, 2);
        var solution = PlannerTestUtils.solve(solverConfig, problem);

        // Check the solution
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getBasicValue() == null || e.getSecondBasicValue() == null || e.getValueList().isEmpty()))
                .isEmpty();
    }

    private static List<Pair<EntitySorterManner, ValueSorterManner>> getSortMannerList() {
        var sortMannerList = new ArrayList<Pair<EntitySorterManner, ValueSorterManner>>();
        for (var valueSortManner : ValueSorterManner.values()) {
            sortMannerList.add(new Pair<>(DECREASING_DIFFICULTY, valueSortManner));
            sortMannerList.add(new Pair<>(DECREASING_DIFFICULTY_IF_AVAILABLE, valueSortManner));
        }
        return sortMannerList;
    }

    @ParameterizedTest
    @MethodSource("getSortMannerList")
    void solveMixedModelWithSortManner(Pair<EntitySorterManner, ValueSorterManner> sorterManner) {
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataMixedSolution.class, TestdataMixedEntity.class, TestdataMixedValue.class,
                TestdataMixedOtherValue.class)
                .withPhases(new ConstructionHeuristicPhaseConfig().withEntityPlacerConfig(new QueuedEntityPlacerConfig()),
                        new ConstructionHeuristicPhaseConfig().withEntityPlacerConfig(new QueuedValuePlacerConfig()
                                .withValueSelectorConfig(new ValueSelectorConfig().withVariableName("valueList"))),
                        new LocalSearchPhaseConfig()
                                .withMoveSelectorConfig(
                                        new ChangeMoveSelectorConfig()
                                                .withEntitySelectorConfig(new EntitySelectorConfig()
                                                        .withCacheType(SelectionCacheType.PHASE)
                                                        .withSelectionOrder(SelectionOrder.SORTED)
                                                        .withSorterManner(sorterManner.key()))
                                                .withValueSelectorConfig(
                                                        new ValueSelectorConfig()
                                                                .withVariableName("basicValue")
                                                                .withCacheType(SelectionCacheType.PHASE)
                                                                .withSelectionOrder(SelectionOrder.SORTED)
                                                                .withSorterManner(sorterManner.value())))
                                .withTerminationConfig(new TerminationConfig().withStepCountLimit(16)))
                .withEasyScoreCalculatorClass(TestdataMixedEasyScoreCalculator.class);

        var problem = TestdataMixedSolution.generateUninitializedSolution(2, 2, 2);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getBasicValue() == null || e.getSecondBasicValue() == null || e.getValueList().isEmpty()))
                .isEmpty();
    }

    @Test
    void solveWithMultipleSorterManners() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataComparatorSortableSolution.class, TestdataComparatorSortableEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig().withEntityPlacerConfig(new QueuedEntityPlacerConfig()),
                        new ConstructionHeuristicPhaseConfig().withEntityPlacerConfig(new QueuedValuePlacerConfig()
                                .withValueSelectorConfig(new ValueSelectorConfig().withVariableName("value"))),
                        new LocalSearchPhaseConfig()
                                .withMoveSelectorConfig(
                                        new UnionMoveSelectorConfig()
                                                .withMoveSelectors(
                                                        // Two moves, one asc and another desc
                                                        new ChangeMoveSelectorConfig()
                                                                .withEntitySelectorConfig(new EntitySelectorConfig())
                                                                .withValueSelectorConfig(
                                                                        new ValueSelectorConfig()
                                                                                .withVariableName("value")
                                                                                .withCacheType(SelectionCacheType.PHASE)
                                                                                .withSelectionOrder(SelectionOrder.SORTED)
                                                                                .withSorterManner(
                                                                                        ValueSorterManner.ASCENDING)),
                                                        new ChangeMoveSelectorConfig()
                                                                .withEntitySelectorConfig(new EntitySelectorConfig())
                                                                .withValueSelectorConfig(
                                                                        new ValueSelectorConfig()
                                                                                .withVariableName("value")
                                                                                .withCacheType(SelectionCacheType.PHASE)
                                                                                .withSelectionOrder(SelectionOrder.SORTED)
                                                                                .withSorterManner(
                                                                                        ValueSorterManner.DESCENDING))))
                                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(10L)))
                .withEasyScoreCalculatorClass(OneValuePerEntityComparatorEasyScoreCalculator.class);

        var problem = TestdataComparatorSortableSolution.generateSolution(5, 5, true);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        assertThat(solution).isNotNull();
    }

    @Test
    void solvePinnedMixedModel() {
        // We don't enable the LS because we want to ensure the pinned entity remains uninitialized
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataMixedSolution.class, TestdataMixedEntity.class, TestdataMixedValue.class,
                TestdataMixedOtherValue.class)
                .withPhases(new ConstructionHeuristicPhaseConfig().withEntityPlacerConfig(new QueuedEntityPlacerConfig()),
                        new ConstructionHeuristicPhaseConfig().withEntityPlacerConfig(new QueuedValuePlacerConfig()
                                .withValueSelectorConfig(new ValueSelectorConfig().withVariableName("valueList"))))
                .withEasyScoreCalculatorClass(TestdataMixedEasyScoreCalculator.class);

        var problem = TestdataMixedSolution.generateUninitializedSolution(3, 2, 2);
        // Pin the first entity
        problem.getEntityList().get(0).setPinned(true);
        problem.getEntityList().get(0).setPinnedIndex(0);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        // The first entity should remain unchanged
        assertThat(solution.getEntityList().get(0).getBasicValue()).isNull();
        assertThat(solution.getEntityList().get(0).getSecondBasicValue()).isNull();
        assertThat(solution.getEntityList().get(0).getValueList()).isEmpty();
    }

    @Test
    void solveUnassignedMixedModel() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataUnassignedMixedSolution.class, TestdataUnassignedMixedEntity.class)
                .withPhaseList(Collections.emptyList())
                .withTerminationConfig(new TerminationConfig().withStepCountLimit(16))
                .withEasyScoreCalculatorClass(TestdataUnassignedMixedEasyScoreCalculator.class);

        var problem = TestdataUnassignedMixedSolution.generateUninitializedSolution(2, 2, 2);
        // Block values and make the basic and list variables unassigned
        problem.getValueList().get(0).setBlocked(true);
        problem.getValueList().get(1).setBlocked(true);
        problem.getOtherValueList().get(0).setBlocked(true);
        problem.getOtherValueList().get(1).setBlocked(true);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getBasicValue() == null))
                .hasSize(2);
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getSecondBasicValue() != null))
                .hasSize(2);
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getValueList().isEmpty()))
                .hasSize(2);
    }

    @Test
    void solvePinnedAndUnassignedMixedModel() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataUnassignedMixedSolution.class, TestdataUnassignedMixedEntity.class)
                .withPhaseList(Collections.emptyList())
                .withTerminationConfig(new TerminationConfig().withStepCountLimit(16))
                .withEasyScoreCalculatorClass(TestdataUnassignedMixedEasyScoreCalculator.class);

        // Pin the entire first entity
        var problem = TestdataUnassignedMixedSolution.generateUninitializedSolution(2, 2, 2);
        problem.getEntityList().get(0).setPinned(true);
        problem.getEntityList().get(0).setBasicValue(problem.getOtherValueList().get(0));
        problem.getEntityList().get(0).setSecondBasicValue(problem.getOtherValueList().get(0));
        problem.getEntityList().get(0).setValueList(List.of(problem.getValueList().get(0)));
        // Block values and make the basic and list variables unassigned
        problem.getValueList().get(0).setBlocked(true);
        problem.getValueList().get(1).setBlocked(true);
        problem.getOtherValueList().get(0).setBlocked(true);
        problem.getOtherValueList().get(1).setBlocked(true);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        // The first entity should remain unchanged
        assertThat(solution.getEntityList().get(0).getBasicValue()).isNotNull();
        assertThat(solution.getEntityList().get(0).getSecondBasicValue()).isNotNull();
        assertThat(solution.getEntityList().get(0).getValueList()).hasSize(1);
        assertThat(solution.getEntityList().get(1).getBasicValue()).isNull();
        assertThat(solution.getEntityList().get(1).getSecondBasicValue()).isNotNull();
        assertThat(solution.getEntityList().get(1).getValueList()).isEmpty();

        // Pin partially the first entity list
        problem = TestdataUnassignedMixedSolution.generateUninitializedSolution(2, 4, 2);
        problem.getEntityList().get(0).setPinnedIndex(2);
        problem.getEntityList().get(0).setValueList(problem.getValueList().subList(1, 3));
        // Block values and make the basic variable unassigned
        problem.getOtherValueList().get(0).setBlocked(true);
        problem.getOtherValueList().get(1).setBlocked(true);
        solution = PlannerTestUtils.solve(solverConfig, problem);
        assertThat(solution.getEntityList().get(0).getBasicValue()).isNull();
        assertThat(solution.getEntityList().get(0).getSecondBasicValue()).isNotNull();
        // The pinning index fixed the values 1 and 2. The only remaining option is values are 0 and 3.
        // The score is bigger when the list size is 3
        assertThat(solution.getEntityList().get(0).getValueList()).hasSize(3);
        assertThat(solution.getEntityList().get(0).getValueList())
                .hasSameElementsAs(
                        List.of(problem.getValueList().get(1), problem.getValueList().get(2), problem.getValueList().get(0)));
        assertThat(solution.getEntityList().get(1).getBasicValue()).isNull();
        assertThat(solution.getEntityList().get(1).getSecondBasicValue()).isNotNull();
        assertThat(solution.getEntityList().get(1).getValueList()).hasSize(1);
        assertThat(solution.getEntityList().get(1).getValueList()).hasSameElementsAs(List.of(problem.getValueList().get(3)));
    }

    @Test
    void solveStaleBuiltinShadows() {
        // Solver config
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataInverseRelationSolution.class, TestdataInverseRelationEntity.class, TestdataInverseRelationValue.class)
                .withEasyScoreCalculatorClass(null)
                .withConstraintProviderClass(TestdataInverseRelationConstraintProvider.class);

        var problem = new TestdataInverseRelationSolution();
        var e1 = new TestdataInverseRelationEntity("e1");
        var e2 = new TestdataInverseRelationEntity("e2");

        var v1 = new TestdataInverseRelationValue("v1");
        var v2 = new TestdataInverseRelationValue("v2");

        e1.setValue(v1);
        e2.setValue(v1);

        problem.setEntityList(List.of(e1, e2));
        problem.setValueList(List.of(v1, v2));

        var solution = PlannerTestUtils.solve(solverConfig, problem);

        assertThat(solution.getEntityList().get(0).getValue().getCode()).isEqualTo("v1");
        assertThat(solution.getEntityList().get(1).getValue().getCode()).isEqualTo("v2");

        assertThat(solution.getScore()).isEqualTo(SimpleScore.of(-2));
    }

    @Test
    void solveStaleDeclarativeShadows() {
        // Solver config
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataConcurrentSolution.class, TestdataConcurrentEntity.class, TestdataConcurrentValue.class)
                .withEasyScoreCalculatorClass(null)
                .withConstraintProviderClass(TestdataConcurrentConstraintProvider.class);

        var e1 = new TestdataConcurrentEntity("e1");
        var e2 = new TestdataConcurrentEntity("e2");

        var a1 = new TestdataConcurrentValue("a1");
        var a2 = new TestdataConcurrentValue("a2");
        var b1 = new TestdataConcurrentValue("b1");
        var b2 = new TestdataConcurrentValue("b2");

        a1.setConcurrentValueGroup(List.of(a1, a2));
        a2.setConcurrentValueGroup(List.of(a1, a2));

        b1.setConcurrentValueGroup(List.of(b1, b2));
        b2.setConcurrentValueGroup(List.of(b1, b2));

        e1.setValues(List.of(a1, b1));
        e2.setValues(List.of(b2, a2));

        var entities = List.of(e1, e2);
        var values = List.of(a1, a2, b1, b2);

        var problem = new TestdataConcurrentSolution();

        problem.setEntities(entities);
        problem.setValues(values);

        var solution = PlannerTestUtils.solve(solverConfig, problem);

        assertThat(solution.getEntities().get(0).getValues()).map(TestdataConcurrentValue::getId).containsExactly("a1", "b1");
        assertThat(solution.getEntities().get(1).getValues()).map(TestdataConcurrentValue::getId).containsExactly("a2", "b2");

        assertThat(solution.getScore()).isEqualTo(HardSoftScore.of(0, -240));
    }

    private static List<MoveSelectorConfig> generateMovesForMixedModel() {
        // Local Search
        var allMoveSelectionConfigList = new ArrayList<MoveSelectorConfig>();
        // Change - basic
        allMoveSelectionConfigList.add(new ChangeMoveSelectorConfig());
        // Swap - basic
        allMoveSelectionConfigList.add(new SwapMoveSelectorConfig());
        // Pillar change - basic
        var pillarChangeMoveSelectorConfig = new PillarChangeMoveSelectorConfig();
        var pillarChangeEntitySelectorConfig =
                new EntitySelectorConfig().withEntityClass(TestdataMixedEntity.class);
        var pillarChangeValueSelectorConfig = new ValueSelectorConfig().withVariableName("basicValue");
        pillarChangeMoveSelectorConfig
                .withPillarSelectorConfig(new PillarSelectorConfig().withEntitySelectorConfig(pillarChangeEntitySelectorConfig))
                .withValueSelectorConfig(pillarChangeValueSelectorConfig);
        allMoveSelectionConfigList.add(pillarChangeMoveSelectorConfig);
        // Pilar swap - basic
        allMoveSelectionConfigList.add(new PillarSwapMoveSelectorConfig().withPillarSelectorConfig(
                new PillarSelectorConfig().withEntitySelectorConfig(pillarChangeEntitySelectorConfig)));
        // R&R - basic
        allMoveSelectionConfigList.add(new RuinRecreateMoveSelectorConfig().withVariableName("basicValue"));
        // Change - list
        allMoveSelectionConfigList.add(new ListChangeMoveSelectorConfig());
        // Swap - list
        allMoveSelectionConfigList.add(new ListSwapMoveSelectorConfig());
        // Sublist change - list
        allMoveSelectionConfigList.add(new SubListChangeMoveSelectorConfig());
        // Sublist swap - list
        allMoveSelectionConfigList.add(new SubListSwapMoveSelectorConfig().withSubListSelectorConfig(
                new SubListSelectorConfig().withValueSelectorConfig(new ValueSelectorConfig().withVariableName("valueList"))));
        // KOpt - list
        allMoveSelectionConfigList.add(new KOptListMoveSelectorConfig());
        // R&R - list
        allMoveSelectionConfigList.add(new ListRuinRecreateMoveSelectorConfig());
        // Union of all moves
        allMoveSelectionConfigList.add(new UnionMoveSelectorConfig(List.copyOf(allMoveSelectionConfigList)));
        return allMoveSelectionConfigList;
    }

    @ParameterizedTest
    @MethodSource("generateMovesForMixedModel")
    void solveMoveConfigMixedModel(MoveSelectorConfig moveSelectionConfig) {
        // Local search
        var localSearchConfig =
                new LocalSearchPhaseConfig()
                        .withMoveSelectorConfig(moveSelectionConfig)
                        .withTerminationConfig(new TerminationConfig().withMoveCountLimit(40L));
        // Solver config
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataMixedSolution.class, TestdataMixedEntity.class, TestdataMixedValue.class,
                TestdataMixedOtherValue.class)
                .withPhases(new ConstructionHeuristicPhaseConfig().withEntityPlacerConfig(new QueuedEntityPlacerConfig()),
                        new ConstructionHeuristicPhaseConfig().withEntityPlacerConfig(new QueuedValuePlacerConfig()
                                .withValueSelectorConfig(new ValueSelectorConfig().withVariableName("valueList"))),
                        localSearchConfig)
                .withEasyScoreCalculatorClass(TestdataMixedEasyScoreCalculator.class);

        var problem = TestdataMixedSolution.generateUninitializedSolution(2, 2, 2);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getBasicValue() == null || e.getSecondBasicValue() == null || e.getValueList().isEmpty()))
                .isEmpty();
    }

    private static List<MoveSelectorConfig> generateMovesForMultiEntityMixedModel() {
        // Local Search
        var allMoveSelectionConfigList = new ArrayList<MoveSelectorConfig>();
        // Change - basic
        allMoveSelectionConfigList.add(new ChangeMoveSelectorConfig());
        // Swap - basic
        allMoveSelectionConfigList.add(new SwapMoveSelectorConfig());
        // Pillar change - basic
        var pillarChangeMoveSelectorConfig = new PillarChangeMoveSelectorConfig();
        var pillarChangeEntitySelectorConfig =
                new EntitySelectorConfig().withEntityClass(TestdataMixedMultiEntitySecondEntity.class);
        var pillarChangeValueSelectorConfig = new ValueSelectorConfig().withVariableName("basicValue");
        pillarChangeMoveSelectorConfig
                .withPillarSelectorConfig(new PillarSelectorConfig().withEntitySelectorConfig(pillarChangeEntitySelectorConfig))
                .withValueSelectorConfig(pillarChangeValueSelectorConfig);
        allMoveSelectionConfigList.add(pillarChangeMoveSelectorConfig);
        // Pilar swap - basic
        allMoveSelectionConfigList.add(new PillarSwapMoveSelectorConfig().withPillarSelectorConfig(
                new PillarSelectorConfig().withEntitySelectorConfig(pillarChangeEntitySelectorConfig)));
        // R&R - basic
        allMoveSelectionConfigList.add(new RuinRecreateMoveSelectorConfig()
                .withEntitySelectorConfig(
                        new EntitySelectorConfig().withEntityClass(TestdataMixedMultiEntitySecondEntity.class))
                .withVariableName("basicValue"));
        // Change - list
        allMoveSelectionConfigList.add(new ListChangeMoveSelectorConfig());
        // Swap - list
        allMoveSelectionConfigList.add(new ListSwapMoveSelectorConfig());
        // Sublist change - list
        allMoveSelectionConfigList.add(new SubListChangeMoveSelectorConfig());
        // Sublist swap - list
        allMoveSelectionConfigList.add(new SubListSwapMoveSelectorConfig());
        // KOpt - list
        allMoveSelectionConfigList.add(new KOptListMoveSelectorConfig());
        // R&R - list
        allMoveSelectionConfigList.add(new ListRuinRecreateMoveSelectorConfig());
        // Union of all moves
        allMoveSelectionConfigList.add(new UnionMoveSelectorConfig(List.copyOf(allMoveSelectionConfigList)));
        return allMoveSelectionConfigList;
    }

    @ParameterizedTest
    @MethodSource("generateMovesForMultiEntityMixedModel")
    void solveMultiEntityMoveConfigMixedModel(MoveSelectorConfig moveSelectionConfig) {
        // Construction Heuristic
        var constructionHeuristicValuePlacer =
                new ConstructionHeuristicPhaseConfig().withEntityPlacerConfig(new QueuedValuePlacerConfig()
                        .withEntityClass(TestdataMixedMultiEntityFirstEntity.class)
                        .withValueSelectorConfig(new ValueSelectorConfig().withVariableName("valueList")));
        var constructionHeuristicEntityPlacer =
                new ConstructionHeuristicPhaseConfig().withEntityPlacerConfig(new QueuedEntityPlacerConfig());
        // Local search
        var localSearchConfig =
                new LocalSearchPhaseConfig()
                        .withMoveSelectorConfig(moveSelectionConfig)
                        .withTerminationConfig(new TerminationConfig().withMoveCountLimit(40L));
        // Solver Config
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataMixedMultiEntitySolution.class, TestdataMixedMultiEntityFirstEntity.class,
                TestdataMixedMultiEntitySecondEntity.class)
                .withPhases(constructionHeuristicValuePlacer, constructionHeuristicEntityPlacer, localSearchConfig)
                .withEasyScoreCalculatorClass(TestdataMixedEntityEasyScoreCalculator.class);

        var problem = TestdataMixedMultiEntitySolution.generateUninitializedSolution(3, 2, 2);
        var solution = PlannerTestUtils.solve(solverConfig, problem);
        // Three planning entities and two planning values. One entity will remain unscheduled.
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getValueList().isEmpty())
                .count()).isEqualTo(1);
        assertThat(solution.getOtherEntityList().stream()
                .filter(e -> e.getBasicValue() == null || e.getSecondBasicValue() == null))
                .isEmpty();
        for (var entity : solution.getOtherEntityList()) {
            // The strength comparator will cause the basic variables to differ during the CH phase,
            // and LS will find no improvement
            assertThat(entity.getBasicValue()).isNotSameAs(entity.getSecondBasicValue());
        }
    }

    private static List<MoveSelectorConfig> generateMovesForBasicVar() {
        var allMoveSelectionConfigList = new ArrayList<MoveSelectorConfig>();
        // Shared entity selector config
        var entitySelectorConfig = new EntitySelectorConfig()
                .withEntityClass(TestdataAllowsUnassignedEntityProvidingEntity.class);
        // Change - basic
        allMoveSelectionConfigList.add(new ChangeMoveSelectorConfig().withEntitySelectorConfig(entitySelectorConfig));
        // Swap - basic
        allMoveSelectionConfigList.add(new SwapMoveSelectorConfig().withEntitySelectorConfig(entitySelectorConfig));
        // Swap - basic - original sort order
        allMoveSelectionConfigList.add(new SwapMoveSelectorConfig()
                .withSelectionOrder(SelectionOrder.ORIGINAL)
                .withEntitySelectorConfig(entitySelectorConfig));
        // Pillar change - basic
        var pillarChangeMoveSelectorConfig = new PillarChangeMoveSelectorConfig();
        var pillarChangeValueSelectorConfig = new ValueSelectorConfig().withVariableName("value");
        pillarChangeMoveSelectorConfig
                .withPillarSelectorConfig(new PillarSelectorConfig().withEntitySelectorConfig(entitySelectorConfig))
                .withValueSelectorConfig(pillarChangeValueSelectorConfig);
        allMoveSelectionConfigList.add(pillarChangeMoveSelectorConfig);
        // Pilar swap - basic
        allMoveSelectionConfigList.add(new PillarSwapMoveSelectorConfig().withPillarSelectorConfig(
                new PillarSelectorConfig().withEntitySelectorConfig(entitySelectorConfig)));
        // R&R - basic
        allMoveSelectionConfigList.add(new RuinRecreateMoveSelectorConfig().withEntitySelectorConfig(entitySelectorConfig));
        // Union of all moves
        allMoveSelectionConfigList.add(new UnionMoveSelectorConfig(List.copyOf(allMoveSelectionConfigList)));
        return allMoveSelectionConfigList;
    }

    @ParameterizedTest
    @MethodSource("generateMovesForBasicVar")
    void solveBasicVarEntityRangeModelSingleLocalSearch(MoveSelectorConfig moveSelectionConfig) {
        solveBasicVarEntityRangeModel(moveSelectionConfig, false);
    }

    @ParameterizedTest
    @MethodSource("generateMovesForBasicVar")
    void solveBasicVarEntityRangeModelMultipleLocalSearch(MoveSelectorConfig moveSelectionConfig) {
        solveBasicVarEntityRangeModel(moveSelectionConfig, true);
    }

    private void solveBasicVarEntityRangeModel(MoveSelectorConfig moveSelectionConfig, boolean multipleLocalSearch) {
        // Local search
        var localSearchConfig = new LocalSearchPhaseConfig()
                .withMoveSelectorConfig(moveSelectionConfig)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(1000L));

        // Local search 2
        var localSearchConfig2 = new LocalSearchPhaseConfig()
                .withMoveSelectorConfig(moveSelectionConfig)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(1000L));

        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataAllowsUnassignedEntityProvidingSolution.class,
                        TestdataAllowsUnassignedEntityProvidingEntity.class)
                .withEasyScoreCalculatorClass(TestdataAllowsUnassignedEntityProvidingScoreCalculator.class)
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withPhases(new ConstructionHeuristicPhaseConfig(), localSearchConfig);

        if (multipleLocalSearch) {
            solverConfig.withPhases(new ConstructionHeuristicPhaseConfig(), localSearchConfig, localSearchConfig2);
        }

        var value1 = new TestdataValue("v1");
        var value2 = new TestdataValue("v2");
        var value3 = new TestdataValue("v3");
        var value4 = new TestdataValue("v4");
        var value5 = new TestdataValue("v5");
        var entity1 = new TestdataAllowsUnassignedEntityProvidingEntity("e1", List.of(value1, value2, value3));
        var entity2 = new TestdataAllowsUnassignedEntityProvidingEntity("e2", List.of(value1, value2, value5));
        var entity3 = new TestdataAllowsUnassignedEntityProvidingEntity("e3", List.of(value4, value5));

        var solution = new TestdataAllowsUnassignedEntityProvidingSolution();
        solution.setEntityList(List.of(entity1, entity2, entity3));

        var bestSolution = PlannerTestUtils.solve(solverConfig, solution, true);
        assertThat(bestSolution).isNotNull();

        var bestEntity1 = bestSolution.getEntityList().get(0);
        assertThat(bestEntity1.getValue()).isNotIn(value4, value5);
        var bestEntity2 = bestSolution.getEntityList().get(1);
        assertThat(bestEntity2.getValue()).isNotIn(value3, value4);
        var bestEntity3 = bestSolution.getEntityList().get(2);
        assertThat(bestEntity3.getValue()).isNotIn(value1, value2, value3);
    }

    private static List<MoveSelectorConfig> generateMovesForListVarEntityRangeModel() {
        // Local Search
        var allMoveSelectionConfigList = new ArrayList<MoveSelectorConfig>();
        // Shared value selector config
        var valueSelectorConfig = new ValueSelectorConfig()
                .withVariableName("valueList");
        // Change - list
        allMoveSelectionConfigList.add(new ListChangeMoveSelectorConfig().withValueSelectorConfig(valueSelectorConfig));
        // Change - list - original sort order
        allMoveSelectionConfigList.add(
                new ListChangeMoveSelectorConfig().withValueSelectorConfig(valueSelectorConfig)
                        .withSelectionOrder(SelectionOrder.ORIGINAL)
                        .withDestinationSelectorConfig(new DestinationSelectorConfig()
                                .withEntitySelectorConfig(
                                        new EntitySelectorConfig().withSelectionOrder(SelectionOrder.ORIGINAL))
                                .withValueSelectorConfig(
                                        new ValueSelectorConfig().withSelectionOrder(SelectionOrder.ORIGINAL))));
        // Swap - list
        allMoveSelectionConfigList.add(new ListSwapMoveSelectorConfig().withValueSelectorConfig(valueSelectorConfig));
        // Swap - list - original sort order
        allMoveSelectionConfigList.add(new ListSwapMoveSelectorConfig().withSelectionOrder(SelectionOrder.ORIGINAL)
                .withValueSelectorConfig(valueSelectorConfig.copyConfig().withSelectionOrder(SelectionOrder.ORIGINAL)));
        // Sublist change - list
        allMoveSelectionConfigList.add(new SubListChangeMoveSelectorConfig()
                .withSubListSelectorConfig(new SubListSelectorConfig().withValueSelectorConfig(valueSelectorConfig)));
        // Sublist swap - list
        allMoveSelectionConfigList.add(new SubListSwapMoveSelectorConfig()
                .withSubListSelectorConfig(new SubListSelectorConfig().withValueSelectorConfig(valueSelectorConfig)));
        // KOpt - list
        allMoveSelectionConfigList.add(new KOptListMoveSelectorConfig().withValueSelectorConfig(valueSelectorConfig));
        // R&R - list
        allMoveSelectionConfigList.add(new ListRuinRecreateMoveSelectorConfig());
        // Union of all moves
        allMoveSelectionConfigList.add(new UnionMoveSelectorConfig(List.copyOf(allMoveSelectionConfigList)));
        return allMoveSelectionConfigList;
    }

    @ParameterizedTest
    @MethodSource("generateMovesForListVarEntityRangeModel")
    void solveListVarEntityRangeModelSingleLocalSearch(MoveSelectorConfig moveSelectionConfig) {
        solveListVarEntityRangeModel(moveSelectionConfig, false);
    }

    @ParameterizedTest
    @MethodSource("generateMovesForListVarEntityRangeModel")
    void solveListVarEntityRangeModelMultipleLocalSearch(MoveSelectorConfig moveSelectionConfig) {
        solveListVarEntityRangeModel(moveSelectionConfig, true);
    }

    private void solveListVarEntityRangeModel(MoveSelectorConfig moveSelectionConfig, boolean multipleLocalSearch) {
        // Local search
        var localSearchConfig = new LocalSearchPhaseConfig()
                .withMoveSelectorConfig(moveSelectionConfig)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(1000L));

        // Local search 2
        var localSearchConfig2 = new LocalSearchPhaseConfig()
                .withMoveSelectorConfig(moveSelectionConfig)
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(1000L));

        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataListUnassignedEntityProvidingSolution.class,
                        TestdataListUnassignedEntityProvidingEntity.class)
                .withEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT)
                .withEasyScoreCalculatorClass(TestdataListUnassignedEntityProvidingScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig(), localSearchConfig);

        if (multipleLocalSearch) {
            solverConfig.withPhases(new ConstructionHeuristicPhaseConfig(), localSearchConfig, localSearchConfig2);
        }

        var value1 = new TestdataValue("v1");
        var value2 = new TestdataValue("v2");
        var value3 = new TestdataValue("v3");
        var value4 = new TestdataValue("v4");
        var value5 = new TestdataValue("v5");
        var entity1 = new TestdataListUnassignedEntityProvidingEntity("e1", List.of(value1, value2));
        var entity2 = new TestdataListUnassignedEntityProvidingEntity("e2", List.of(value2, value3));
        var entity3 = new TestdataListUnassignedEntityProvidingEntity("e3", List.of(value4, value5));

        var solution = new TestdataListUnassignedEntityProvidingSolution();
        solution.setEntityList(List.of(entity1, entity2, entity3));

        var bestSolution = PlannerTestUtils.solve(solverConfig, solution, true);
        assertThat(bestSolution).isNotNull();

        var bestEntity1 = bestSolution.getEntityList().get(0);
        assertThat(bestEntity1.getValueList()).hasSizeGreaterThan(0);
        assertThat(bestEntity1.getValueList()).doesNotContain(value3, value4, value5);
        var bestEntity2 = bestSolution.getEntityList().get(1);
        assertThat(bestEntity2.getValueList()).hasSizeGreaterThan(0);
        assertThat(bestEntity2.getValueList()).doesNotContain(value1, value4, value5);
        var bestEntity3 = bestSolution.getEntityList().get(2);
        assertThat(bestEntity3.getValueList()).hasSizeGreaterThan(0);
        assertThat(bestEntity3.getValueList()).doesNotContain(value1, value2, value3);
    }

    @Test
    void failRuinRecreateWithMultiVar() {
        // Solver config
        var localSearchConfig = new LocalSearchPhaseConfig().withMoveSelectorConfig(new RuinRecreateMoveSelectorConfig());
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataMixedSolution.class, TestdataMixedEntity.class, TestdataMixedValue.class,
                TestdataMixedOtherValue.class)
                .withPhases(localSearchConfig)
                .withEasyScoreCalculatorClass(TestdataMixedEasyScoreCalculator.class);
        var problem = TestdataMixedSolution.generateUninitializedSolution(2, 2, 2);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining("The entity class")
                .hasMessageContaining("TestdataMixedEntity")
                .hasMessageContaining("contains several variables")
                .hasMessageContaining("it cannot be deduced automatically.")
                .hasMessageContaining("Maybe set the property variableName");
    }

    @Test
    void failRuinRecreateWithBadVar() {
        // Solver config
        var moveSelectionConfig = new RuinRecreateMoveSelectorConfig()
                .withVariableName("badVariable");
        var localSearchConfig = new LocalSearchPhaseConfig().withMoveSelectorConfig(moveSelectionConfig);
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataMixedSolution.class, TestdataMixedEntity.class, TestdataMixedValue.class,
                TestdataMixedOtherValue.class)
                .withPhases(localSearchConfig)
                .withEasyScoreCalculatorClass(TestdataMixedEasyScoreCalculator.class);
        var problem = TestdataMixedSolution.generateUninitializedSolution(2, 2, 2);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining("The entity class")
                .hasMessageContaining("TestdataMixedEntity")
                .hasMessageContaining("has no variable named badVariable");
    }

    @Test
    void failRuinRecreateWithMultiEntityMultiVar() {
        // Solver config
        var localSearchConfig = new LocalSearchPhaseConfig().withMoveSelectorConfig(new RuinRecreateMoveSelectorConfig());
        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataMixedMultiEntitySolution.class, TestdataMixedMultiEntityFirstEntity.class,
                        TestdataMixedMultiEntitySecondEntity.class)
                .withPhases(localSearchConfig)
                .withEasyScoreCalculatorClass(TestdataMixedEntityEasyScoreCalculator.class);
        var problem = TestdataMixedMultiEntitySolution.generateUninitializedSolution(2, 2, 2);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining("has no entityClass configured and because there are multiple in the entityClassSet")
                .hasMessageContaining("it cannot be deduced automatically");
    }

    @Test
    void failBasicVariableInvalidValueRange() {
        // Solver config
        var solverConfig =
                PlannerTestUtils.buildSolverConfig(TestdataEntityProvidingSolution.class, TestdataEntityProvidingEntity.class)
                        .withEasyScoreCalculatorClass(DummySimpleScoreEasyScoreCalculator.class);

        var problem = new TestdataEntityProvidingSolution();
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        var v3 = new TestdataValue("3");
        // The entity has an assigned value v3 that is not included in the entity value ranges
        var e1 = new TestdataEntityProvidingEntity("e1", List.of(v1, v2), v3);
        var e2 = new TestdataEntityProvidingEntity("e2", List.of(v1, v2), v1);
        problem.setEntityList(new ArrayList<>(Arrays.asList(e1, e2)));

        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining(
                        "The value (3) from the planning variable (value) has been assigned to the entity (e1), but it is outside of the related value range [1-2]");
    }

    @Test
    void failMultipleBasicVariableInvalidValueRange() {
        // Solver config
        var solverConfig =
                PlannerTestUtils
                        .buildSolverConfig(TestdataAllowsUnassignedMultiVarEntityProvidingSolution.class,
                                TestdataAllowsUnassignedMultiVarEntityProvidingEntity.class)
                        .withEasyScoreCalculatorClass(DummySimpleScoreEasyScoreCalculator.class);

        var problem = new TestdataAllowsUnassignedMultiVarEntityProvidingSolution();
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        var v3 = new TestdataValue("3");
        // The entity has been assigned a value v3 for the second value range,
        // which is not included in the entity's value ranges
        var e1 = new TestdataAllowsUnassignedMultiVarEntityProvidingEntity("e1", List.of(v1, v2, v3), v3, List.of(v1, v2), v3,
                v3);
        var e2 = new TestdataAllowsUnassignedMultiVarEntityProvidingEntity("e2", List.of(v1, v2, v3), v1, List.of(v1, v2, v3),
                v1, v1);
        problem.setEntityList(new ArrayList<>(Arrays.asList(e1, e2)));

        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining(
                        "The value (3) from the planning variable (secondValue) has been assigned to the entity (e1), but it is outside of the related value range [null]∪[1-2]");
    }

    @Test
    void failListVariableInvalidValueRange() {
        // Solver config
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataListEntityProvidingSolution.class, TestdataListEntityProvidingEntity.class,
                TestdataListEntityProvidingValue.class)
                .withEasyScoreCalculatorClass(DummySimpleScoreEasyScoreCalculator.class);

        var problem = new TestdataListEntityProvidingSolution();
        var v1 = new TestdataListEntityProvidingValue("1");
        var v2 = new TestdataListEntityProvidingValue("2");
        var v3 = new TestdataListEntityProvidingValue("3");
        // The entity has an assigned value v3 that is not included in the entity value ranges
        var e1 = new TestdataListEntityProvidingEntity("e1", List.of(v1, v2), List.of(v3));
        var e2 = new TestdataListEntityProvidingEntity("e2", List.of(v1, v2), List.of(v1));
        problem.setEntityList(new ArrayList<>(Arrays.asList(e1, e2)));

        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining(
                        "The value (3) from the planning variable (valueList) has been assigned to the entity (e1), but it is outside of the related value range [1-2]");
    }

    @Test
    void failMixedModelInvalidValueRange() {
        // Solver config
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataMixedMultiEntitySolution.class, TestdataMixedMultiEntityFirstEntity.class,
                TestdataMixedMultiEntitySecondEntity.class)
                .withEasyScoreCalculatorClass(DummySimpleScoreEasyScoreCalculator.class);

        var problem = new TestdataMixedMultiEntitySolution();
        var v1a = new TestdataMixedMultiEntityFirstValue("1");
        var v2a = new TestdataMixedMultiEntityFirstValue("2");
        var v3a = new TestdataMixedMultiEntityFirstValue("3");
        var v1b = new TestdataMixedMultiEntitySecondValue("1", 1);
        var v2b = new TestdataMixedMultiEntitySecondValue("2", 1);
        var v3b = new TestdataMixedMultiEntitySecondValue("3", 1);

        // 1 - Invalid basic variable
        var e1a = new TestdataMixedMultiEntityFirstEntity("e1", 1);
        var e1b = new TestdataMixedMultiEntitySecondEntity("e1");
        e1b.setBasicValue(v1b);
        var e2b = new TestdataMixedMultiEntitySecondEntity("e2");
        // Invalid assigned value
        problem.setEntityList(List.of(e1a));
        problem.setOtherEntityList(List.of(e1b, e2b));
        problem.setValueList(List.of(v1a, v2a, v3a));
        problem.setOtherValueList(List.of(v2b, v3b));

        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining(
                        "The value (1) from the planning variable (basicValue) has been assigned to the entity (e1), but it is outside of the related value range [2-3]");
        e1b.setBasicValue(null);

        // 2 - Invalid list variable
        // Invalid assigned value
        e1a.getValueList().add(v1a);
        problem.setEntityList(List.of(e1a));
        problem.setOtherEntityList(List.of(e1b, e2b));
        problem.setValueList(List.of(v2a, v3a));
        problem.setOtherValueList(List.of(v2b, v3b));

        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining(
                        "The value (1) from the planning variable (valueList) has been assigned to the entity (e1), but it is outside of the related value range [2-3]");
    }

    @Test
    void failLocalSearchValueRangeAssertion() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataListSolution.class, TestdataListEntity.class,
                TestdataListValue.class);
        solverConfig.setEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        var localSearchPhaseConfig = new LocalSearchPhaseConfig();
        localSearchPhaseConfig.setMoveSelectorConfig(
                new MoveIteratorFactoryConfig().withMoveIteratorFactoryClass(InvalidMoveListFactory.class));
        solverConfig.setPhaseConfigList(List.of(new ConstructionHeuristicPhaseConfig(), localSearchPhaseConfig));

        var problem = TestdataListSolution.generateUninitializedSolution(2, 2);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining(
                        "The value (bad value) from the planning variable (valueList) has been assigned to the entity (Generated Entity 0), but it is outside of the related value range [Generated Value 0-Generated Value 1]");
    }

    @Test
    void failCustomPhaseValueRangeAssertion() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataListSolution.class, TestdataListEntity.class,
                TestdataListValue.class);
        solverConfig.setEnvironmentMode(EnvironmentMode.STEP_ASSERT);
        var customPhaseConfig = new CustomPhaseConfig()
                .withCustomPhaseCommands(new InvalidCustomPhaseCommand());
        solverConfig.setPhaseConfigList(List.of(new ConstructionHeuristicPhaseConfig(), customPhaseConfig));

        var problem = TestdataListSolution.generateUninitializedSolution(2, 2);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining(
                        "The value (bad value) from the planning variable (valueList) has been assigned to the entity (Generated Entity 0), but it is outside of the related value range [Generated Value 0-Generated Value 1]");
    }

    public static final class MinimizeUnusedEntitiesEasyScoreCalculator
            implements EasyScoreCalculator<Object, SimpleScore> {

        @Override
        public @NonNull SimpleScore calculateScore(@NonNull Object solution) {
            return new MaximizeUnusedEntitiesEasyScoreCalculator().calculateScore(solution).negate();
        }
    }

    public static final class MinimizeUnassignedEntitiesEasyScoreCalculator
            implements EasyScoreCalculator<TestdataAllowsUnassignedValuesListSolution, SimpleScore> {

        @Override
        public @NonNull SimpleScore calculateScore(@NonNull TestdataAllowsUnassignedValuesListSolution solution) {
            var i = 0;
            for (var entity : solution.getEntityList()) {
                i += entity.getValueList().size();
            }
            return SimpleScore.of(i);
        }
    }

    public static final class MaximizeUnusedEntitiesEasyScoreCalculator
            implements EasyScoreCalculator<Object, SimpleScore> {

        @Override
        public @NonNull SimpleScore calculateScore(@NonNull Object solution) {
            if (solution instanceof TestdataPinnedListSolution testdataPinnedListSolution) {
                var unusedEntities = 0;
                for (var entity : testdataPinnedListSolution.getEntityList()) {
                    if (entity.getValueList().isEmpty()) {
                        unusedEntities++;
                    }
                }
                return SimpleScore.of(unusedEntities);
            } else if (solution instanceof TestdataPinnedWithIndexListSolution testdataPinnedWithIndexListSolution) {
                var unusedEntities = 0;
                for (var entity : testdataPinnedWithIndexListSolution.getEntityList()) {
                    if (entity.getValueList().isEmpty()) {
                        unusedEntities++;
                    }
                }
                return SimpleScore.of(unusedEntities);
            } else if (solution instanceof TestdataAllowsUnassignedValuesListSolution testdataAllowsUnassignedValuesListSolution) {
                var unusedEntities = 0;
                for (var entity : testdataAllowsUnassignedValuesListSolution.getEntityList()) {
                    if (entity.getValueList().isEmpty()) {
                        unusedEntities++;
                    }
                }
                return SimpleScore.of(unusedEntities);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    public static class CorruptedEasyScoreCalculator implements EasyScoreCalculator<TestdataSolution, SimpleScore> {

        @Override
        public @NonNull SimpleScore calculateScore(@NonNull TestdataSolution testdataSolution) {
            var random = new Random();
            return SimpleScore.of(random.nextInt(1000));
        }
    }

    public static class CorruptedIncrementalScoreCalculator
            implements ConstraintMatchAwareIncrementalScoreCalculator<TestdataSolution, SimpleScore> {

        @Override
        public void resetWorkingSolution(@NonNull TestdataSolution workingSolution, boolean constraintMatchEnabled) {
            // Ignore
        }

        @Override
        public @NonNull Collection<ConstraintMatchTotal<SimpleScore>> getConstraintMatchTotals() {
            return Collections.singletonList(new DefaultConstraintMatchTotal<>(ConstraintRef.of("a", "b"), SimpleScore.of(1)));
        }

        @Override
        public @Nullable Map<Object, Indictment<SimpleScore>> getIndictmentMap() {
            return Collections.singletonMap(new TestdataEntity("e1"),
                    new DefaultIndictment<>(new TestdataEntity("e1"), SimpleScore.ONE));
        }

        @Override
        public void resetWorkingSolution(@NonNull TestdataSolution workingSolution) {
            // Ignore
        }

        @Override
        public void beforeEntityAdded(@NonNull Object entity) {
            // Ignore
        }

        @Override
        public void afterEntityAdded(@NonNull Object entity) {
            // Ignore
        }

        @Override
        public void beforeVariableChanged(@NonNull Object entity, @NonNull String variableName) {
            // Ignore
        }

        @Override
        public void afterVariableChanged(@NonNull Object entity, @NonNull String variableName) {
            // Ignore
        }

        @Override
        public void beforeEntityRemoved(@NonNull Object entity) {
            // Ignore
        }

        @Override
        public void afterEntityRemoved(@NonNull Object entity) {
            // Ignore
        }

        @Override
        public @NonNull SimpleScore calculateScore() {
            var random = new Random();
            return SimpleScore.of(random.nextInt(1000));
        }
    }

    @NullMarked
    public static final class TestingNeighborhoodProvider implements NeighborhoodProvider<TestdataSolution> {

        @Override
        public Neighborhood defineNeighborhood(NeighborhoodBuilder<TestdataSolution> builder) {
            throw new UnsupportedOperationException(); // The test will not get here.
        }

    }

    public static final class DummyEasyScoreCalculator implements EasyScoreCalculator<TestdataSolution, SimpleScore> {

        @Override
        public @NonNull SimpleScore calculateScore(@NonNull TestdataSolution solution) {
            var valueList = solution.getValueList();
            var firstValue = valueList.get(0);
            var valueSet = new HashSet<TestdataValue>(valueList.size());
            solution.getEntityList().forEach(e -> {
                if (e.getValue() == firstValue) {
                    valueSet.add(e.getValue());
                }
            });
            return SimpleScore.of(-valueSet.size());
        }

    }

    public static final class DummyMultiVarEasyScoreCalculator
            implements EasyScoreCalculator<TestdataMultiVarSolution, SimpleScore> {

        @Override
        public @NonNull SimpleScore calculateScore(@NonNull TestdataMultiVarSolution solution) {
            var primaryValue = solution.getValueList().get(0);
            var secondaryValue = solution.getValueList().get(1);
            var otherValue = solution.getOtherValueList().get(0);
            var valueSet = new HashSet<Pair<Object, Integer>>();
            solution.getMultiVarEntityList().forEach(e -> {
                if (e.getPrimaryValue() == primaryValue) {
                    valueSet.add(new Pair<>(e.getPrimaryValue(), 1));
                }
                if (e.getSecondaryValue() == secondaryValue) {
                    valueSet.add(new Pair<>(e.getSecondaryValue(), 2));
                }
                if (e.getTertiaryValueAllowedUnassigned() == otherValue) {
                    valueSet.add(new Pair<>(e.getTertiaryValueAllowedUnassigned(), 2));
                }
            });
            return SimpleScore.of(-valueSet.size());
        }

    }

    public static final class DummyMultiEntityEasyScoreCalculator
            implements EasyScoreCalculator<TestdataMultiEntitySolution, SimpleScore> {

        @Override
        public @NonNull SimpleScore calculateScore(@NonNull TestdataMultiEntitySolution solution) {
            var primaryValue = solution.getValueList().get(0);
            var secondaryValue = solution.getLeadEntityList().get(0);
            var valueSet = new HashSet<>();
            solution.getLeadEntityList().forEach(e -> {
                if (e.getValue() == primaryValue) {
                    valueSet.add(e.getValue());
                }
            });
            solution.getHerdEntityList().forEach(e -> {
                if (e.getLeadEntity() == secondaryValue) {
                    valueSet.add(e.getLeadEntity());
                }
            });
            return SimpleScore.of(-valueSet.size());
        }

    }

    /**
     * Penalizes the number of values in the list, exponentially.
     * Only penalizes is length of the list is greater than 1.
     */
    public static final class TestingListEasyScoreCalculator implements EasyScoreCalculator<TestdataListSolution, SimpleScore> {

        @Override
        public @NonNull SimpleScore calculateScore(@NonNull TestdataListSolution testdataSolution) {
            var sum = new LongAdder();
            testdataSolution.getEntityList().forEach(e -> {
                var size = e.getValueList().size();
                if (size > 1) {
                    var penalty = Math.pow(size - 1, 2);
                    sum.add((long) penalty);
                }
            });
            return SimpleScore.of(-sum.intValue());
        }

    }

    public static final class TestingMixedEasyScoreCalculator
            implements EasyScoreCalculator<TestdataMixedSolution, SimpleScore> {

        @Override
        public @NonNull SimpleScore calculateScore(@NonNull TestdataMixedSolution testdataSolution) {
            var firstValue = testdataSolution.getOtherValueList().get(0);
            var secondValue = testdataSolution.getOtherValueList().get(1);
            var sum = new LongAdder();
            testdataSolution.getEntityList().forEach(e -> {
                var size = e.getValueList().size();
                if (size > 1) {
                    var penalty = Math.pow(size - 1, 2);
                    sum.add((long) penalty);
                }
                if (e.getBasicValue() == firstValue) {
                    sum.add(1L);
                }
                if (e.getSecondBasicValue() == secondValue) {
                    sum.add(1L);
                }
            });
            return SimpleScore.of(-sum.intValue());
        }

    }

    public static final class InvalidCustomPhaseCommand implements PhaseCommand<TestdataListSolution> {

        @Override
        public void changeWorkingSolution(ScoreDirector<TestdataListSolution> scoreDirector,
                BooleanSupplier isPhaseTerminated) {
            var entity = scoreDirector.getWorkingSolution().getEntityList().get(0);
            scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
            entity.getValueList().add(new TestdataListValue("bad value"));
            scoreDirector.afterListVariableChanged(entity, "valueList", 0, entity.getValueList().size());
        }
    }

    public static final class InvalidMoveListFactory implements MoveIteratorFactory<TestdataListSolution, InvalidMove> {
        @Override
        public long getSize(ScoreDirector<TestdataListSolution> scoreDirector) {
            return 1;
        }

        @Override
        public Iterator<InvalidMove>
                createOriginalMoveIterator(ScoreDirector<TestdataListSolution> scoreDirector) {
            return List.of(new InvalidMove()).iterator();
        }

        @Override
        public Iterator<InvalidMove> createRandomMoveIterator(
                ScoreDirector<TestdataListSolution> scoreDirector,
                Random workingRandom) {
            return createOriginalMoveIterator(scoreDirector);
        }
    }

    public static final class InvalidMove extends AbstractMove<TestdataListSolution> {

        @Override
        protected void doMoveOnGenuineVariables(ScoreDirector<TestdataListSolution> scoreDirector) {
            var entity = scoreDirector.getWorkingSolution().getEntityList().get(0);
            scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
            entity.getValueList().add(new TestdataListValue("bad value"));
            scoreDirector.afterListVariableChanged(entity, "valueList", 0, entity.getValueList().size());
        }

        @Override
        public boolean isMoveDoable(ScoreDirector<TestdataListSolution> scoreDirector) {
            return true;
        }
    }
}
