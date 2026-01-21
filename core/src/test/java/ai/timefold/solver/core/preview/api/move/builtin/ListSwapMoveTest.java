package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.preview.api.move.MoveRunner;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

import org.junit.jupiter.api.Test;

class ListSwapMoveTest {

    @Test
    void listSwapMoveWithinSameEntity() {
        var solution = TestdataListSolution.generateInitializedSolution(3, 1);
        var entity = solution.getEntityList().get(0);
        var value1 = entity.getValueList().get(0);
        var value2 = entity.getValueList().get(1);
        var value3 = entity.getValueList().get(2);

        var solutionMetaModel = TestdataListSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var swapMove = Moves.swap(variableMetaModel, entity, 0, entity, 2);

        MoveRunner.build(TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class)
                .using(solution)
                .execute(swapMove);

        // Assert - order should be: value3, value2, value1
        assertThat(entity.getValueList()).hasSize(3);
        assertThat(entity.getValueList().get(0)).isEqualTo(value3);
        assertThat(entity.getValueList().get(1)).isEqualTo(value2);
        assertThat(entity.getValueList().get(2)).isEqualTo(value1);
    }

    @Test
    void listSwapMoveBetweenEntities() {
        var solution = TestdataListSolution.generateInitializedSolution(4, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);

        var value1 = entity1.getValueList().get(0);
        var value2 = entity2.getValueList().get(0);

        var solutionMetaModel = TestdataListSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var swapMove = Moves.swap(variableMetaModel, entity1, 0, entity2, 0);

        MoveRunner.build(TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class)
                .using(solution)
                .execute(swapMove);

        // Assert - values should be swapped
        assertThat(entity1.getValueList().get(0)).isEqualTo(value2);
        assertThat(entity2.getValueList().get(0)).isEqualTo(value1);
    }

    @Test
    void listSwapMoveAdjacentPositions() {
        // Arrange
        var solution = TestdataListSolution.generateInitializedSolution(4, 1);
        var entity = solution.getEntityList().get(0);
        var value1 = entity.getValueList().get(1);
        var value2 = entity.getValueList().get(2);

        var solutionMetaModel = TestdataListSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        // Swap adjacent positions 1 and 2
        var swapMove = Moves.swap(variableMetaModel, entity, 1, entity, 2);

        MoveRunner.build(TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class)
                .using(solution)
                .execute(swapMove);

        // Assert - positions 1 and 2 should be swapped
        assertThat(entity.getValueList().get(1)).isEqualTo(value2);
        assertThat(entity.getValueList().get(2)).isEqualTo(value1);
    }

    @Test
    void multipleListSwapMoves() {
        var solution = TestdataListSolution.generateInitializedSolution(6, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);

        var solutionMetaModel = TestdataListSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var move1 = Moves.swap(variableMetaModel, entity1, 0, entity1, 1);
        var move2 = Moves.swap(variableMetaModel, entity2, 0, entity2, 1);

        var e1v0 = entity1.getValueList().get(0);
        var e1v1 = entity1.getValueList().get(1);
        var e2v0 = entity2.getValueList().get(0);
        var e2v1 = entity2.getValueList().get(1);

        var context = MoveRunner.build(TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class)
                .using(solution);
        context.execute(move1);
        context.execute(move2);

        // Assert - both entities have their first two positions swapped
        assertThat(entity1.getValueList().get(0)).isEqualTo(e1v1);
        assertThat(entity1.getValueList().get(1)).isEqualTo(e1v0);
        assertThat(entity2.getValueList().get(0)).isEqualTo(e2v1);
        assertThat(entity2.getValueList().get(1)).isEqualTo(e2v0);
    }

    @Test
    void listSwapMoveExecutesTemporarilyWithUndo() {
        var solution = TestdataListSolution.generateInitializedSolution(3, 1);
        var entity = solution.getEntityList().get(0);
        var originalList = entity.getValueList().stream().toList();

        var solutionMetaModel = TestdataListSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var swapMove = Moves.swap(variableMetaModel, entity, 0, entity, 2);

        MoveRunner.build(TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class)
                .using(solution)
                .executeTemporarily(swapMove, view -> {
                    // During temporary execution, positions should be swapped
                    assertThat(entity.getValueList().get(0)).isEqualTo(originalList.get(2));
                    assertThat(entity.getValueList().get(1)).isEqualTo(originalList.get(1));
                    assertThat(entity.getValueList().get(2)).isEqualTo(originalList.get(0));
                });

        // After undo, list should be restored
        assertThat(entity.getValueList()).containsExactlyElementsOf(originalList);
    }

    @Test
    void listSwapMoveTemporaryBetweenEntities() {
        var solution = TestdataListSolution.generateInitializedSolution(4, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var originalList1 = entity1.getValueList().stream().toList();
        var originalList2 = entity2.getValueList().stream().toList();

        var solutionMetaModel = TestdataListSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var swapMove = Moves.swap(variableMetaModel, entity1, 0, entity2, 0);

        MoveRunner.build(TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class)
                .using(solution)
                .executeTemporarily(swapMove, view -> {
                    // Values should be swapped
                    assertThat(entity1.getValueList().get(0)).isEqualTo(originalList2.get(0));
                    assertThat(entity2.getValueList().get(0)).isEqualTo(originalList1.get(0));
                });

        // Both lists should be restored
        assertThat(entity1.getValueList()).containsExactlyElementsOf(originalList1);
        assertThat(entity2.getValueList()).containsExactlyElementsOf(originalList2);
    }
}
