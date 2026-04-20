package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.pinned.unassignedvar.TestdataPinnedAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.pinned.unassignedvar.TestdataPinnedAllowsUnassignedSolution;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class AssignMoveProviderTest {

    @Test
    void constructorRejectsNonUnassignedVariable() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AssignMoveProvider<>(variableMetaModel));
    }

    @Test
    void pinnedEntitySkipped() {
        var solutionMetaModel = TestdataPinnedAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedAllowsUnassignedEntity.class)
                .basicVariable();

        var solution = TestdataPinnedAllowsUnassignedSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        firstEntity.setPinned(true);
        firstEntity.setValue(null);
        secondEntity.setValue(null);
        var firstValue = solution.getValueList().get(0);
        var secondValue = solution.getValueList().get(1);

        // firstEntity is pinned → no assign moves. Only secondEntity gets assignments.
        var moveList = NeighborhoodTester.build(new AssignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList(
                        move -> (ChangeMove<TestdataPinnedAllowsUnassignedSolution, TestdataPinnedAllowsUnassignedEntity, TestdataValue>) move);
        assertThat(moveList).hasSize(2);

        var firstMove = moveList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(firstMove.getPlanningEntities()).containsExactly(secondEntity);
            softly.assertThat(firstMove.getPlanningValues()).containsExactly(firstValue);
        });

        var secondMove = moveList.get(1);
        assertSoftly(softly -> {
            softly.assertThat(secondMove.getPlanningEntities()).containsExactly(secondEntity);
            softly.assertThat(secondMove.getPlanningValues()).containsExactly(secondValue);
        });
    }

    @Test
    void fromEntity() {
        var solutionMetaModel = TestdataAllowsUnassignedEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntityProvidingEntity.class)
                .basicVariable();

        // entity1: range={v1,v2}, null; entity2: range={v1,v3}, null.
        var solution = TestdataAllowsUnassignedEntityProvidingSolution.generateSolution();
        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        var v1 = firstEntity.getValueRange().get(0);
        var v2 = firstEntity.getValueRange().get(1);
        var v3 = secondEntity.getValueRange().get(1);

        // Both entities start null; only values in each entity's range are offered.
        var moveList = NeighborhoodTester.build(new AssignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList(
                        move -> (ChangeMove<TestdataAllowsUnassignedEntityProvidingSolution, TestdataAllowsUnassignedEntityProvidingEntity, TestdataValue>) move);
        assertThat(moveList).hasSize(4);

        var firstMove = moveList.get(0);
        assertSoftly(softly -> {
            softly.assertThat(firstMove.getPlanningEntities()).containsExactly(firstEntity);
            softly.assertThat(firstMove.getPlanningValues()).containsExactly(v1);
        });

        var secondMove = moveList.get(1);
        assertSoftly(softly -> {
            softly.assertThat(secondMove.getPlanningEntities()).containsExactly(firstEntity);
            softly.assertThat(secondMove.getPlanningValues()).containsExactly(v2);
        });

        var thirdMove = moveList.get(2);
        assertSoftly(softly -> {
            softly.assertThat(thirdMove.getPlanningEntities()).containsExactly(secondEntity);
            softly.assertThat(thirdMove.getPlanningValues()).containsExactly(v1);
        });

        var fourthMove = moveList.get(3);
        assertSoftly(softly -> {
            softly.assertThat(fourthMove.getPlanningEntities()).containsExactly(secondEntity);
            softly.assertThat(fourthMove.getPlanningValues()).containsExactly(v3);
        });
    }

    @Test
    void fromSolution() {
        var solutionMetaModel = TestdataAllowsUnassignedSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedEntity.class)
                .basicVariable();

        var solution = TestdataAllowsUnassignedSolution.generateSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        firstEntity.setValue(null);
        var secondEntity = solution.getEntityList().get(1);
        secondEntity.setValue(null);
        var firstValue = solution.getValueList().get(0);
        var secondValue = solution.getValueList().get(1);

        var moveList = NeighborhoodTester.build(new AssignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .getMovesAsList(
                        move -> (ChangeMove<TestdataAllowsUnassignedSolution, TestdataAllowsUnassignedEntity, TestdataValue>) move);
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

}
