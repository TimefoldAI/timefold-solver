package ai.timefold.solver.core.impl.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.ConstraintMatchAwareIncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.constraint.Indictment;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicType;
import ai.timefold.solver.core.config.constructionheuristic.placer.QueuedEntityPlacerConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.SwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.TailChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchType;
import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.monitoring.MonitoringConfig;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMove;
import ai.timefold.solver.core.impl.phase.custom.CustomPhaseCommand;
import ai.timefold.solver.core.impl.phase.custom.NoChangeCustomPhaseCommand;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.DummySimpleScoreEasyScoreCalculator;
import ai.timefold.solver.core.impl.score.constraint.DefaultConstraintMatchTotal;
import ai.timefold.solver.core.impl.score.constraint.DefaultIndictment;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.allows_unassigned.TestdataAllowsUnassignedEasyScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.allows_unassigned.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.impl.testdata.domain.allows_unassigned.TestdataAllowsUnassignedSolution;
import ai.timefold.solver.core.impl.testdata.domain.chained.TestdataChainedAnchor;
import ai.timefold.solver.core.impl.testdata.domain.chained.TestdataChainedEntity;
import ai.timefold.solver.core.impl.testdata.domain.chained.TestdataChainedSolution;
import ai.timefold.solver.core.impl.testdata.domain.chained.multientity.TestdataChainedBrownEntity;
import ai.timefold.solver.core.impl.testdata.domain.chained.multientity.TestdataChainedGreenEntity;
import ai.timefold.solver.core.impl.testdata.domain.chained.multientity.TestdataChainedMultiEntityAnchor;
import ai.timefold.solver.core.impl.testdata.domain.chained.multientity.TestdataChainedMultiEntitySolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListEasyScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.TestdataPinnedListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.TestdataPinnedListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.TestdataPinnedListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListValue;
import ai.timefold.solver.core.impl.testdata.domain.multientity.TestdataHerdEntity;
import ai.timefold.solver.core.impl.testdata.domain.multientity.TestdataLeadEntity;
import ai.timefold.solver.core.impl.testdata.domain.multientity.TestdataMultiEntitySolution;
import ai.timefold.solver.core.impl.testdata.domain.pinned.TestdataPinnedEntity;
import ai.timefold.solver.core.impl.testdata.domain.pinned.TestdataPinnedSolution;
import ai.timefold.solver.core.impl.testdata.domain.score.TestdataHardSoftScoreSolution;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;
import ai.timefold.solver.core.impl.testutil.TestMeterRegistry;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

@ExtendWith(SoftAssertionsExtension.class)
class DefaultSolverTest {

    @BeforeEach
    void resetGlobalRegistry() {
        Metrics.globalRegistry.clear();
        List<MeterRegistry> meterRegistryList = new ArrayList<>();
        meterRegistryList.addAll(Metrics.globalRegistry.getRegistries());
        meterRegistryList.forEach(Metrics.globalRegistry::remove);
    }

    @Test
    void constructionHeuristicWithAllowsUnassignedBasicVariable() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataAllowsUnassignedSolution.class,
                TestdataAllowsUnassignedEntity.class)
                .withEasyScoreCalculatorClass(TestdataAllowsUnassignedEasyScoreCalculator.class);
        var phaseConfig = new ConstructionHeuristicPhaseConfig();
        solverConfig.setPhaseConfigList(Collections.singletonList(phaseConfig));
        var solverFactory = SolverFactory.<TestdataAllowsUnassignedSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();

        var value1 = new TestdataValue("v1");
        var value2 = new TestdataValue("v2");
        var entity = new TestdataAllowsUnassignedEntity("e1");
        entity.setValue(value1);
        var entity2 = new TestdataAllowsUnassignedEntity("e2");
        var entity3 = new TestdataAllowsUnassignedEntity("e3");

        var solution = new TestdataAllowsUnassignedSolution();
        solution.setEntityList(List.of(entity, entity2, entity3));
        solution.setValueList(Arrays.asList(value1, value2));

        var bestSolution = solver.solve(solution);
        assertSoftly(softly -> {
            softly.assertThat(bestSolution.getScore())
                    .isEqualTo(SimpleScore.of(-1)); // No value assigned twice, null once.
            var firstEntity = bestSolution.getEntityList().get(0);
            var firstValue = bestSolution.getValueList().get(0);
            softly.assertThat(firstEntity.getValue())
                    .isEqualTo(firstValue);
            var secondEntity = bestSolution.getEntityList().get(1);
            var secondValue = bestSolution.getValueList().get(1);
            softly.assertThat(secondEntity.getValue())
                    .isEqualTo(secondValue);
            var thirdEntity = bestSolution.getEntityList().get(2);
            softly.assertThat(thirdEntity.getValue())
                    .isNull();
        });

    }

    @Test
    void constructionHeuristicWithAllowsUnassignedValuesListVariable() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataAllowsUnassignedValuesListSolution.class,
                TestdataAllowsUnassignedValuesListEntity.class, TestdataAllowsUnassignedValuesListValue.class)
                .withEasyScoreCalculatorClass(TestdataAllowsUnassignedValuesListEasyScoreCalculator.class);
        var phaseConfig = new ConstructionHeuristicPhaseConfig();
        solverConfig.setPhaseConfigList(Collections.singletonList(phaseConfig));
        var solverFactory = SolverFactory.<TestdataAllowsUnassignedValuesListSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();

        var value1 = new TestdataAllowsUnassignedValuesListValue("v1");
        var value2 = new TestdataAllowsUnassignedValuesListValue("v2");
        var value3 = new TestdataAllowsUnassignedValuesListValue("v3");
        var value4 = new TestdataAllowsUnassignedValuesListValue("v4");
        var entity = TestdataAllowsUnassignedValuesListEntity.createWithValues("e1", value1, value2);

        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(Arrays.asList(value1, value2, value3, value4));

        var bestSolution = solver.solve(solution);
        assertSoftly(softly -> {
            softly.assertThat(bestSolution.getScore())
                    .isEqualTo(SimpleScore.of(-2)); // Length of the entity's value list.
            var firstEntity = bestSolution.getEntityList().get(0);
            var firstValue = bestSolution.getValueList().get(0);
            var secondValue = bestSolution.getValueList().get(1);
            softly.assertThat(firstEntity.getValueList())
                    .containsExactly(firstValue, secondValue);
        });

    }

    @Test
    void solve() {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class);
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<TestdataSolution> solver = solverFactory.buildSolver();

        TestdataSolution solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        solution = solver.solve(solution);
        assertThat(solution).isNotNull();
        assertThat(solution.getScore().isSolutionInitialized()).isTrue();
    }

    @Test
    void solveCorruptedEasyUninitialized() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withEasyScoreCalculatorClass(CorruptedEasyScoreCalculator.class);
        var solverFactory = SolverFactory.<TestdataSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        Assertions.assertThatThrownBy(() -> solver.solve(solution))
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
                .hasMessageContaining("Score corruption analysis could not be generated");
    }

    @Test
    void solveCorruptedIncrementalUninitialized() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withIncrementalScoreCalculatorClass(CorruptedIncrementalScoreCalculator.class));
        var solverFactory = SolverFactory.<TestdataSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        Assertions.assertThatThrownBy(() -> solver.solve(solution))
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
        TestMeterRegistry meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class);
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);

        DefaultSolver<TestdataSolution> solver = (DefaultSolver<TestdataSolution>) solverFactory.buildSolver();
        meterRegistry.publish(solver);
        assertThat(meterRegistry.getMeters().stream().map(Meter::getId)).isEmpty();

        TestdataSolution solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        AtomicBoolean updatedTime = new AtomicBoolean();
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
        });
        solver.solve(solution);

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
        TestMeterRegistry meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class);
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);

        DefaultSolver<TestdataSolution> solver = (DefaultSolver<TestdataSolution>) solverFactory.buildSolver();
        solver.setMonitorTagMap(Map.of("tag.key", "tag.value"));
        meterRegistry.publish(solver);
        assertThat(meterRegistry.getMeters().stream().map(Meter::getId)).isEmpty();

        TestdataSolution solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        AtomicBoolean updatedTime = new AtomicBoolean();
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
        });
        solver.solve(solution);

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
        TestMeterRegistry meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class);
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);

        Solver<TestdataSolution> solver = solverFactory.buildSolver();
        ((DefaultSolver<TestdataSolution>) solver).setMonitorTagMap(Map.of("solver.id", "solveMetrics"));
        meterRegistry.publish(solver);

        TestdataSolution solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        AtomicBoolean updatedTime = new AtomicBoolean();
        solver.addEventListener(event -> {
            if (!updatedTime.get()) {
                meterRegistry.getClock().addSeconds(2);
                meterRegistry.publish(solver);
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
        });
        solution = solver.solve(solution);

        meterRegistry.publish(solver);
        assertThat(solution).isNotNull();
        assertThat(solution.getScore().isSolutionInitialized()).isTrue();

        assertThat(meterRegistry.getMeasurement(SolverMetric.SOLVE_DURATION.getMeterId(), "DURATION")).isZero();
        assertThat(meterRegistry.getMeasurement(SolverMetric.SOLVE_DURATION.getMeterId(), "ACTIVE_TASKS")).isZero();
        assertThat(meterRegistry.getMeasurement(SolverMetric.ERROR_COUNT.getMeterId(), "COUNT")).isZero();
    }

    @Test
    void solveMetricsProblemChange() throws InterruptedException, ExecutionException {
        TestMeterRegistry meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class);
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);

        Solver<TestdataSolution> solver = solverFactory.buildSolver();
        meterRegistry.publish(solver);

        final TestdataSolution solution = new TestdataSolution("s1");
        solution.setValueList(new ArrayList<>(List.of(new TestdataValue("v1"), new TestdataValue("v2"))));
        solution.setEntityList(new ArrayList<>(List.of(new TestdataEntity("e1"), new TestdataEntity("e2"))));

        CountDownLatch latch = new CountDownLatch(1);
        solver.addEventListener(bestSolutionChangedEvent -> {
            try {
                latch.await();
                meterRegistry.publish(solver);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        ExecutorService executorService = Executors.newSingleThreadExecutor();
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
        public HardSoftScore calculateScore(TestdataHardSoftScoreSolution testdataSolution) {
            long count = testdataSolution.getEntityList()
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
        TestMeterRegistry meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(
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

        Solver<TestdataHardSoftScoreSolution> solver = solverFactory.buildSolver();
        ((DefaultSolver<TestdataHardSoftScoreSolution>) solver).setMonitorTagMap(Map.of("solver.id", "solveMetrics"));
        meterRegistry.publish(solver);
        TestdataHardSoftScoreSolution solution = new TestdataHardSoftScoreSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("none"), new TestdataValue("reward")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));
        AtomicInteger step = new AtomicInteger(-1);

        solver.addEventListener(event -> {
            meterRegistry.publish(solver);
            System.out.println(event.getNewBestScore());

            // This event listener is added before the best score event listener
            // so it is one step behind
            if (step.get() != -1) {
                assertThat(
                        meterRegistry.getMeasurement(SolverMetric.BEST_SCORE.getMeterId() + ".hard.score", "VALUE").intValue())
                        .isEqualTo(0);
            }
            if (step.get() == 0) {
                assertThat(
                        meterRegistry.getMeasurement(SolverMetric.BEST_SCORE.getMeterId() + ".soft.score", "VALUE").intValue())
                        .isEqualTo(0);
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
        });
        solution = solver.solve(solution);

        assertThat(step.get()).isEqualTo(2);
        meterRegistry.publish(solver);
        assertThat(solution).isNotNull();
        assertThat(meterRegistry.getMeasurement(SolverMetric.BEST_SCORE.getMeterId() + ".hard.score", "VALUE").intValue())
                .isEqualTo(0);
        assertThat(meterRegistry.getMeasurement(SolverMetric.BEST_SCORE.getMeterId() + ".soft.score", "VALUE").intValue())
                .isEqualTo(2);
    }

    private static class SetTestdataEntityValueCustomPhaseCommand implements CustomPhaseCommand<TestdataHardSoftScoreSolution> {
        final TestdataEntity entity;
        final TestdataValue value;

        public SetTestdataEntityValueCustomPhaseCommand(TestdataEntity entity, TestdataValue value) {
            this.entity = entity;
            this.value = value;
        }

        @Override
        public void changeWorkingSolution(ScoreDirector<TestdataHardSoftScoreSolution> scoreDirector) {
            TestdataEntity workingEntity = scoreDirector.lookUpWorkingObject(entity);
            TestdataValue workingValue = scoreDirector.lookUpWorkingObject(value);

            scoreDirector.beforeVariableChanged(workingEntity, "value");
            workingEntity.setValue(workingValue);
            scoreDirector.afterVariableChanged(workingEntity, "value");
            scoreDirector.triggerVariableListeners();
        }
    }

    @Test
    void solveStepScoreMetrics() {
        TestMeterRegistry meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataHardSoftScoreSolution.class, TestdataEntity.class);
        solverConfig.setScoreDirectorFactoryConfig(
                new ScoreDirectorFactoryConfig().withEasyScoreCalculatorClass(BestScoreMetricEasyScoreCalculator.class));
        solverConfig.setTerminationConfig(new TerminationConfig().withBestScoreLimit("0hard/3soft"));
        solverConfig.setMonitoringConfig(new MonitoringConfig()
                .withSolverMetricList(List.of(SolverMetric.STEP_SCORE)));

        TestdataHardSoftScoreSolution solution = new TestdataHardSoftScoreSolution("s1");
        TestdataEntity e1 = new TestdataEntity("e1");
        TestdataEntity e2 = new TestdataEntity("e2");
        TestdataEntity e3 = new TestdataEntity("e3");
        TestdataValue none = new TestdataValue("none");
        TestdataValue reward = new TestdataValue("reward");
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

        Solver<TestdataHardSoftScoreSolution> solver = solverFactory.buildSolver();
        ((DefaultSolver<TestdataHardSoftScoreSolution>) solver).setMonitorTagMap(Map.of("solver.id", "solveMetrics"));
        AtomicInteger step = new AtomicInteger(-1);

        ((DefaultSolver<TestdataHardSoftScoreSolution>) solver)
                .addPhaseLifecycleListener(new PhaseLifecycleListenerAdapter<TestdataHardSoftScoreSolution>() {
                    @Override
                    public void stepEnded(AbstractStepScope<TestdataHardSoftScoreSolution> stepScope) {
                        super.stepEnded(stepScope);
                        meterRegistry.publish(solver);

                        // first 3 steps are construction heuristic steps and don't have a step score since it uninitialized
                        if (step.get() < 2) {
                            step.incrementAndGet();
                            return;
                        }

                        assertThat(
                                meterRegistry.getMeasurement(SolverMetric.STEP_SCORE.getMeterId() + ".hard.score", "VALUE")
                                        .intValue())
                                .isEqualTo(0);

                        if (step.get() == 2) {
                            assertThat(
                                    meterRegistry.getMeasurement(SolverMetric.STEP_SCORE.getMeterId() + ".soft.score", "VALUE")
                                            .intValue())
                                    .isEqualTo(0);
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
        meterRegistry.publish(solver);
        assertThat(solution).isNotNull();
        assertThat(meterRegistry.getMeasurement(SolverMetric.STEP_SCORE.getMeterId() + ".hard.score", "VALUE").intValue())
                .isEqualTo(0);
        assertThat(meterRegistry.getMeasurement(SolverMetric.STEP_SCORE.getMeterId() + ".soft.score", "VALUE").intValue())
                .isEqualTo(3);
    }

    public static class ErrorThrowingEasyScoreCalculator implements EasyScoreCalculator<TestdataSolution, SimpleScore> {

        @Override
        public SimpleScore calculateScore(TestdataSolution testdataSolution) {
            throw new IllegalStateException("Thrown exception in constraint provider");
        }
    }

    @Test
    void solveMetricsError() {
        TestMeterRegistry meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class);

        solverConfig.setScoreDirectorFactoryConfig(
                new ScoreDirectorFactoryConfig().withEasyScoreCalculatorClass(ErrorThrowingEasyScoreCalculator.class));
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);

        Solver<TestdataSolution> solver = solverFactory.buildSolver();
        ((DefaultSolver<TestdataSolution>) solver).setMonitorTagMap(Map.of("solver.id", "solveMetricsError"));
        meterRegistry.publish(solver);

        TestdataSolution solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2")));

        meterRegistry.publish(solver);

        assertThatCode(() -> {
            solver.solve(solution);
        }).hasStackTraceContaining("Thrown exception in constraint provider");

        meterRegistry.getClock().addSeconds(1);
        meterRegistry.publish(solver);
        assertThat(meterRegistry.getMeasurement(SolverMetric.SOLVE_DURATION.getMeterId(), "ACTIVE_TASKS")).isZero();
        assertThat(meterRegistry.getMeasurement(SolverMetric.SOLVE_DURATION.getMeterId(), "DURATION")).isZero();
        assertThat(meterRegistry.getMeasurement(SolverMetric.ERROR_COUNT.getMeterId(), "COUNT")).isOne();
    }

    @Test
    void solveEmptyEntityList() {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new CustomPhaseConfig().withCustomPhaseCommands(
                        scoreDirector -> fail("All phases should be skipped because there are no movable entities.")));
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<TestdataSolution> solver = solverFactory.buildSolver();

        TestdataSolution solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Collections.emptyList());

        solution = solver.solve(solution);
        assertThat(solution).isNotNull();
        assertThat(solution.getScore().isSolutionInitialized()).isTrue();
    }

    @Test
    void solveChainedEmptyEntityList() {
        SolverConfig solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataChainedSolution.class, TestdataChainedEntity.class)
                .withPhases(new CustomPhaseConfig().withCustomPhaseCommands(
                        scoreDirector -> fail("All phases should be skipped because there are no movable entities.")));
        SolverFactory<TestdataChainedSolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<TestdataChainedSolution> solver = solverFactory.buildSolver();

        TestdataChainedSolution solution = new TestdataChainedSolution("s1");
        solution.setChainedAnchorList(Arrays.asList(new TestdataChainedAnchor("v1"), new TestdataChainedAnchor("v2")));
        solution.setChainedEntityList(Collections.emptyList());

        solution = solver.solve(solution);
        assertThat(solution).isNotNull();
        assertThat(solution.getScore().isSolutionInitialized()).isTrue();
    }

    @Test
    void solveEmptyEntityListAndEmptyValueList() {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new CustomPhaseConfig().withCustomPhaseCommands(
                        scoreDirector -> fail("All phases should be skipped because there are no movable entities.")));
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<TestdataSolution> solver = solverFactory.buildSolver();

        TestdataSolution solution = new TestdataSolution("s1");
        solution.setValueList(Collections.emptyList());
        solution.setEntityList(Collections.emptyList());

        solution = solver.solve(solution);
        assertThat(solution).isNotNull();
        assertThat(solution.getScore().isSolutionInitialized()).isTrue();
    }

    @Test
    void solvePinnedEntityList() {
        SolverConfig solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataPinnedSolution.class, TestdataPinnedEntity.class)
                .withPhases(new CustomPhaseConfig().withCustomPhaseCommands(
                        scoreDirector -> fail("All phases should be skipped because there are no movable entities.")));
        SolverFactory<TestdataPinnedSolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<TestdataPinnedSolution> solver = solverFactory.buildSolver();

        TestdataPinnedSolution solution = new TestdataPinnedSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataPinnedEntity("e1", true, false),
                new TestdataPinnedEntity("e2", false, true)));

        solution = solver.solve(solution);
        assertThat(solution).isNotNull();
        assertThat(solution.getScore().isSolutionInitialized()).isFalse();
    }

    @Test
    void solveStopsWhenUninitialized() {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class);
        CustomPhaseConfig phaseConfig = new CustomPhaseConfig()
                .withCustomPhaseCommandClassList(Collections.singletonList(NoChangeCustomPhaseCommand.class));
        solverConfig.setPhaseConfigList(Collections.singletonList(phaseConfig));
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<TestdataSolution> solver = solverFactory.buildSolver();

        TestdataSolution solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"),
                new TestdataEntity("e3"), new TestdataEntity("e4"), new TestdataEntity("e5")));

        solution = solver.solve(solution);
        assertThat(solution).isNotNull();
        assertThat(solution.getScore().isSolutionInitialized()).isFalse();
    }

    @Test
    void solveStopsWhenPartiallyInitialized() {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class);
        ConstructionHeuristicPhaseConfig phaseConfig = new ConstructionHeuristicPhaseConfig();
        // Run only 2 steps, although 5 are needed to initialize all entities
        phaseConfig.setTerminationConfig(new TerminationConfig().withStepCountLimit(2));
        solverConfig.setPhaseConfigList(Collections.singletonList(phaseConfig));
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<TestdataSolution> solver = solverFactory.buildSolver();

        TestdataSolution solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"),
                new TestdataEntity("e3"), new TestdataEntity("e4"), new TestdataEntity("e5")));

        solution = solver.solve(solution);
        assertThat(solution).isNotNull();
        assertThat(solution.getScore().isSolutionInitialized()).isFalse();
    }

    @Test
    @Timeout(60)
    void solveWithProblemChange() throws InterruptedException {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        solverConfig.setDaemon(true); // Avoid terminating the solver too quickly.
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<TestdataSolution> solver = solverFactory.buildSolver();
        final int valueCount = 4;
        TestdataSolution solution = TestdataSolution.generateSolution(valueCount, valueCount);

        AtomicReference<TestdataSolution> bestSolution = new AtomicReference<>();
        CountDownLatch solutionWithProblemChangeReceived = new CountDownLatch(1);
        solver.addEventListener(bestSolutionChangedEvent -> {
            if (bestSolutionChangedEvent.isEveryProblemChangeProcessed()) {
                TestdataSolution newBestSolution = bestSolutionChangedEvent.getNewBestSolution();
                if (newBestSolution.getValueList().size() == valueCount + 1) {
                    bestSolution.set(newBestSolution);
                    solutionWithProblemChangeReceived.countDown();
                }
            }
        });

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            solver.solve(solution);
        });

        solver.addProblemChange((workingSolution, problemChangeDirector) -> {
            problemChangeDirector.addProblemFact(new TestdataValue("added value"), solution.getValueList()::add);
        });

        solutionWithProblemChangeReceived.await();
        assertThat(bestSolution.get().getValueList()).hasSize(valueCount + 1);

        solver.terminateEarly();
        executorService.shutdown();
    }

    @Test
    void solveRepeatedlyBasicVariable(SoftAssertions softly) {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        ConstructionHeuristicPhaseConfig phaseConfig = new ConstructionHeuristicPhaseConfig();
        // Run only 2 steps at a time, although 5 are needed to initialize all entities.
        int stepCountLimit = 2;
        phaseConfig.setTerminationConfig(new TerminationConfig().withStepCountLimit(stepCountLimit));
        solverConfig.setPhaseConfigList(Collections.singletonList(phaseConfig));
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<TestdataSolution> solver = solverFactory.buildSolver();

        TestdataSolution solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        final int entityCount = 5;
        solution.setEntityList(IntStream.rangeClosed(1, entityCount)
                .mapToObj(id -> new TestdataEntity("e" + id))
                .collect(Collectors.toList()));

        Score<?> score = SolutionManager.create(solverFactory).update(solution);
        assertThat(score.initScore()).isEqualTo(-entityCount);
        assertThat(score.isSolutionInitialized()).isFalse();

        // Keep restarting the solver until the solution is initialized.
        for (int initScore = -entityCount; initScore < 0; initScore += stepCountLimit) {
            softly.assertThat(solution.getScore().initScore()).isEqualTo(initScore);
            softly.assertThat(solution.getScore().isSolutionInitialized()).isFalse();
            solution = solver.solve(solution);
        }

        // Finally, the initScore is 0.
        softly.assertThat(solution.getScore().initScore()).isZero();
        softly.assertThat(solution.getScore().isSolutionInitialized()).isTrue();
    }

    @Test
    void solveRepeatedlyListVariable(SoftAssertions softly) {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class);

        // Run only 7 steps at a time, although the total number of steps needed to complete CH is equal to maximumValueRangeSize.
        final int stepCountLimit = 7;
        ConstructionHeuristicPhaseConfig phaseConfig = new ConstructionHeuristicPhaseConfig();
        phaseConfig.setTerminationConfig(new TerminationConfig().withStepCountLimit(stepCountLimit));
        solverConfig.setPhaseConfigList(Collections.singletonList(phaseConfig));
        SolverFactory<TestdataListSolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<TestdataListSolution> solver = solverFactory.buildSolver();

        final int valueCount = 24;
        TestdataListSolution solution = TestdataListSolution.generateUninitializedSolution(valueCount, 8);

        Score<?> score = SolutionManager.create(solverFactory).update(solution);
        assertThat(score.initScore()).isEqualTo(-valueCount);
        assertThat(score.isSolutionInitialized()).isFalse();

        // Keep restarting the solver until the solution is initialized.
        for (int initScore = -valueCount; initScore < 0; initScore += stepCountLimit) {
            softly.assertThat(solution.getScore().initScore()).isEqualTo(initScore);
            softly.assertThat(solution.getScore().isSolutionInitialized()).isFalse();
            solution = solver.solve(solution);
        }

        // Finally, the initScore is 0.
        softly.assertThat(solution.getScore().initScore()).isZero();
        softly.assertThat(solution.getScore().isSolutionInitialized()).isTrue();
    }

    @Test
    void solveWithAllowsUnassignedValuesListVariable() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataAllowsUnassignedValuesListSolution.class,
                TestdataAllowsUnassignedValuesListEntity.class, TestdataAllowsUnassignedValuesListValue.class)
                .withEasyScoreCalculatorClass(TestdataAllowsUnassignedValuesListEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig().withBestScoreLimit("0"))
                .withPhases();
        var solverFactory = SolverFactory.<TestdataAllowsUnassignedValuesListSolution> create(solverConfig);
        var solver = solverFactory.buildSolver();

        var value1 = new TestdataAllowsUnassignedValuesListValue("v1");
        var value2 = new TestdataAllowsUnassignedValuesListValue("v2");
        var value3 = new TestdataAllowsUnassignedValuesListValue("v3");
        var value4 = new TestdataAllowsUnassignedValuesListValue("v4");
        var entity = TestdataAllowsUnassignedValuesListEntity.createWithValues("e1", value1, value2);

        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(Arrays.asList(value1, value2, value3, value4));

        var bestSolution = solver.solve(solution);
        assertSoftly(softly -> {
            softly.assertThat(bestSolution.getScore()).isEqualTo(SimpleScore.of(0)); // Nothing is assigned.
            var firstEntity = bestSolution.getEntityList().get(0);
            softly.assertThat(firstEntity.getValueList()).isEmpty();
        });

    }

    @Test
    void constructionHeuristicAllocateToValueFromQueue() {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        ConstructionHeuristicPhaseConfig phaseConfig = new ConstructionHeuristicPhaseConfig()
                .withConstructionHeuristicType(ConstructionHeuristicType.ALLOCATE_TO_VALUE_FROM_QUEUE);
        solverConfig.setPhaseConfigList(Collections.singletonList(phaseConfig));
        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<TestdataSolution> solver = solverFactory.buildSolver();

        TestdataSolution solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1")));

        solution = solver.solve(solution);
        assertThat(solution).isNotNull();
        assertThat(solution.getScore().isSolutionInitialized()).isTrue();
    }

    @Test
    void solveWithMultipleGenuinePlanningEntities() {
        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(TestdataMultiEntitySolution.class)
                .withEntityClasses(TestdataLeadEntity.class, TestdataHerdEntity.class)
                .withEasyScoreCalculatorClass(DummySimpleScoreEasyScoreCalculator.class)
                .withTerminationConfig(new TerminationConfig().withBestScoreLimit("0"));
        SolverFactory<TestdataMultiEntitySolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<TestdataMultiEntitySolution> solver = solverFactory.buildSolver();

        TestdataMultiEntitySolution solution = new TestdataMultiEntitySolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setLeadEntityList(Arrays.asList(new TestdataLeadEntity("lead1"), new TestdataLeadEntity("lead2")));
        solution.setHerdEntityList(Arrays.asList(new TestdataHerdEntity("herd1"), new TestdataHerdEntity("herd2")));

        solution = solver.solve(solution);
        assertThat(solution).isNotNull();
        assertThat(solution.getScore().isSolutionInitialized()).isTrue();
    }

    /**
     * Verifies <a href="https://issues.redhat.com/browse/PLANNER-2798">PLANNER-2798</a>.
     */
    @Test
    void solveWithMultipleChainedPlanningEntities() {
        SolverConfig solverConfig = new SolverConfig()
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
        Solver<TestdataChainedMultiEntitySolution> solver = solverFactory.buildSolver();

        List<TestdataChainedMultiEntityAnchor> anchors = List.of(
                new TestdataChainedMultiEntityAnchor("a1"),
                new TestdataChainedMultiEntityAnchor("a2"),
                new TestdataChainedMultiEntityAnchor("a3"));
        List<TestdataChainedBrownEntity> brownEntities = List.of(
                new TestdataChainedBrownEntity("b1"),
                new TestdataChainedBrownEntity("b2"));
        List<TestdataChainedGreenEntity> greenEntities = List.of(
                new TestdataChainedGreenEntity("g1"),
                new TestdataChainedGreenEntity("g2"),
                new TestdataChainedGreenEntity("g3"));
        TestdataChainedMultiEntitySolution solution =
                new TestdataChainedMultiEntitySolution(brownEntities, greenEntities, anchors);

        solution = solver.solve(solution);
        assertThat(solution).isNotNull();
        assertThat(solution.getScore().isSolutionInitialized()).isTrue();
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
        int actualValueCount = solution.getEntityList().stream()
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
        int actualValueCount = solution.getEntityList().stream()
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
        int actualValueCount = solution.getEntityList().stream()
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

    public static final class MinimizeUnusedEntitiesEasyScoreCalculator
            implements EasyScoreCalculator<Object, SimpleScore> {

        @Override
        public SimpleScore calculateScore(Object solution) {
            return new MaximizeUnusedEntitiesEasyScoreCalculator().calculateScore(solution).negate();
        }
    }

    public static final class MaximizeUnusedEntitiesEasyScoreCalculator
            implements EasyScoreCalculator<Object, SimpleScore> {

        @Override
        public SimpleScore calculateScore(Object solution) {
            if (solution instanceof TestdataPinnedListSolution testdataPinnedListSolution) {
                int unusedEntities = 0;
                for (var entity : testdataPinnedListSolution.getEntityList()) {
                    if (entity.getValueList().isEmpty()) {
                        unusedEntities++;
                    }
                }
                return SimpleScore.of(unusedEntities);
            } else if (solution instanceof TestdataPinnedWithIndexListSolution testdataPinnedWithIndexListSolution) {
                int unusedEntities = 0;
                for (var entity : testdataPinnedWithIndexListSolution.getEntityList()) {
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
        public SimpleScore calculateScore(TestdataSolution testdataSolution) {
            int random = (int) (Math.random() * 1000);
            return SimpleScore.of(random);
        }
    }

    public static class CorruptedIncrementalScoreCalculator
            implements ConstraintMatchAwareIncrementalScoreCalculator<TestdataSolution, SimpleScore> {

        @Override
        public void resetWorkingSolution(TestdataSolution workingSolution, boolean constraintMatchEnabled) {

        }

        @Override
        public Collection<ConstraintMatchTotal<SimpleScore>> getConstraintMatchTotals() {
            return Collections.singletonList(new DefaultConstraintMatchTotal<>(ConstraintRef.of("a", "b"), SimpleScore.of(1)));
        }

        @Override
        public Map<Object, Indictment<SimpleScore>> getIndictmentMap() {
            return Collections.singletonMap(new TestdataEntity("e1"),
                    new DefaultIndictment<>(new TestdataEntity("e1"), SimpleScore.ONE));
        }

        @Override
        public void resetWorkingSolution(TestdataSolution workingSolution) {

        }

        @Override
        public void beforeEntityAdded(Object entity) {

        }

        @Override
        public void afterEntityAdded(Object entity) {

        }

        @Override
        public void beforeVariableChanged(Object entity, String variableName) {

        }

        @Override
        public void afterVariableChanged(Object entity, String variableName) {

        }

        @Override
        public void beforeEntityRemoved(Object entity) {

        }

        @Override
        public void afterEntityRemoved(Object entity) {

        }

        @Override
        public SimpleScore calculateScore() {
            int random = (int) (Math.random() * 1000);
            return SimpleScore.of(random);
        }
    }

}
