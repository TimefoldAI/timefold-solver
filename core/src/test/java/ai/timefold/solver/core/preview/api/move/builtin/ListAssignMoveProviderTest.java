package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.List;

import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class ListAssignMoveProviderTest {

    @Test
    void constructorRejectsNonUnassignedVariable() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ListAssignMoveProvider<>(variableMetaModel));
    }

    @Test
    void fromSolution() {
        var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                .listVariable();

        // e1 is empty; e2 has v1 assigned; v2 is unassigned.
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(2, 2);
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        var v1 = solution.getValueList().get(0);
        var v2 = solution.getValueList().get(1);
        e2.getValueList().add(v1);
        SolutionManager.updateShadowVariables(solution);

        // v1 is assigned → not picked. v2 is unassigned → picked.
        // Destinations for v2: e1@0, e2@0, e2@1 → 3 moves.
        var moveList = NeighborhoodTester.build(new ListAssignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList();
        assertThat(moveList).hasSize(3);

        var move1 = getListAssignMove(moveList, 0);
        assertSoftly(softly -> {
            softly.assertThat(move1.getDestinationEntity()).isEqualTo(e1);
            softly.assertThat(move1.getDestinationIndex()).isEqualTo(0);
            softly.assertThat(move1.getPlanningEntities()).containsExactly(e1);
            softly.assertThat(move1.getPlanningValues()).containsExactly(v2);
        });

        var move2 = getListAssignMove(moveList, 1);
        assertSoftly(softly -> {
            softly.assertThat(move2.getDestinationEntity()).isEqualTo(e2);
            softly.assertThat(move2.getDestinationIndex()).isEqualTo(1);
            softly.assertThat(move2.getPlanningEntities()).containsExactly(e2);
            softly.assertThat(move2.getPlanningValues()).containsExactly(v2);
        });

        var move3 = getListAssignMove(moveList, 2);
        assertSoftly(softly -> {
            softly.assertThat(move3.getDestinationEntity()).isEqualTo(e2);
            softly.assertThat(move3.getDestinationIndex()).isEqualTo(0);
            softly.assertThat(move3.getPlanningEntities()).containsExactly(e2);
            softly.assertThat(move3.getPlanningValues()).containsExactly(v2);
        });
    }

    private static <Solution_, Entity_, Value_> ListAssignMove<Solution_, Entity_, Value_>
            getListAssignMove(List<Move<Solution_>> moveList, int index) {
        return (ListAssignMove<Solution_, Entity_, Value_>) moveList.get(index);
    }

}
