package ai.timefold.solver.core.impl.exhaustivesearch;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.monitoring.MonitoringConfig;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.exhaustivesearch.decider.ExhaustiveSearchDecider;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchLayer;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchPhaseScope;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchStepScope;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.move.director.MoveDirector;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.DefaultSolver;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedEntity;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedSolution;
import ai.timefold.solver.core.testdomain.pinned.unassignedvar.TestdataPinnedAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.pinned.unassignedvar.TestdataPinnedAllowsUnassignedSolution;
import ai.timefold.solver.core.testutil.AbstractMeterTest;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Metrics;

class DefaultExhaustiveSearchPhaseTest extends AbstractMeterTest {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void restoreWorkingSolution() {
        var phaseScope = mock(ExhaustiveSearchPhaseScope.class);
        var lastCompletedStepScope = mock(ExhaustiveSearchStepScope.class);
        when(phaseScope.getLastCompletedStepScope()).thenReturn(lastCompletedStepScope);
        var stepScope = mock(ExhaustiveSearchStepScope.class);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        var workingSolution = new TestdataSolution();
        when(phaseScope.getWorkingSolution()).thenReturn(workingSolution);
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector = mock(InnerScoreDirector.class);
        var moveDirector = new MoveDirector<>(scoreDirector);
        doAnswer(invocation -> {
            var move = (Move<TestdataSolution>) invocation.getArgument(0);
            moveDirector.execute(move);
            return null;
        }).when(scoreDirector)
                .executeMove(any());
        when(phaseScope.getScoreDirector()).thenReturn((InnerScoreDirector) scoreDirector);

        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        when(phaseScope.getSolutionDescriptor()).thenReturn(solutionDescriptor);

        var layer0 = new ExhaustiveSearchLayer(0, mock(Object.class));
        var layer1 = new ExhaustiveSearchLayer(1, mock(Object.class));
        var layer2 = new ExhaustiveSearchLayer(2, mock(Object.class));
        var layer3 = new ExhaustiveSearchLayer(3, mock(Object.class));
        var layer4 = new ExhaustiveSearchLayer(4, mock(Object.class));
        var node0 = createNode(layer0, null);
        var node1 = createNode(layer1, node0);
        var node2A = createNode(layer2, node1);
        var node3A = createNode(layer3, node2A); // oldNode
        var node2B = createNode(layer2, node1);
        var node3B = createNode(layer3, node2B);
        var node4B = createNode(layer4, node3B); // newNode
        node4B.setScore(InnerScore.withUnassignedCount(SimpleScore.of(7), 96));
        when(lastCompletedStepScope.getExpandingNode()).thenReturn(node3A);
        when(stepScope.getExpandingNode()).thenReturn(node4B);

        DefaultExhaustiveSearchPhase<TestdataSolution> phase = new DefaultExhaustiveSearchPhase.Builder<>(0, "", null, null,
                mock(EntitySelector.class), mock(ExhaustiveSearchDecider.class)).build();
        phase.restoreWorkingSolution(stepScope);

        verify(node0.getMove(), times(0)).execute(any(MutableSolutionView.class));
        verify(node0.getUndoMove(), times(0)).execute(any(MutableSolutionView.class));
        verify(node1.getMove(), times(0)).execute(any(MutableSolutionView.class));
        verify(node1.getUndoMove(), times(0)).execute(any(MutableSolutionView.class));
        verify(node2A.getMove(), times(0)).execute(any(MutableSolutionView.class));
        verify(node2A.getUndoMove(), times(1)).execute(any(MutableSolutionView.class));
        verify(node3A.getMove(), times(0)).execute(any(MutableSolutionView.class));
        verify(node3A.getUndoMove(), times(1)).execute(any(MutableSolutionView.class));
        verify(node2B.getMove(), times(1)).execute(any(MutableSolutionView.class));
        verify(node2B.getUndoMove(), times(0)).execute(any(MutableSolutionView.class));
        verify(node3B.getMove(), times(1)).execute(any(MutableSolutionView.class));
        verify(node3B.getUndoMove(), times(0)).execute(any(MutableSolutionView.class));
        verify(node4B.getMove(), times(1)).execute(any(MutableSolutionView.class));
        verify(node4B.getUndoMove(), times(0)).execute(any(MutableSolutionView.class));
    }

    ExhaustiveSearchNode createNode(ExhaustiveSearchLayer layer, ExhaustiveSearchNode parentNode) {
        var node = new ExhaustiveSearchNode(layer, parentNode);
        node.setMove(mock(Move.class));
        node.setUndoMove(mock(Move.class));
        return node;
    }

    @Test
    void solveWithInitializedEntities() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class,
                TestdataEntity.class);
        solverConfig.setPhaseConfigList(Collections.singletonList(new ExhaustiveSearchPhaseConfig()));

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
    void solveWithInitializedEntitiesAndMetric() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class,
                TestdataEntity.class);
        solverConfig.setPhaseConfigList(Collections.singletonList(new ExhaustiveSearchPhaseConfig()));

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
        AtomicReference<TestdataSolution> eventBestSolutionRef = new AtomicReference<>();
        solver.addEventListener(event -> eventBestSolutionRef.set(event.getNewBestSolution()));
        TestdataSolution solution = solver.solve(problem);
        assertThat(eventBestSolutionRef).doesNotHaveNullValue();

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

        SolverMetric.MOVE_EVALUATION_COUNT.register(solver);
        SolverMetric.SCORE_CALCULATION_COUNT.register(solver);
        meterRegistry.publish();
        var scoreCount = meterRegistry.getMeasurement(SolverMetric.SCORE_CALCULATION_COUNT.getMeterId(), "VALUE");
        var moveCount = meterRegistry.getMeasurement(SolverMetric.MOVE_EVALUATION_COUNT.getMeterId(), "VALUE");
        assertThat(scoreCount).isPositive();
        assertThat(moveCount).isPositive();
    }

    @Test
    void solveCustomMetrics() {
        var meterRegistry = new TestMeterRegistry();
        Metrics.addRegistry(meterRegistry);

        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataSolution.class,
                TestdataEntity.class);
        solverConfig.withPhases(new ExhaustiveSearchPhaseConfig())
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
    void solveWithPinnedEntities() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataPinnedSolution.class, TestdataPinnedEntity.class)
                .withPhases(new ExhaustiveSearchPhaseConfig());

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
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataPinnedAllowsUnassignedSolution.class,
                TestdataPinnedAllowsUnassignedEntity.class)
                .withPhases(new ExhaustiveSearchPhaseConfig());

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
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataPinnedSolution.class, TestdataPinnedEntity.class)
                .withPhases(new ExhaustiveSearchPhaseConfig());

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
        var solverConfig =
                PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        solverConfig.setPhaseConfigList(Collections.singletonList(new ExhaustiveSearchPhaseConfig()));

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

}
