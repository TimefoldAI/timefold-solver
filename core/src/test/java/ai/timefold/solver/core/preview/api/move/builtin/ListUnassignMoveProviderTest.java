package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class ListUnassignMoveProviderTest {

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

        var moveList = NeighborhoodTester.build(new ListUnassignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList(
                        move -> (ListUnassignMove<TestdataAllowsUnassignedValuesListSolution, TestdataAllowsUnassignedValuesListEntity, TestdataAllowsUnassignedValuesListValue>) move);
        assertThat(moveList).hasSize(3);

        var move1 = moveList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(move1.getSourceEntity()).isEqualTo(e1);
            softly.assertThat(move1.getSourceIndex()).isEqualTo(0);
            softly.assertThat(move1.getPlanningEntities()).containsExactly(e1);
            softly.assertThat(move1.getPlanningValues()).containsExactly(v1);
        });

        var move2 = moveList.get(1);
        assertSoftly(softly -> {
            softly.assertThat(move2.getSourceEntity()).isEqualTo(e1);
            softly.assertThat(move2.getSourceIndex()).isEqualTo(1);
            softly.assertThat(move2.getPlanningEntities()).containsExactly(e1);
            softly.assertThat(move2.getPlanningValues()).containsExactly(v2);
        });

        var move3 = moveList.get(2);
        assertSoftly(softly -> {
            softly.assertThat(move3.getSourceEntity()).isEqualTo(e2);
            softly.assertThat(move3.getSourceIndex()).isEqualTo(0);
            softly.assertThat(move3.getPlanningEntities()).containsExactly(e2);
            softly.assertThat(move3.getPlanningValues()).containsExactly(v3);
        });
    }

    @Test
    void noAssignedValues() {
        var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                .listVariable();

        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(2, 2);
        // All values unassigned, no moves expected.
        var moveList = NeighborhoodTester.build(new ListUnassignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList();
        assertThat(moveList).isEmpty();
    }

    @Test
    void failsOnNonUnassignedListVariable() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class).listVariable();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ListUnassignMoveProvider<>(variableMetaModel));
    }

}
