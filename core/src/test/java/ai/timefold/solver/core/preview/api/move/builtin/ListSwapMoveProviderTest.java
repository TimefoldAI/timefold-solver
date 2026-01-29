package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodEvaluator;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class ListSwapMoveProviderTest {

    @Test
    void fromSolution() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable();

        var solution = TestdataListSolution.generateUninitializedSolution(4, 2);
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        var assignedValue1 = solution.getValueList().get(1);
        var assignedValue2 = solution.getValueList().get(2);
        var assignedValue3 = solution.getValueList().get(3);
        e1.getValueList().add(assignedValue1);
        e2.getValueList().add(assignedValue2);
        e2.getValueList().add(assignedValue3);
        SolutionManager.updateShadowVariables(solution);

        var moveList = NeighborhoodEvaluator.build(new ListSwapMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList();
        assertThat(moveList).hasSize(6);

        // We have 4 values.
        // One is unassigned, therefore isn't included in the swaps.
        // Three other values can be mutually swapped:
        // - assignedValue1 <-> assignedValue2
        // - assignedValue1 <-> assignedValue3
        // - assignedValue2 <-> assignedValue3
        // That makes 6 possible swap moves. (Includes duplicates.)

        var move1 = (ListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) moveList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(move1.getPlanningEntities())
                    .containsOnly(e1, e2);
            softly.assertThat(move1.getPlanningValues())
                    .containsOnly(assignedValue1, assignedValue2);
        });

        var move2 = (ListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) moveList.get(1);
        assertSoftly(softly -> {
            softly.assertThat(move2.getPlanningEntities())
                    .containsOnly(e1, e2);
            softly.assertThat(move2.getPlanningValues())
                    .containsOnly(assignedValue1, assignedValue3);
        });

        var move3 = (ListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) moveList.get(2);
        assertSoftly(softly -> {
            softly.assertThat(move3.getPlanningEntities())
                    .containsOnly(e1, e2);
            softly.assertThat(move3.getPlanningValues())
                    .containsOnly(assignedValue1, assignedValue2);
        });

        var move4 = (ListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) moveList.get(3);
        assertSoftly(softly -> {
            softly.assertThat(move4.getPlanningEntities())
                    .containsOnly(e2);
            softly.assertThat(move4.getPlanningValues())
                    .containsOnly(assignedValue2, assignedValue3);
        });

        var move5 = (ListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) moveList.get(4);
        assertSoftly(softly -> {
            softly.assertThat(move5.getPlanningEntities())
                    .containsOnly(e1, e2);
            softly.assertThat(move5.getPlanningValues())
                    .containsOnly(assignedValue1, assignedValue3);
        });

        var move6 = (ListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) moveList.get(5);
        assertSoftly(softly -> {
            softly.assertThat(move6.getPlanningEntities())
                    .containsOnly(e2);
            softly.assertThat(move6.getPlanningValues())
                    .containsOnly(assignedValue2, assignedValue3);
        });
    }

    @Test
    void fromEntity() {
        var solutionMetaModel = TestdataListEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntityProvidingEntity.class)
                .listVariable();

        var solution = TestdataListEntityProvidingSolution.generateSolution();
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        e1.getValueList().clear();
        var initiallyAssignedValue = e2.getValueRange().get(0);
        e2.getValueList().add(initiallyAssignedValue);
        SolutionManager.updateShadowVariables(solution);

        var moveList = NeighborhoodEvaluator.build(new ListSwapMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList();

        // There is only one overlapping value between the ranges of e1 and e2: v1.
        // Therefore there are no possible swap moves.
        assertThat(moveList).isEmpty();
    }

}
