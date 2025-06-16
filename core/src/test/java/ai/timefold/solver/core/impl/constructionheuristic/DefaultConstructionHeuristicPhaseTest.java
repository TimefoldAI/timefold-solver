package ai.timefold.solver.core.impl.constructionheuristic;

import static ai.timefold.solver.core.config.solver.PreviewFeature.DECLARATIVE_SHADOW_VARIABLES;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicType;
import ai.timefold.solver.core.config.solver.monitoring.MonitoringConfig;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.solver.DefaultSolver;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedEntity;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedOtherValue;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedSolution;
import ai.timefold.solver.core.testdomain.mixed.singleentity.TestdataMixedValue;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedEntity;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedSolution;
import ai.timefold.solver.core.testdomain.pinned.unassignedvar.TestdataPinnedAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.pinned.unassignedvar.TestdataPinnedAllowsUnassignedSolution;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;
import ai.timefold.solver.core.testutil.AbstractMeterTest;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Metrics;

class DefaultConstructionHeuristicPhaseTest extends AbstractMeterTest {

    @Test
    void solveWithInitializedEntities() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());

        var solution = new TestdataSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Arrays.asList(
                new TestdataEntity("e1", null),
                new TestdataEntity("e2", v2),
                new TestdataEntity("e3", v1)));

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
        var solvedE1 = solution.getEntityList().get(0);
        assertCode("e1", solvedE1);
        assertThat(solvedE1.getValue()).isNotNull();
        var solvedE2 = solution.getEntityList().get(1);
        assertCode("e2", solvedE2);
        assertThat(solvedE2.getValue()).isEqualTo(v2);
        var solvedE3 = solution.getEntityList().get(2);
        assertCode("e3", solvedE3);
        assertThat(solvedE3.getValue()).isEqualTo(v1);
    }

    @Test
    void solveWithInitializedSolution() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());

        var inputProblem = new TestdataSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        inputProblem.setValueList(Arrays.asList(v1, v2, v3));
        inputProblem.setEntityList(Arrays.asList(
                new TestdataEntity("e1", v1),
                new TestdataEntity("e2", v2),
                new TestdataEntity("e3", v3)));

        var solution = PlannerTestUtils.solve(solverConfig, inputProblem, false);
        assertThat(inputProblem).isSameAs(solution);
    }

    @Test
    void solveWithCustomMetrics() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig())
                .withMonitoringConfig(new MonitoringConfig().withSolverMetricList(List.of(SolverMetric.MOVE_COUNT_PER_TYPE)));

        var problem = new TestdataSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        problem.setValueList(Arrays.asList(v1, v2, v3));
        problem.setEntityList(Arrays.asList(
                new TestdataEntity("e1", null),
                new TestdataEntity("e2", v2),
                new TestdataEntity("e3", v1)));

        SolverFactory<TestdataSolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<TestdataSolution> solver = solverFactory.buildSolver();
        var moveCountPerChange = new AtomicLong();
        ((DefaultSolver<TestdataSolution>) solver).addPhaseLifecycleListener(new PhaseLifecycleListenerAdapter<>() {
            @Override
            public void solvingEnded(SolverScope<TestdataSolution> solverScope) {
                meterRegistry.publish();
                var changeMoveKey = "ChangeMove(TestdataEntity.value)";
                if (solverScope.getMoveCountTypes().contains(changeMoveKey)) {
                    var counter = meterRegistry
                            .getMeasurement(SolverMetric.MOVE_COUNT_PER_TYPE.getMeterId() + "." + changeMoveKey, "VALUE");
                    moveCountPerChange.set(counter.longValue());
                }
            }
        });
        solver.solve(problem);
        assertThat(moveCountPerChange.get()).isPositive();
    }

    @Test
    void solveWithListVariableAndCustomMetrics() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class)
                .withPhases(new ConstructionHeuristicPhaseConfig())
                .withMonitoringConfig(new MonitoringConfig().withSolverMetricList(List.of(SolverMetric.MOVE_COUNT_PER_TYPE)));

        var problem = new TestdataListSolution();
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");
        problem.setValueList(Arrays.asList(v1, v2, v3));
        problem.setEntityList(List.of(new TestdataListEntity("e1", new ArrayList<>())));

        SolverFactory<TestdataListSolution> solverFactory = SolverFactory.create(solverConfig);
        Solver<TestdataListSolution> solver = solverFactory.buildSolver();
        var moveCount = new AtomicLong();
        ((DefaultSolver<TestdataListSolution>) solver).addPhaseLifecycleListener(new PhaseLifecycleListenerAdapter<>() {
            @Override
            public void solvingEnded(SolverScope<TestdataListSolution> solverScope) {
                meterRegistry.publish();
                var changeMoveKey = "ListAssignMove(TestdataListEntity.valueList)";
                if (solverScope.getMoveCountTypes().contains(changeMoveKey)) {
                    var counter = meterRegistry
                            .getMeasurement(SolverMetric.MOVE_COUNT_PER_TYPE.getMeterId() + "." + changeMoveKey, "VALUE");
                    moveCount.set(counter.longValue());
                }
            }
        });
        solver.solve(problem);
        assertThat(moveCount.get()).isPositive();
    }

    @Test
    void solveWithPinnedEntities() {
        var solverConfig =
                PlannerTestUtils.buildSolverConfig(TestdataPinnedSolution.class, TestdataPinnedEntity.class)
                        .withPhases(new ConstructionHeuristicPhaseConfig());

        var solution = new TestdataPinnedSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Arrays.asList(
                new TestdataPinnedEntity("e1", null, false, false),
                new TestdataPinnedEntity("e2", v2, true, false),
                new TestdataPinnedEntity("e3", v3, false, true)));

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
        var solvedE1 = solution.getEntityList().get(0);
        assertCode("e1", solvedE1);
        assertThat(solvedE1.getValue()).isNotNull();
        var solvedE2 = solution.getEntityList().get(1);
        assertCode("e2", solvedE2);
        assertThat(solvedE2.getValue()).isEqualTo(v2);
        var solvedE3 = solution.getEntityList().get(2);
        assertCode("e3", solvedE3);
        assertThat(solvedE3.getValue()).isEqualTo(v3);
        assertThat(solution.getScore()).isEqualTo(SimpleScore.ZERO);
    }

    @Test
    void solveWithPinnedEntitiesWhenUnassignedAllowedAndPinnedToNull() {
        var solverConfig =
                PlannerTestUtils.buildSolverConfig(TestdataPinnedAllowsUnassignedSolution.class,
                        TestdataPinnedAllowsUnassignedEntity.class)
                        .withPhases(new ConstructionHeuristicPhaseConfig());

        var solution = new TestdataPinnedAllowsUnassignedSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Arrays.asList(
                new TestdataPinnedAllowsUnassignedEntity("e1", null, false, false),
                new TestdataPinnedAllowsUnassignedEntity("e2", v2, true, false),
                new TestdataPinnedAllowsUnassignedEntity("e3", null, false, true)));

        solution = PlannerTestUtils.solve(solverConfig, solution, false); // No change will be made.
        assertThat(solution).isNotNull();
        assertThat(solution.getScore()).isEqualTo(SimpleScore.ZERO);
    }

    @Test
    void solveWithPinnedEntitiesWhenUnassignedNotAllowedAndPinnedToNull() {
        var solverConfig =
                PlannerTestUtils.buildSolverConfig(TestdataPinnedSolution.class, TestdataPinnedEntity.class)
                        .withPhases(new ConstructionHeuristicPhaseConfig());

        var solution = new TestdataPinnedSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Arrays.asList(
                new TestdataPinnedEntity("e1", null, false, false),
                new TestdataPinnedEntity("e2", v2, true, false),
                new TestdataPinnedEntity("e3", null, false, true)));

        assertThatThrownBy(() -> PlannerTestUtils.solve(solverConfig, solution))
                .hasMessageContaining("entity (e3)")
                .hasMessageContaining("variable (value");
    }

    @Test
    void solveWithEmptyEntityList() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());

        var solution = new TestdataSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Collections.emptyList());

        solution = PlannerTestUtils.solve(solverConfig, solution, false);
        assertThat(solution).isNotNull();
        assertThat(solution.getEntityList()).isEmpty();
    }

    @Test
    void solveWithAllowsUnassignedBasicVariable() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataAllowsUnassignedSolution.class,
                TestdataAllowsUnassignedEntity.class)
                .withEasyScoreCalculatorClass(TestdataAllowsUnassignedEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());

        var value1 = new TestdataValue("v1");
        var value2 = new TestdataValue("v2");
        var entity = new TestdataAllowsUnassignedEntity("e1");
        entity.setValue(value1);
        var entity2 = new TestdataAllowsUnassignedEntity("e2");
        var entity3 = new TestdataAllowsUnassignedEntity("e3");

        var solution = new TestdataAllowsUnassignedSolution();
        solution.setEntityList(List.of(entity, entity2, entity3));
        solution.setValueList(Arrays.asList(value1, value2));

        var bestSolution = PlannerTestUtils.solve(solverConfig, solution);
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
    void solveWithAllowsUnassignedValuesListVariable() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataAllowsUnassignedValuesListSolution.class,
                TestdataAllowsUnassignedValuesListEntity.class, TestdataAllowsUnassignedValuesListValue.class)
                .withEasyScoreCalculatorClass(TestdataAllowsUnassignedValuesListEasyScoreCalculator.class)
                .withPhases(new ConstructionHeuristicPhaseConfig());

        var value1 = new TestdataAllowsUnassignedValuesListValue("v1");
        var value2 = new TestdataAllowsUnassignedValuesListValue("v2");
        var value3 = new TestdataAllowsUnassignedValuesListValue("v3");
        var value4 = new TestdataAllowsUnassignedValuesListValue("v4");
        var entity = TestdataAllowsUnassignedValuesListEntity.createWithValues("e1", value1, value2);

        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(Arrays.asList(value1, value2, value3, value4));

        var bestSolution = PlannerTestUtils.solve(solverConfig, solution, false);
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
    void constructionHeuristicAllocateToValueFromQueue() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig()
                        .withConstructionHeuristicType(ConstructionHeuristicType.ALLOCATE_TO_VALUE_FROM_QUEUE));

        var solution = new TestdataSolution("s1");
        solution.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));
        solution.setEntityList(Arrays.asList(new TestdataEntity("e1")));

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
        assertThat(solution.getEntityList().stream()
                .filter(e -> e.getValue() == null)).isEmpty();
    }

    @Test
    void failMixedModelDefaultConfiguration() {
        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataMixedSolution.class, TestdataMixedEntity.class, TestdataMixedValue.class,
                        TestdataMixedOtherValue.class)
                .withPreviewFeature(DECLARATIVE_SHADOW_VARIABLES);

        assertThatCode(() -> PlannerTestUtils.solve(solverConfig, new TestdataSolution("s1")))
                .hasMessageContaining(
                        "has both basic and list variables and cannot be deduced automatically");
    }
}
