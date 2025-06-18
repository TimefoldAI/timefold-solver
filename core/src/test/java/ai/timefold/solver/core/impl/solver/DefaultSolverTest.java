package ai.timefold.solver.core.impl.solver;

import static ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner.DECREASING_DIFFICULTY;
import static ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner.DECREASING_DIFFICULTY_IF_AVAILABLE;
import static ai.timefold.solver.core.config.solver.PreviewFeature.DECLARATIVE_SHADOW_VARIABLES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicType;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedEntityPlacerConfig;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedValuePlacerConfig;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySorterManner;
import ai.timefold.solver.core.config.heuristic.selector.entity.pillar.PillarSelectorConfig;
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
import ai.timefold.solver.core.config.localsearch.LocalSearchType;
import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.monitoring.MonitoringConfig;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMove;
import ai.timefold.solver.core.impl.move.streams.generic.provider.ChangeMoveProvider;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProvider;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProviders;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.DummySimpleScoreEasyScoreCalculator;
import ai.timefold.solver.core.impl.score.constraint.DefaultConstraintMatchTotal;
import ai.timefold.solver.core.impl.score.constraint.DefaultIndictment;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
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
import ai.timefold.solver.core.testdomain.mixed.multientity.TestdataMixedEntityEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.mixed.multientity.TestdataMixedMultiEntityFirstEntity;
import ai.timefold.solver.core.testdomain.mixed.multientity.TestdataMixedMultiEntitySecondEntity;
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
import ai.timefold.solver.core.testdomain.score.TestdataHardSoftScoreSolution;
import ai.timefold.solver.core.testutil.AbstractMeterTest;
import ai.timefold.solver.core.testutil.NoChangeCustomPhaseCommand;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

@ExtendWith(SoftAssertionsExtension.class)
class DefaultSolverTest extends AbstractMeterTest {

    @Test
    void solve() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getValue() == null)).isEmpty();
    }

    @Test
    void solveWithMoveStreams() {
        var solverConfig = new SolverConfig()
                .withPreviewFeature(PreviewFeature.MOVE_STREAMS)
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestingEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig()
                        .withBestScoreLimit("0")) // Should get there quickly.
                .withPhases(new LocalSearchPhaseConfig()
                        .withMoveProvidersClass(TestingMoveProviders.class));

        var solution = TestdataSolution.generateSolution(3, 2);

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getValue() == null)).isEmpty();
    }

    @Test
    void solveWithMoveStreamsNotEnabled() {
        var solverConfig = new SolverConfig() // Preview feature not enabled.
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestingEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig()
                        .withBestScoreLimit("0")) // Should get there quickly.
                .withPhases(new LocalSearchPhaseConfig()
                        .withMoveProvidersClass(TestingMoveProviders.class));

        var solution = TestdataSolution.generateSolution(3, 2);
        Assertions.assertThatThrownBy(() -> PlannerTestUtils.solve(solverConfig, solution))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MOVE_STREAMS");
    }

    @Test
    void solveWithMoveStreamsAndMoveSelectorsFails() {
        var solverConfig = new SolverConfig()
                .withPreviewFeature(PreviewFeature.MOVE_STREAMS)
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEasyScoreCalculatorClass(TestingEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig()
                        .withBestScoreLimit("0")) // Should get there quickly.
                .withPhases(new LocalSearchPhaseConfig()
                        .withMoveSelectorConfig(new ChangeMoveSelectorConfig())
                        .withMoveProvidersClass(TestingMoveProviders.class));

        var solution = TestdataSolution.generateSolution(3, 2);
        Assertions.assertThatThrownBy(() -> PlannerTestUtils.solve(solverConfig, solution))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("move selectors")
                .hasMessageContaining("Move Streams");
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
    void checkDefaultMeters() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class);
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);

        var solver = (DefaultSolver<TestdataSolution>) solverFactory.buildSolver();
        meterRegistry.publish();
        assertThat(meterRegistry.getMeters().stream().map(Meter::getId)).isEmpty();

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        var updatedTime = new AtomicBoolean();
        var latch = new CountDownLatch(1);
        solver.addEventListener(event -> {
            if (!updatedTime.get()) {
                assertThat(meterRegistry.getMeters().stream().map(Meter::getId))
                        .containsExactlyInAnyOrder(
                                new Meter.Id(SolverMetric.SOLVE_DURATION.getMeterId(),
                                        Tags.empty(),
                                        null,
                                        null,
                                        Meter.Type.LONG_TASK_TIMER),
                                new Meter.Id(SolverMetric.ERROR_COUNT.getMeterId(),
                                        Tags.empty(),
                                        null,
                                        null,
                                        Meter.Type.COUNTER),
                                new Meter.Id(SolverMetric.SCORE_CALCULATION_COUNT.getMeterId(),
                                        Tags.empty(),
                                        null,
                                        null,
                                        Meter.Type.GAUGE),
                                new Meter.Id(SolverMetric.MOVE_EVALUATION_COUNT.getMeterId(),
                                        Tags.empty(),
                                        null,
                                        null,
                                        Meter.Type.GAUGE),
                                new Meter.Id(SolverMetric.PROBLEM_ENTITY_COUNT.getMeterId(),
                                        Tags.empty(),
                                        null,
                                        null,
                                        Meter.Type.GAUGE),
                                new Meter.Id(SolverMetric.PROBLEM_VARIABLE_COUNT.getMeterId(),
                                        Tags.empty(),
                                        null,
                                        null,
                                        Meter.Type.GAUGE),
                                new Meter.Id(SolverMetric.PROBLEM_VALUE_COUNT.getMeterId(),
                                        Tags.empty(),
                                        null,
                                        null,
                                        Meter.Type.GAUGE),
                                new Meter.Id(SolverMetric.PROBLEM_SIZE_LOG.getMeterId(),
                                        Tags.empty(),
                                        null,
                                        null,
                                        Meter.Type.GAUGE));
                updatedTime.set(true);
            }
            latch.countDown();
        });
        solver.solve(solution);

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assertions.fail("Failed waiting for the event to happen.", e);
        }

        // Score calculation and problem scale counts should be removed
        // since registering multiple gauges with the same id
        // make it return the average, and the solver holds
        // onto the solver scope, meaning it won't automatically
        // be deregistered.
        assertThat(meterRegistry.getMeters().stream().map(Meter::getId))
                .containsExactlyInAnyOrder(
                        new Meter.Id(SolverMetric.SOLVE_DURATION.getMeterId(),
                                Tags.empty(),
                                null,
                                null,
                                Meter.Type.LONG_TASK_TIMER),
                        new Meter.Id(SolverMetric.ERROR_COUNT.getMeterId(),
                                Tags.empty(),
                                null,
                                null,
                                Meter.Type.COUNTER));
    }

    @Test
    void checkDefaultMetersTags() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);

        var solver = (DefaultSolver<TestdataSolution>) solverFactory.buildSolver();
        solver.setMonitorTagMap(Map.of("tag.key", "tag.value"));
        meterRegistry.publish();
        assertThat(meterRegistry.getMeters().stream().map(Meter::getId)).isEmpty();

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        var updatedTime = new AtomicBoolean();
        var latch = new CountDownLatch(1);
        solver.addEventListener(event -> {
            if (!updatedTime.get()) {
                assertThat(meterRegistry.getMeters().stream().map(Meter::getId))
                        .containsExactlyInAnyOrder(
                                new Meter.Id(SolverMetric.SOLVE_DURATION.getMeterId(),
                                        Tags.empty(),
                                        null,
                                        null,
                                        Meter.Type.LONG_TASK_TIMER),
                                new Meter.Id(SolverMetric.ERROR_COUNT.getMeterId(),
                                        Tags.empty(),
                                        null,
                                        null,
                                        Meter.Type.COUNTER),
                                new Meter.Id(SolverMetric.SCORE_CALCULATION_COUNT.getMeterId(),
                                        Tags.of("tag.key", "tag.value"),
                                        null,
                                        null,
                                        Meter.Type.GAUGE),
                                new Meter.Id(SolverMetric.MOVE_EVALUATION_COUNT.getMeterId(),
                                        Tags.of("tag.key", "tag.value"),
                                        null,
                                        null,
                                        Meter.Type.GAUGE),
                                new Meter.Id(SolverMetric.PROBLEM_ENTITY_COUNT.getMeterId(),
                                        Tags.of("tag.key", "tag.value"),
                                        null,
                                        null,
                                        Meter.Type.GAUGE),
                                new Meter.Id(SolverMetric.PROBLEM_VARIABLE_COUNT.getMeterId(),
                                        Tags.of("tag.key", "tag.value"),
                                        null,
                                        null,
                                        Meter.Type.GAUGE),
                                new Meter.Id(SolverMetric.PROBLEM_VALUE_COUNT.getMeterId(),
                                        Tags.of("tag.key", "tag.value"),
                                        null,
                                        null,
                                        Meter.Type.GAUGE),
                                new Meter.Id(SolverMetric.PROBLEM_SIZE_LOG.getMeterId(),
                                        Tags.of("tag.key", "tag.value"),
                                        null,
                                        null,
                                        Meter.Type.GAUGE));
                updatedTime.set(true);
            }
            latch.countDown();
        });
        solver.solve(solution);

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assertions.fail("Failed waiting for the event to happen.", e);
        }

        // Score calculation and problem scale counts should be removed
        // since registering multiple gauges with the same id
        // make it return the average, and the solver holds
        // onto the solver scope, meaning it won't automatically
        // be deregistered.
        assertThat(meterRegistry.getMeters().stream().map(Meter::getId))
                .containsExactlyInAnyOrder(
                        new Meter.Id(SolverMetric.SOLVE_DURATION.getMeterId(),
                                Tags.empty(),
                                null,
                                null,
                                Meter.Type.LONG_TASK_TIMER),
                        new Meter.Id(SolverMetric.ERROR_COUNT.getMeterId(),
                                Tags.empty(),
                                null,
                                null,
                                Meter.Type.COUNTER));
    }

    @Test
    void solveMetrics() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class);
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);

        var solver = solverFactory.buildSolver();
        ((DefaultSolver<TestdataSolution>) solver).setMonitorTagMap(Map.of("solver.id", UUID.randomUUID().toString()));
        meterRegistry.publish();

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        var latch = new CountDownLatch(1);
        var updatedTime = new AtomicBoolean();
        solver.addEventListener(event -> {
            if (!updatedTime.get()) {
                meterRegistry.getClock().addSeconds(2);
                meterRegistry.publish();
                assertThat(meterRegistry.getMeasurement(SolverMetric.SOLVE_DURATION.getMeterId(), "ACTIVE_TASKS")).isOne();
                assertThat(meterRegistry.getMeasurement(SolverMetric.SOLVE_DURATION.getMeterId(), "DURATION").longValue())
                        .isEqualTo(2L);

                // 2 Entities
                assertThat(meterRegistry.getMeasurement(SolverMetric.PROBLEM_ENTITY_COUNT.getMeterId(), "VALUE").longValue())
                        .isEqualTo(2L);
                // 1 Genuine variable on 2 entities = 2 total variables
                assertThat(meterRegistry.getMeasurement(SolverMetric.PROBLEM_VARIABLE_COUNT.getMeterId(), "VALUE").longValue())
                        .isEqualTo(2L);
                // The maximum assignable value count of any variable is 2
                assertThat(meterRegistry.getMeasurement(SolverMetric.PROBLEM_VALUE_COUNT.getMeterId(), "VALUE").longValue())
                        .isEqualTo(2L);
                updatedTime.set(true);
            }
            latch.countDown();
        });
        solution = solver.solve(solution);

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assertions.fail("Failed waiting for the event to happen.", e);
        }
        meterRegistry.publish();
        assertThat(solution).isNotNull();
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getValue() == null)).isEmpty();

        assertThat(meterRegistry.getMeasurement(SolverMetric.SOLVE_DURATION.getMeterId(), "DURATION")).isZero();
        assertThat(meterRegistry.getMeasurement(SolverMetric.SOLVE_DURATION.getMeterId(), "ACTIVE_TASKS")).isZero();
        assertThat(meterRegistry.getMeasurement(SolverMetric.ERROR_COUNT.getMeterId(), "COUNT")).isZero();
        assertThat(meterRegistry.getMeasurement(SolverMetric.SCORE_CALCULATION_COUNT.getMeterId(), "VALUE")).isPositive();
        assertThat(meterRegistry.getMeasurement(SolverMetric.MOVE_EVALUATION_COUNT.getMeterId(), "VALUE")).isPositive();
    }

    @Test
    void solveMetricsProblemChange() throws InterruptedException, ExecutionException {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class);
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);

        var solver = solverFactory.buildSolver();
        meterRegistry.publish();

        final var solution = new TestdataSolution("s1");
        solution.setValueList(new ArrayList<>(List.of(new TestdataValue("v1"), new TestdataValue("v2"))));
        solution.setEntityList(new ArrayList<>(List.of(new TestdataEntity("e1"), new TestdataEntity("e2"))));

        var latch = new CountDownLatch(1);
        solver.addEventListener(bestSolutionChangedEvent -> {
            try {
                latch.await();
                meterRegistry.publish();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        var executorService = Executors.newSingleThreadExecutor();
        var job = executorService.submit(() -> {
            solver.solve(solution);
        });

        solver.addProblemChange((workingSolution, problemChangeDirector) -> {
            problemChangeDirector.addEntity(new TestdataEntity("added entity"), workingSolution.getEntityList()::add);
            problemChangeDirector.addProblemFact(new TestdataValue("added value"), workingSolution.getValueList()::add);
        });

        latch.countDown();
        job.get();
        // 3 Entities
        assertThat(
                meterRegistry.getMeasurement(SolverMetric.PROBLEM_ENTITY_COUNT.getMeterId(), "VALUE").longValue())
                .isEqualTo(3L);
        // 1 Genuine variable on 3 entities = 3 total variables
        assertThat(
                meterRegistry.getMeasurement(SolverMetric.PROBLEM_VARIABLE_COUNT.getMeterId(), "VALUE").longValue())
                .isEqualTo(3L);
        assertThat(meterRegistry.getMeasurement(SolverMetric.PROBLEM_VALUE_COUNT.getMeterId(), "VALUE").longValue())
                .isEqualTo(3L);
    }

    public static class BestScoreMetricEasyScoreCalculator
            implements EasyScoreCalculator<TestdataHardSoftScoreSolution, HardSoftScore> {

        @Override
        public @NonNull HardSoftScore calculateScore(@NonNull TestdataHardSoftScoreSolution testdataSolution) {
            var count = testdataSolution.getEntityList()
                    .stream()
                    .filter(e -> e.getValue() != null)
                    .filter(e -> e.getValue().getCode().startsWith("reward"))
                    .count();
            return HardSoftScore.ofSoft((int) count);
        }
    }

    public static class NoneValueSelectionFilter
            implements SelectionFilter<TestdataHardSoftScoreSolution, ChangeMove<TestdataHardSoftScoreSolution>> {
        @Override
        public boolean accept(ScoreDirector<TestdataHardSoftScoreSolution> scoreDirector,
                ChangeMove<TestdataHardSoftScoreSolution> selection) {
            return ((TestdataValue) (selection.getToPlanningValue())).getCode().equals("none");
        }
    }

    @Test
    void solveBestScoreMetrics() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataHardSoftScoreSolution.class, TestdataEntity.class);
        solverConfig.setScoreDirectorFactoryConfig(
                new ScoreDirectorFactoryConfig().withEasyScoreCalculatorClass(BestScoreMetricEasyScoreCalculator.class));
        solverConfig.setTerminationConfig(new TerminationConfig().withBestScoreLimit("0hard/2soft"));
        solverConfig.setMonitoringConfig(new MonitoringConfig()
                .withSolverMetricList(List.of(SolverMetric.BEST_SCORE)));
        solverConfig.setPhaseConfigList(List.of(
                // Force Timefold to select "none" value which reward 0 soft
                new ConstructionHeuristicPhaseConfig()
                        .withConstructionHeuristicType(ConstructionHeuristicType.FIRST_FIT)
                        .withMoveSelectorConfigList(
                                List.of(new ChangeMoveSelectorConfig()
                                        .withFilterClass(NoneValueSelectionFilter.class))),
                // Then do a local search, which allow Timefold to select "reward" value
                // which reward 1 soft per entity
                new LocalSearchPhaseConfig()
                        .withLocalSearchType(LocalSearchType.HILL_CLIMBING)));
        SolverFactory<TestdataHardSoftScoreSolution> solverFactory = SolverFactory.create(solverConfig);

        var solver = solverFactory.buildSolver();
        ((DefaultSolver<TestdataHardSoftScoreSolution>) solver)
                .setMonitorTagMap(Map.of("solver.id", UUID.randomUUID().toString()));
        meterRegistry.publish();
        var solution = new TestdataHardSoftScoreSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("none"), new TestdataValue("reward")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));
        var step = new AtomicInteger(-1);

        var latch = new CountDownLatch(1);
        solver.addEventListener(event -> {
            meterRegistry.publish();

            // This event listener is added before the best score event listener
            // so it is one step behind
            if (step.get() != -1) {
                assertThat(
                        meterRegistry.getMeasurement(SolverMetric.BEST_SCORE.getMeterId() + ".hard.score", "VALUE").intValue())
                        .isZero();
            }
            if (step.get() == 0) {
                assertThat(
                        meterRegistry.getMeasurement(SolverMetric.BEST_SCORE.getMeterId() + ".soft.score", "VALUE").intValue())
                        .isZero();
            } else if (step.get() == 1) {
                assertThat(
                        meterRegistry.getMeasurement(SolverMetric.BEST_SCORE.getMeterId() + ".soft.score", "VALUE").intValue())
                        .isEqualTo(1);
            } else if (step.get() == 2) {
                assertThat(
                        meterRegistry.getMeasurement(SolverMetric.BEST_SCORE.getMeterId() + ".soft.score", "VALUE").intValue())
                        .isEqualTo(2);
            }
            step.incrementAndGet();
            latch.countDown();
        });
        solution = solver.solve(solution);

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Failed waiting for the event to happen.", e);
        }
        assertThat(step.get()).isEqualTo(2);
        meterRegistry.publish();
        assertThat(solution).isNotNull();
        assertThat(meterRegistry.getMeasurement(SolverMetric.BEST_SCORE.getMeterId() + ".hard.score", "VALUE").intValue())
                .isZero();
        assertThat(meterRegistry.getMeasurement(SolverMetric.BEST_SCORE.getMeterId() + ".soft.score", "VALUE").intValue())
                .isEqualTo(2);
    }

    private static class SetTestdataEntityValueCustomPhaseCommand implements PhaseCommand<TestdataHardSoftScoreSolution> {
        final TestdataEntity entity;
        final TestdataValue value;

        public SetTestdataEntityValueCustomPhaseCommand(TestdataEntity entity, TestdataValue value) {
            this.entity = entity;
            this.value = value;
        }

        @Override
        public void changeWorkingSolution(ScoreDirector<TestdataHardSoftScoreSolution> scoreDirector,
                BooleanSupplier isPhaseTerminated) {
            var workingEntity = scoreDirector.lookUpWorkingObject(entity);
            var workingValue = scoreDirector.lookUpWorkingObject(value);

            scoreDirector.beforeVariableChanged(workingEntity, "value");
            workingEntity.setValue(workingValue);
            scoreDirector.afterVariableChanged(workingEntity, "value");
            scoreDirector.triggerVariableListeners();
        }
    }

    @Test
    void solveStepScoreMetrics() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataHardSoftScoreSolution.class, TestdataEntity.class);
        solverConfig.setScoreDirectorFactoryConfig(
                new ScoreDirectorFactoryConfig().withEasyScoreCalculatorClass(BestScoreMetricEasyScoreCalculator.class));
        solverConfig.setTerminationConfig(new TerminationConfig().withBestScoreLimit("0hard/3soft"));
        solverConfig.setMonitoringConfig(new MonitoringConfig()
                .withSolverMetricList(List.of(SolverMetric.STEP_SCORE)));

        var solution = new TestdataHardSoftScoreSolution("s1");
        var e1 = new TestdataEntity("e1");
        var e2 = new TestdataEntity("e2");
        var e3 = new TestdataEntity("e3");
        var none = new TestdataValue("none");
        var reward = new TestdataValue("reward");
        solution.setValueList(Arrays.asList(none, reward));
        solution.setEntityList(Arrays.asList(e1, e2, e3));

        solverConfig.setPhaseConfigList(List.of(
                // Force Timefold to select "none" value which reward 0 soft
                new ConstructionHeuristicPhaseConfig()
                        .withConstructionHeuristicType(ConstructionHeuristicType.FIRST_FIT)
                        .withMoveSelectorConfigList(
                                List.of(new ChangeMoveSelectorConfig()
                                        .withFilterClass(NoneValueSelectionFilter.class))),
                // Then do a custom phase, to force certain steps to be taken
                new CustomPhaseConfig()
                        .withCustomPhaseCommands(
                                new SetTestdataEntityValueCustomPhaseCommand(e1, reward),
                                new SetTestdataEntityValueCustomPhaseCommand(e2, reward),
                                new SetTestdataEntityValueCustomPhaseCommand(e1, none),
                                new SetTestdataEntityValueCustomPhaseCommand(e1, reward),
                                new SetTestdataEntityValueCustomPhaseCommand(e3, reward))));
        SolverFactory<TestdataHardSoftScoreSolution> solverFactory = SolverFactory.create(solverConfig);

        var solver = solverFactory.buildSolver();
        ((DefaultSolver<TestdataHardSoftScoreSolution>) solver)
                .setMonitorTagMap(Map.of("solver.id", UUID.randomUUID().toString()));
        var step = new AtomicInteger(-1);

        ((DefaultSolver<TestdataHardSoftScoreSolution>) solver)
                .addPhaseLifecycleListener(new PhaseLifecycleListenerAdapter<TestdataHardSoftScoreSolution>() {
                    @Override
                    public void stepEnded(AbstractStepScope<TestdataHardSoftScoreSolution> stepScope) {
                        super.stepEnded(stepScope);
                        meterRegistry.publish();

                        // first 3 steps are construction heuristic steps and don't have a step score since it uninitialized
                        if (step.get() < 2) {
                            step.incrementAndGet();
                            return;
                        }

                        assertThat(
                                meterRegistry.getMeasurement(SolverMetric.STEP_SCORE.getMeterId() + ".hard.score", "VALUE")
                                        .intValue())
                                .isZero();

                        if (step.get() == 2) {
                            assertThat(
                                    meterRegistry.getMeasurement(SolverMetric.STEP_SCORE.getMeterId() + ".soft.score", "VALUE")
                                            .intValue())
                                    .isZero();
                        } else if (step.get() == 3) {
                            assertThat(
                                    meterRegistry.getMeasurement(SolverMetric.STEP_SCORE.getMeterId() + ".soft.score", "VALUE")
                                            .intValue())
                                    .isEqualTo(1);
                        } else if (step.get() == 4) {
                            assertThat(
                                    meterRegistry.getMeasurement(SolverMetric.STEP_SCORE.getMeterId() + ".soft.score", "VALUE")
                                            .intValue())
                                    .isEqualTo(2);
                        } else if (step.get() == 5) {
                            assertThat(
                                    meterRegistry.getMeasurement(SolverMetric.STEP_SCORE.getMeterId() + ".soft.score", "VALUE")
                                            .intValue())
                                    .isEqualTo(1);
                        } else if (step.get() == 6) {
                            assertThat(
                                    meterRegistry.getMeasurement(SolverMetric.STEP_SCORE.getMeterId() + ".soft.score", "VALUE")
                                            .intValue())
                                    .isEqualTo(2);
                        }
                        step.incrementAndGet();
                    }
                });
        solution = solver.solve(solution);

        assertThat(step.get()).isEqualTo(7);
        meterRegistry.publish();
        assertThat(solution).isNotNull();
        assertThat(meterRegistry.getMeasurement(SolverMetric.STEP_SCORE.getMeterId() + ".hard.score", "VALUE").intValue())
                .isZero();
        assertThat(meterRegistry.getMeasurement(SolverMetric.STEP_SCORE.getMeterId() + ".soft.score", "VALUE").intValue())
                .isEqualTo(3);
    }

    public static class ErrorThrowingEasyScoreCalculator implements EasyScoreCalculator<TestdataSolution, SimpleScore> {

        @Override
        public @NonNull SimpleScore calculateScore(@NonNull TestdataSolution testdataSolution) {
            throw new IllegalStateException("Thrown exception in constraint provider");
        }
    }

    @Test
    void solveMetricsError() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withScoreDirectorFactory(
                        new ScoreDirectorFactoryConfig().withEasyScoreCalculatorClass(ErrorThrowingEasyScoreCalculator.class));
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);

        var solver = solverFactory.buildSolver();
        ((DefaultSolver<TestdataSolution>) solver).setMonitorTagMap(Map.of("solver.id", UUID.randomUUID().toString()));
        meterRegistry.publish();

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        meterRegistry.publish();

        assertThatCode(() -> solver.solve(solution))
                .hasStackTraceContaining("Thrown exception in constraint provider");

        meterRegistry.getClock().addSeconds(1);
        meterRegistry.publish();
        assertThat(meterRegistry.getMeasurement(SolverMetric.SOLVE_DURATION.getMeterId(), "ACTIVE_TASKS")).isZero();
        assertThat(meterRegistry.getMeasurement(SolverMetric.SOLVE_DURATION.getMeterId(), "DURATION")).isZero();
        assertThat(meterRegistry.getMeasurement(SolverMetric.ERROR_COUNT.getMeterId(), "COUNT")).isOne();
    }

    @Test
    void solveEmptyEntityList() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new CustomPhaseConfig()
                        .withCustomPhaseCommands(new FailCommand()));

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Collections.emptyList());

        solution = PlannerTestUtils.solve(solverConfig, solution, false);
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

        solution = PlannerTestUtils.solve(solverConfig, solution, false);
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

        solution = PlannerTestUtils.solve(solverConfig, solution, false);
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

        solution = PlannerTestUtils.solve(solverConfig, solution, false);
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
        var entity = TestdataAllowsUnassignedValuesListEntity.createWithValues("e1", value1, value2);

        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(Arrays.asList(value1, value2, value3, value4));

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
        var entity = TestdataAllowsUnassignedValuesListEntity.createWithValues("e1", value1, value2);

        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(Arrays.asList(value1, value2, value3, value4));

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
     * @see <a href="https://github.com/TimefoldAI/timefold-solver/issues/1130">Github issue 1130</a>.
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
        var entity = TestdataAllowsUnassignedValuesListEntity.createWithValues("e1");

        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(value1));

        var bestSolution = PlannerTestUtils.solve(solverConfig, solution, false);
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
        partiallyPinnedEntity.setPlanningPinToIndex(1); // The first value is pinned.
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
        partiallyPinnedEntity.setPlanningPinToIndex(1); // The first value is pinned.
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
                .withPreviewFeature(DECLARATIVE_SHADOW_VARIABLES)
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
                .withPreviewFeature(DECLARATIVE_SHADOW_VARIABLES)
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
                .withPreviewFeature(DECLARATIVE_SHADOW_VARIABLES)
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
                .withPreviewFeature(DECLARATIVE_SHADOW_VARIABLES)
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
                .withPreviewFeature(DECLARATIVE_SHADOW_VARIABLES)
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
    void solvePinnedMixedModel() {
        // We don't enable the LS because we want to ensure the pinned entity remains uninitialized
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataMixedSolution.class, TestdataMixedEntity.class, TestdataMixedValue.class,
                TestdataMixedOtherValue.class)
                .withPreviewFeature(DECLARATIVE_SHADOW_VARIABLES)
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
                .withPreviewFeature(DECLARATIVE_SHADOW_VARIABLES)
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

    @Test
    void failRuinRecreateWithMultiVar() {
        // Solver config
        var localSearchConfig = new LocalSearchPhaseConfig().withMoveSelectorConfig(new RuinRecreateMoveSelectorConfig());
        var solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataMixedSolution.class, TestdataMixedEntity.class, TestdataMixedValue.class,
                TestdataMixedOtherValue.class)
                .withPreviewFeature(DECLARATIVE_SHADOW_VARIABLES)
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
                .withPreviewFeature(DECLARATIVE_SHADOW_VARIABLES)
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
                .withPreviewFeature(DECLARATIVE_SHADOW_VARIABLES)
                .withPhases(localSearchConfig)
                .withEasyScoreCalculatorClass(TestdataMixedEntityEasyScoreCalculator.class);
        var problem = TestdataMixedMultiEntitySolution.generateUninitializedSolution(2, 2, 2);
        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, problem))
                .hasMessageContaining("has no entityClass configured and because there are multiple in the entityClassSet")
                .hasMessageContaining("it cannot be deduced automatically");
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

    public static final class TestingMoveProviders implements MoveProviders<TestdataSolution> {
        @Override
        public List<MoveProvider<TestdataSolution>> defineMoves(PlanningSolutionMetaModel<TestdataSolution> solutionMetaModel) {
            var variableMetamodel = solutionMetaModel.entity(TestdataEntity.class)
                    .<TestdataValue> genuineVariable("value");
            return List.of(new ChangeMoveProvider<>(
                    (PlanningVariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue>) variableMetamodel));
        }
    }

    /**
     * Penalizes the number of values which are not the first value.
     */
    public static final class TestingEasyScoreCalculator implements EasyScoreCalculator<TestdataSolution, SimpleScore> {

        @Override
        public @NonNull SimpleScore calculateScore(@NonNull TestdataSolution testdataSolution) {
            var valueList = testdataSolution.getValueList();
            var firstValue = valueList.get(0);
            var valueSet = new HashSet<TestdataValue>(valueList.size());
            testdataSolution.getEntityList().forEach(e -> {
                if (e.getValue() != firstValue) {
                    valueSet.add(e.getValue());
                }
            });
            return SimpleScore.of(-valueSet.size());
        }

    }

}
