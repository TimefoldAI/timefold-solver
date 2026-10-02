package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.unassignedvar.TestdataPinnedAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.unassignedvar.TestdataPinnedAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class ListUnassignMoveProviderTest {

    @Test
    void pinnedEntitySkipped() {
        var solutionMetaModel = TestdataPinnedAllowsUnassignedValuesListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedAllowsUnassignedValuesListEntity.class)
                .listVariable();

        var solution = TestdataPinnedAllowsUnassignedValuesListSolution.generateUninitializedSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        var firstValue = solution.getValueList().get(0);
        var secondValue = solution.getValueList().get(1);
        firstEntity.getValueList().add(firstValue);
        secondEntity.getValueList().add(secondValue);
        SolutionManager.updateShadowVariables(solution);
        firstEntity.setPinned(true);

        // firstEntity is pinned -> firstValue cannot be unassigned from it.
        // Only secondValue from secondEntity can be unassigned.
        var context = NeighborhoodTester.build(new ListUnassignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(Moves.unassign(variableMetaModel, secondEntity, 0));
        context.producesNoneOf(Moves.unassign(variableMetaModel, firstEntity, 0));
    }

    @Test
    void fromSolution() {
        var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                .listVariable();

        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(4, 2);
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        var v1 = solution.getValueList().get(0);
        var v2 = solution.getValueList().get(1);
        var v3 = solution.getValueList().get(2);
        // v4 stays unassigned
        e1.getValueList().add(v1);
        e1.getValueList().add(v2);
        e2.getValueList().add(v3);
        SolutionManager.updateShadowVariables(solution);

        NeighborhoodTester.build(new ListUnassignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .producesAllOf(
                        Moves.unassign(variableMetaModel, e1, 0),
                        Moves.unassign(variableMetaModel, e1, 1),
                        Moves.unassign(variableMetaModel, e2, 0));
    }

    @Test
    void noAssignedValues() {
        var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                .listVariable();

        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(2, 2);
        // All values unassigned; no entity holds a value, so no move can even be named.
        var context = NeighborhoodTester.build(new ListUnassignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        assertThat(context.getMovesAsIterator().hasNext()).isFalse();
    }

    @Test
    void failsOnNonUnassignedListVariable() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class).listVariable();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ListUnassignMoveProvider<>(variableMetaModel));
    }

}
