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

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchPhaseConfig;
import ai.timefold.solver.core.impl.exhaustivesearch.decider.BasicExhaustiveSearchDecider;
import ai.timefold.solver.core.impl.exhaustivesearch.decider.ListVariableExhaustiveSearchDecider;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchLayer;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchPhaseScope;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchStepScope;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.move.MoveDirector;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.valuerange.pinned.TestdataListPinnedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.pinned.TestdataListPinnedEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedEntity;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedSolution;
import ai.timefold.solver.core.testdomain.pinned.unassignedvar.TestdataPinnedAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.pinned.unassignedvar.TestdataPinnedAllowsUnassignedSolution;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class DefaultExhaustiveSearchPhaseTest {

    @SuppressWarnings({ "unchecked" })
    @Test
    void restoreWorkingSolutionForBasicVariable() {
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
        when(phaseScope.getScoreDirector()).thenReturn(scoreDirector);

        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        when(phaseScope.getSolutionDescriptor()).thenReturn(solutionDescriptor);

        var layer0 = new ExhaustiveSearchLayer(0, mock(Object.class));
        var layer1 = new ExhaustiveSearchLayer(1, mock(Object.class));
        var layer2 = new ExhaustiveSearchLayer(2, mock(Object.class));
        var layer3 = new ExhaustiveSearchLayer(3, mock(Object.class));
        var layer4 = new ExhaustiveSearchLayer(4, mock(Object.class));
        var node0 = createNode(layer0, null, true);
        var node1 = createNode(layer1, node0, true);
        var node2A = createNode(layer2, node1, true);
        var node3A = createNode(layer3, node2A, true); // oldNode
        var node2B = createNode(layer2, node1, true);
        var node3B = createNode(layer3, node2B, true);
        var node4B = createNode(layer4, node3B, true); // newNode
        node4B.setScore(InnerScore.withUnassignedCount(SimpleScore.of(7), 96));
        when(lastCompletedStepScope.getExpandingNode()).thenReturn(node3A);
        when(stepScope.getExpandingNode()).thenReturn(node4B);

        var decider = new BasicExhaustiveSearchDecider<TestdataSolution, SimpleScore>("", null, null,
                mock(EntitySelector.class), null, null,
                false, null);
        decider.restoreWorkingSolution(stepScope, false, false);

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

    @SuppressWarnings({ "unchecked" })
    @Test
    void restoreWorkingSolutionForListVariable() {
        var phaseScope = mock(ExhaustiveSearchPhaseScope.class);
        var lastCompletedStepScope = mock(ExhaustiveSearchStepScope.class);
        when(phaseScope.getLastCompletedStepScope()).thenReturn(lastCompletedStepScope);
        var stepScope = mock(ExhaustiveSearchStepScope.class);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        var workingSolution = new TestdataSolution();
        when(phaseScope.getWorkingSolution()).thenReturn(workingSolution);
        InnerScoreDirector<TestdataListSolution, SimpleScore> scoreDirector = mock(InnerScoreDirector.class);
        var moveDirector = new MoveDirector<>(scoreDirector);
        doAnswer(invocation -> {
            var move = (Move<TestdataListSolution>) invocation.getArgument(0);
            moveDirector.execute(move);
            return null;
        }).when(scoreDirector)
                .executeMove(any());
        when(phaseScope.getScoreDirector()).thenReturn(scoreDirector);
        when(phaseScope.calculateScore()).thenReturn(InnerScore.withUnassignedCount(SimpleScore.of(7), 96));

        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        when(phaseScope.getSolutionDescriptor()).thenReturn(solutionDescriptor);

        var layer0 = new ExhaustiveSearchLayer(0, mock(Object.class));
        var layer1 = new ExhaustiveSearchLayer(1, mock(Object.class));
        var layer2 = new ExhaustiveSearchLayer(2, mock(Object.class));
        var layer3 = new ExhaustiveSearchLayer(3, mock(Object.class));
        var layer4 = new ExhaustiveSearchLayer(4, mock(Object.class));
        var node0 = createNode(layer0, null, false);
        var node1 = createNode(layer1, node0, true);
        var node2A = createNode(layer2, node1, true);
        var node2B = createNode(layer2, node1, true);
        var node3A = createNode(layer3, node2B, true);
        var node3B = createNode(layer3, node3A, true);
        var node4B = createNode(layer4, node3B, true);
        node4B.setScore(InnerScore.withUnassignedCount(SimpleScore.of(7), 96));
        when(lastCompletedStepScope.getExpandingNode()).thenReturn(node3B);
        when(stepScope.getExpandingNode()).thenReturn(node4B);

        var decider = new ListVariableExhaustiveSearchDecider<TestdataListSolution, SimpleScore>("", null, null,
                mock(EntitySelector.class), null, null, false, null);
        decider.restoreWorkingSolution(stepScope, false, false);

        verify(node2A.getMove(), times(0)).execute(any(MutableSolutionView.class));
        verify(node2A.getUndoMove(), times(0)).execute(any(MutableSolutionView.class));

        verify(node1.getMove(), times(1)).execute(any(MutableSolutionView.class));
        verify(node1.getUndoMove(), times(1)).execute(any(MutableSolutionView.class));
        verify(node3A.getMove(), times(1)).execute(any(MutableSolutionView.class));
        verify(node3A.getUndoMove(), times(1)).execute(any(MutableSolutionView.class));
        verify(node2B.getMove(), times(1)).execute(any(MutableSolutionView.class));
        verify(node2B.getUndoMove(), times(1)).execute(any(MutableSolutionView.class));
        verify(node3B.getMove(), times(1)).execute(any(MutableSolutionView.class));
        verify(node3B.getUndoMove(), times(1)).execute(any(MutableSolutionView.class));
        verify(node4B.getMove(), times(1)).execute(any(MutableSolutionView.class));
        verify(node4B.getUndoMove(), times(0)).execute(any(MutableSolutionView.class));
    }

    <Solution_> ExhaustiveSearchNode<Solution_> createNode(ExhaustiveSearchLayer layer,
            ExhaustiveSearchNode<Solution_> parentNode, boolean addMoves) {
        var node = new ExhaustiveSearchNode<>(layer, parentNode);
        if (addMoves) {
            node.setMove(mock(Move.class));
            node.setUndoMove(mock(Move.class));
        }
        return node;
    }

    @Test
    void solveBasicVariableWithInitializedEntities() {
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
    void solveListVariableWithInitializedEntities() {
        var solverConfig = PlannerTestUtils.buildSolverConfig(TestdataListSolution.class,
                TestdataListEntity.class, TestdataListValue.class);
        solverConfig.setPhaseConfigList(Collections.singletonList(new ExhaustiveSearchPhaseConfig()));

        var solution = new TestdataListSolution();
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Arrays.asList(
                new TestdataListEntity("e1"),
                new TestdataListEntity("e2", v2),
                new TestdataListEntity("e3", v1)));

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
        var solvedV1 = solution.getValueList().get(0);
        var solvedV2 = solution.getValueList().get(1);
        var solvedV3 = solution.getValueList().get(2);
        var solvedE1 = solution.getEntityList().get(0);
        assertCode("e1", solvedE1);
        assertThat(solvedE1.getValueList())
                .hasSize(1)
                .contains(solvedV3);
        var solvedE2 = solution.getEntityList().get(1);
        assertCode("e2", solvedE2);
        assertThat(solvedE2.getValueList())
                .hasSize(1)
                .contains(solvedV2);
        var solvedE3 = solution.getEntityList().get(2);
        assertCode("e3", solvedE3);
        assertThat(solvedE3.getValueList())
                .hasSize(1)
                .contains(solvedV1);
    }

    @Test
    void solveBasicWithPinnedEntities() {
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
    void solveListWithPinnedEntities() {
        var solverConfig = PlannerTestUtils
                .buildSolverConfig(TestdataListPinnedEntityProvidingSolution.class,
                        TestdataListPinnedEntityProvidingEntity.class)
                .withPhases(new ExhaustiveSearchPhaseConfig());

        var solution = new TestdataListPinnedEntityProvidingSolution();
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        var v4 = new TestdataValue("v4");
        var v5 = new TestdataValue("v5");
        var e1 = new TestdataListPinnedEntityProvidingEntity("e1", List.of(v1, v2, v3));
        e1.setValueList(List.of(v2, v1));
        // Pin entire entity
        e1.setPinned(true);
        var e2 = new TestdataListPinnedEntityProvidingEntity("e2", List.of(v1, v4, v5));
        e2.setValueList(List.of(v5, v4));
        // Pin the two first elements
        e2.setPlanningPinToIndex(2);
        var e3 = new TestdataListPinnedEntityProvidingEntity("e3", List.of(v3, v4, v5));
        solution.setEntityList(List.of(e1, e2, e3));

        solution = PlannerTestUtils.solve(solverConfig, solution);
        assertThat(solution).isNotNull();
        var solvedE1 = solution.getEntityList().get(0);
        var solvedV1 = solvedE1.getValueRange().get(0);
        var solvedV2 = solvedE1.getValueRange().get(1);
        assertCode("e1", solvedE1);
        // No new value added
        assertThat(solvedE1.getValueList())
                .hasSize(2)
                .contains(solvedV2, solvedV1);
        // The unassigned value is not reachable by e2 and the two first elements are pinned
        var solvedE2 = solution.getEntityList().get(1);
        var solvedV4 = solvedE2.getValueRange().get(1);
        var solvedV5 = solvedE2.getValueRange().get(2);
        assertCode("e2", solvedE2);
        // No new value added
        assertThat(solvedE2.getValueList())
                .hasSize(2)
                .contains(solvedV5, solvedV4);
        // v3 can only be assigned to e3
        var solvedE3 = solution.getEntityList().get(2);
        var solvedV3 = solvedE3.getValueRange().get(0);
        assertCode("e3", solvedE3);
        // No new value added
        assertThat(solvedE3.getValueList())
                .hasSize(1)
                .contains(solvedV3);
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

        solution = PlannerTestUtils.solve(solverConfig, solution, true); // No change will be made, but shadows will be updated.
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
    void solveBasicVariableWithEmptyEntityList() {
        var solverConfig =
                PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
        solverConfig.setPhaseConfigList(Collections.singletonList(new ExhaustiveSearchPhaseConfig()));

        var solution = new TestdataSolution("s1");
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Collections.emptyList());

        solution = PlannerTestUtils.solve(solverConfig, solution, true);
        assertThat(solution).isNotNull();
        assertThat(solution.getEntityList()).isEmpty();
    }

    @Test
    void solveListVariableWithEmptyEntityList() {
        var solverConfig =
                PlannerTestUtils.buildSolverConfig(TestdataListSolution.class, TestdataListEntity.class,
                        TestdataListValue.class);
        solverConfig.setPhaseConfigList(Collections.singletonList(new ExhaustiveSearchPhaseConfig()));

        var solution = new TestdataListSolution();
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");
        solution.setValueList(Arrays.asList(v1, v2, v3));
        solution.setEntityList(Collections.emptyList());

        solution = PlannerTestUtils.solve(solverConfig, solution, false);
        assertThat(solution).isNotNull();
        assertThat(solution.getEntityList()).isEmpty();
    }

}
