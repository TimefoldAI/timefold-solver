package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Collections;

import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedEntity;
import ai.timefold.solver.core.testdomain.pinned.TestdataPinnedSolution;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.TestdataEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.valuerange.incomplete.TestdataIncompleteValueRangeEntity;
import ai.timefold.solver.core.testdomain.valuerange.incomplete.TestdataIncompleteValueRangeSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class ChangeMoveProviderTest {

    @Test
    void fromSolution() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable();

        var solution = TestdataSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        var firstValue = solution.getValueList().get(0);
        var secondValue = solution.getValueList().get(1);
        secondEntity.setValue(secondValue);

        var moveList = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList(move -> (ChangeMove<TestdataSolution, TestdataEntity, TestdataValue>) move);
        assertThat(moveList).hasSize(2);

        var firstMove = moveList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(firstMove.getPlanningEntities())
                    .containsExactly(firstEntity);
            softly.assertThat(firstMove.getPlanningValues())
                    .containsExactly(secondValue);
        });

        var secondMove = moveList.get(1);
        assertSoftly(softly -> {
            softly.assertThat(secondMove.getPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(secondMove.getPlanningValues())
                    .containsExactly(firstValue);
        });
    }

    @Test
    void fromSolutionIncompleteValueRange() {
        var solutionMetaModel = TestdataIncompleteValueRangeSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataIncompleteValueRangeEntity.class)
                .basicVariable();

        // The point of this test is to ensure that the move provider skips values that are not in the value range.
        var solution = TestdataIncompleteValueRangeSolution.generateSolution(2, 2);
        var valueNotInValueRange = new TestdataValue("third");
        solution.setValueListNotInValueRange(Collections.singletonList(valueNotInValueRange));

        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        var firstValue = solution.getValueList().get(0);
        var secondValue = solution.getValueList().get(1);
        firstEntity.setValue(firstValue);
        secondEntity.setValue(secondValue);

        var moveList = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList(
                        move -> (ChangeMove<TestdataIncompleteValueRangeSolution, TestdataIncompleteValueRangeEntity, TestdataValue>) move);
        assertThat(moveList).hasSize(2);

        var firstMove = moveList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(firstMove.getPlanningEntities())
                    .containsExactly(firstEntity);
            softly.assertThat(firstMove.getPlanningValues())
                    .containsExactly(secondValue);
        });

        var secondMove = moveList.get(1);
        assertSoftly(softly -> {
            softly.assertThat(secondMove.getPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(secondMove.getPlanningValues())
                    .containsExactly(firstValue);
        });
    }

    @Test
    void fromEntity() {
        var solutionMetaModel = TestdataEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntityProvidingEntity.class)
                .basicVariable();

        var solution = TestdataEntityProvidingSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        var firstValue = firstEntity.getValueRange().getFirst();

        // One move is expected:
        // - firstEntity is already assigned to firstValue, the only possible value; skip.
        // - Assign secondEntity to firstValue,
        //   as it is currently assigned to secondValue, and the value range only contains firstValue.
        var moveList = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList(
                        move -> (ChangeMove<TestdataEntityProvidingSolution, TestdataEntityProvidingEntity, TestdataValue>) move);
        assertThat(moveList).hasSize(1);

        var firstMove = moveList.getFirst();
        assertSoftly(softly -> {
            softly.assertThat(firstMove.getPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(firstMove.getPlanningValues())
                    .hasSize(1)
                    .containsExactly(firstValue);
        });
    }

    @Test
    void fromEntityAllowsUnassigned() {
        var solutionMetaModel = TestdataAllowsUnassignedEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntityProvidingEntity.class)
                .basicVariable();

        var solution = TestdataAllowsUnassignedEntityProvidingSolution.generateSolution(2, 2);
        var secondEntity = solution.getEntityList().get(1);
        var firstValue = solution.getEntityList().get(0).getValueRange().getFirst();

        // One move is expected:
        // - secondEntity is assigned to secondValue, and the value range only contains firstValue;
        //   so a change to firstValue is generated.
        // - firstEntity is assigned to firstValue, same as its only possible non-null value; no change.
        // Null (unassign) moves are not generated by ChangeMoveProvider.
        var moveList = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList(
                        move -> (ChangeMove<TestdataAllowsUnassignedEntityProvidingSolution, TestdataAllowsUnassignedEntityProvidingEntity, TestdataValue>) move);
        assertThat(moveList).hasSize(1);

        var firstMove = moveList.getFirst();
        assertSoftly(softly -> {
            softly.assertThat(firstMove.getPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(firstMove.getPlanningValues())
                    .containsExactly(firstValue);
        });
    }

    @Test
    void pinnedEntitySkipped() {
        var solutionMetaModel = TestdataPinnedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedEntity.class)
                .basicVariable();

        var solution = TestdataPinnedSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        var firstValue = solution.getValueList().getFirst();
        firstEntity.setPinned(true);

        // firstEntity is pinned; only secondEntity can change to firstValue.
        var moveList = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList(move -> (ChangeMove<TestdataPinnedSolution, TestdataPinnedEntity, TestdataValue>) move);
        assertThat(moveList).hasSize(1);

        var firstMove = moveList.getFirst();
        assertSoftly(softly -> {
            softly.assertThat(firstMove.getPlanningEntities()).containsExactly(secondEntity);
            softly.assertThat(firstMove.getPlanningValues()).containsExactly(firstValue);
        });
    }

    @Test
    void fromSolutionAllowsUnassigned() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class)
                .basicVariable();

        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 2);
        var secondEntity = solution.getEntityList().get(1); // Assigned to secondValue.
        var firstValue = solution.getValueList().getFirst(); // Not assigned to any entity.

        // First entity is assigned to null, so it is filtered out by ChangeMoveProvider.
        // Second entity is assigned to secondValue, so the only applicable move assigns to firstValue.
        // Null (unassign) moves are not generated by ChangeMoveProvider.
        var moveList = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList(
                        move -> (ChangeMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity, TestdataValue>) move);
        assertThat(moveList).hasSize(1);

        var firstMove = moveList.getFirst();
        assertSoftly(softly -> {
            softly.assertThat(firstMove.getPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(firstMove.getPlanningValues())
                    .containsExactly(firstValue);
        });
    }

}