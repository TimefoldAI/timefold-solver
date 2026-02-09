package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Collections;

import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
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
        firstEntity.setValue(null);
        var secondEntity = solution.getEntityList().get(1);
        secondEntity.setValue(null);
        var firstValue = solution.getValueList().get(0);
        var secondValue = solution.getValueList().get(1);

        var moveList = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList(move -> (ChangeMove<TestdataSolution, TestdataEntity, TestdataValue>) move);
        assertThat(moveList).hasSize(4);

        var firstMove = moveList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(firstMove.getPlanningEntities())
                    .containsExactly(firstEntity);
            softly.assertThat(firstMove.getPlanningValues())
                    .containsExactly(firstValue);
        });

        var secondMove = moveList.get(1);
        assertSoftly(softly -> {
            softly.assertThat(secondMove.getPlanningEntities())
                    .containsExactly(firstEntity);
            softly.assertThat(secondMove.getPlanningValues())
                    .containsExactly(secondValue);
        });

        var thirdMove = moveList.get(2);
        assertSoftly(softly -> {
            softly.assertThat(thirdMove.getPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(thirdMove.getPlanningValues())
                    .containsExactly(firstValue);
        });

        var fourthMove = moveList.get(3);
        assertSoftly(softly -> {
            softly.assertThat(fourthMove.getPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(fourthMove.getPlanningValues())
                    .containsExactly(secondValue);
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
        firstEntity.setValue(null);
        var secondEntity = solution.getEntityList().get(1);
        secondEntity.setValue(null);
        var firstValue = solution.getValueList().get(0);
        var secondValue = solution.getValueList().get(1);

        var moveList = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList(
                        move -> (ChangeMove<TestdataIncompleteValueRangeSolution, TestdataIncompleteValueRangeEntity, TestdataValue>) move);
        assertThat(moveList).hasSize(4);

        var firstMove = moveList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(firstMove.getPlanningEntities())
                    .containsExactly(firstEntity);
            softly.assertThat(firstMove.getPlanningValues())
                    .containsExactly(firstValue);
        });

        var secondMove = moveList.get(1);
        assertSoftly(softly -> {
            softly.assertThat(secondMove.getPlanningEntities())
                    .containsExactly(firstEntity);
            softly.assertThat(secondMove.getPlanningValues())
                    .containsExactly(secondValue);
        });

        var thirdMove = moveList.get(2);
        assertSoftly(softly -> {
            softly.assertThat(thirdMove.getPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(thirdMove.getPlanningValues())
                    .containsExactly(firstValue);
        });

        var fourthMove = moveList.get(3);
        assertSoftly(softly -> {
            softly.assertThat(fourthMove.getPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(fourthMove.getPlanningValues())
                    .containsExactly(secondValue);
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
        var firstValue = firstEntity.getValueRange().get(0);

        // One move is expected:
        // - firstEntity is already assigned to firstValue, the only possible value; skip.
        // - Assign secondEntity to firstValue,
        //   as it is currently assigned to secondValue, and the value range only contains firstValue.
        var moveList = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList(
                        move -> (ChangeMove<TestdataEntityProvidingSolution, TestdataEntityProvidingEntity, TestdataValue>) move);
        assertThat(moveList).hasSize(1);

        var firstMove = moveList.get(0);
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
        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        var firstValue = firstEntity.getValueRange().get(0);

        // Three moves are expected:
        // - Assign firstEntity to null,
        //   as it is currently assigned to firstValue, and the value range only contains firstValue.
        // - Assign secondEntity to null and to firstValue,
        //   as it is currently assigned to secondValue, and the value range only contains firstValue.
        // Null is not in the value range, but as documented,
        // null is added automatically to value ranges when allowsUnassigned is true.
        var moveList = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList(
                        move -> (ChangeMove<TestdataAllowsUnassignedEntityProvidingSolution, TestdataAllowsUnassignedEntityProvidingEntity, TestdataValue>) move);
        assertThat(moveList).hasSize(3);

        var firstMove = moveList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(firstMove.getPlanningEntities())
                    .containsExactly(firstEntity);
            softly.assertThat(firstMove.getPlanningValues())
                    .hasSize(1)
                    .containsNull();
        });

        var secondMove = moveList.get(1);
        assertSoftly(softly -> {
            softly.assertThat(secondMove.getPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(secondMove.getPlanningValues())
                    .hasSize(1)
                    .containsNull();
        });

        var thirdMove = moveList.get(2);
        assertSoftly(softly -> {
            softly.assertThat(thirdMove.getPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(thirdMove.getPlanningValues())
                    .containsExactly(firstValue);
        });
    }

    @Test
    void fromSolutionAllowsUnassigned() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class)
                .basicVariable();

        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0); // Assigned to null.
        var secondEntity = solution.getEntityList().get(1); // Assigned to secondValue.
        var firstValue = solution.getValueList().get(0); // Not assigned to any entity.
        var secondValue = solution.getValueList().get(1);

        // Filters out moves that would change the value to the value the entity already has.
        // Therefore this will have 4 moves (2 entities * 2 values) as opposed to 6 (2 entities * 3 values).
        var moveList = NeighborhoodTester.build(new ChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList(
                        move -> (ChangeMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity, TestdataValue>) move);
        assertThat(moveList).hasSize(4);

        // First entity is assigned to null, therefore the applicable moves assign to firstValue and secondValue.
        var firstMove = moveList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(firstMove.getPlanningEntities())
                    .containsExactly(firstEntity);
            softly.assertThat(firstMove.getPlanningValues())
                    .containsExactly(firstValue);
        });

        var secondMove = moveList.get(1);
        assertSoftly(softly -> {
            softly.assertThat(secondMove.getPlanningEntities())
                    .containsExactly(firstEntity);
            softly.assertThat(secondMove.getPlanningValues())
                    .containsExactly(secondValue);
        });

        // Second entity is assigned to secondValue, therefore the applicable moves assign to null and firstValue.
        var thirdMove = moveList.get(2);
        assertSoftly(softly -> {
            softly.assertThat(thirdMove.getPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(thirdMove.getPlanningValues())
                    .containsExactly(new TestdataValue[] { null });
        });

        var fourthMove = moveList.get(3);
        assertSoftly(softly -> {
            softly.assertThat(fourthMove.getPlanningEntities())
                    .containsExactly(secondEntity);
            softly.assertThat(fourthMove.getPlanningValues())
                    .containsExactly(firstValue);
        });

    }

}