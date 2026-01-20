package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.preview.api.move.MoveRunner;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ListSwapMove} using the {@link MoveRunner} API.
 * Validates permanent execution of ListSwapMove on list planning variables.
 */
class ListSwapMoveTest {

    @Test
    void listSwapMoveWithinSameEntity() {
        // Arrange - entity with 3 values
        var solution = TestdataListSolution.generateInitializedSolution(3, 1);
        var entity = solution.getEntityList().get(0);
        var value1 = entity.getValueList().get(0);
        var value2 = entity.getValueList().get(1);
        var value3 = entity.getValueList().get(2);

        var solutionMetaModel = TestdataListSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        // Swap positions 0 and 2
        var swapMove = Moves.swap(variableMetaModel, entity, 0, entity, 2);

        // Act
        try (var runner = MoveRunner.build(TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class)) {
            var context = runner.using(solution);
            context.execute(swapMove);
        }

        // Assert - order should be: value3, value2, value1
        assertThat(entity.getValueList()).hasSize(3);
        assertThat(entity.getValueList().get(0)).isEqualTo(value3);
        assertThat(entity.getValueList().get(1)).isEqualTo(value2);
        assertThat(entity.getValueList().get(2)).isEqualTo(value1);
    }

    @Test
    void listSwapMoveBetweenEntities() {
        // Arrange - 2 entities with values
        var solution = TestdataListSolution.generateInitializedSolution(4, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);

        var value1 = entity1.getValueList().get(0);
        var value2 = entity2.getValueList().get(0);

        var solutionMetaModel = TestdataListSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        // Swap entity1[0] with entity2[0]
        var swapMove = Moves.swap(variableMetaModel, entity1, 0, entity2, 0);

        // Act
        try (var runner = MoveRunner.build(TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class)) {
            var context = runner.using(solution);
            context.execute(swapMove);
        }

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

        // Act
        try (var runner = MoveRunner.build(TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class)) {
            var context = runner.using(solution);
            context.execute(swapMove);
        }

        // Assert - positions 1 and 2 should be swapped
        assertThat(entity.getValueList().get(1)).isEqualTo(value2);
        assertThat(entity.getValueList().get(2)).isEqualTo(value1);
    }

    @Test
    void multipleListSwapMoves() {
        // Arrange
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

        // Act
        try (var runner = MoveRunner.build(TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class)) {
            var context = runner.using(solution);
            context.execute(move1);
            context.execute(move2);
        }

        // Assert - both entities have their first two positions swapped
        assertThat(entity1.getValueList().get(0)).isEqualTo(e1v1);
        assertThat(entity1.getValueList().get(1)).isEqualTo(e1v0);
        assertThat(entity2.getValueList().get(0)).isEqualTo(e2v1);
        assertThat(entity2.getValueList().get(1)).isEqualTo(e2v0);
    }

    // ************************************************************************
    // Temporary execution tests (T038)
    // ************************************************************************

    @Test
    void listSwapMoveExecutesTemporarilyWithUndo() {
        // Arrange
        var solution = TestdataListSolution.generateInitializedSolution(3, 1);
        var entity = solution.getEntityList().get(0);
        var originalList = entity.getValueList().stream().toList();

        var solutionMetaModel = TestdataListSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var swapMove = Moves.swap(variableMetaModel, entity, 0, entity, 2);

        // Act & Assert
        try (var runner = MoveRunner.build(TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class)) {
            var context = runner.using(solution);

            context.executeTemporarily(swapMove, view -> {
                // During temporary execution, positions should be swapped
                assertThat(entity.getValueList().get(0)).isEqualTo(originalList.get(2));
                assertThat(entity.getValueList().get(1)).isEqualTo(originalList.get(1));
                assertThat(entity.getValueList().get(2)).isEqualTo(originalList.get(0));
            });

            // After undo, list should be restored
            assertThat(entity.getValueList()).containsExactlyElementsOf(originalList);
        }
    }

    @Test
    void listSwapMoveTemporaryBetweenEntities() {
        // Arrange
        var solution = TestdataListSolution.generateInitializedSolution(4, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var originalList1 = entity1.getValueList().stream().toList();
        var originalList2 = entity2.getValueList().stream().toList();

        var solutionMetaModel = TestdataListSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var swapMove = Moves.swap(variableMetaModel, entity1, 0, entity2, 0);

        // Act & Assert
        try (var runner = MoveRunner.build(TestdataListSolution.class, TestdataListEntity.class, TestdataListValue.class)) {
            var context = runner.using(solution);

            context.executeTemporarily(swapMove, view -> {
                // Values should be swapped
                assertThat(entity1.getValueList().get(0)).isEqualTo(originalList2.get(0));
                assertThat(entity2.getValueList().get(0)).isEqualTo(originalList1.get(0));
            });

            // Both lists should be restored
            assertThat(entity1.getValueList()).containsExactlyElementsOf(originalList1);
            assertThat(entity2.getValueList()).containsExactlyElementsOf(originalList2);
        }
    }
}
