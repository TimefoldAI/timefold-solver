package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.preview.api.move.MoveRunner;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CompositeMove} using the {@link MoveRunner} API.
 * Validates permanent execution of CompositeMove combining multiple atomic moves.
 */
class CompositeMoveTest {

    @Test
    void compositeMoveExecutesAllSubMoves() {
        // Arrange
        var solution = TestdataSolution.generateSolution(3, 3);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var entity3 = solution.getEntityList().get(2);
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);
        var value3 = solution.getValueList().get(2);

        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        // Create composite move with 3 change moves
        var move1 = Moves.change(variableMetaModel, entity1, value3);
        var move2 = Moves.change(variableMetaModel, entity2, value1);
        var move3 = Moves.change(variableMetaModel, entity3, value2);
        var compositeMove = Moves.compose(move1, move2, move3);

        // Act
        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            var context = runner.using(solution);
            context.execute(compositeMove);
        }

        // Assert - all changes should be applied
        assertThat(entity1.getValue()).isEqualTo(value3);
        assertThat(entity2.getValue()).isEqualTo(value1);
        assertThat(entity3.getValue()).isEqualTo(value2);
    }

    @Test
    void compositeMoveWithSwap() {
        // Arrange
        var solution = TestdataSolution.generateSolution(3, 3);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var entity3 = solution.getEntityList().get(2);
        var initialValue1 = entity1.getValue();
        var initialValue2 = entity2.getValue();
        var value3 = solution.getValueList().get(2);

        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        // Composite: swap entity1 and entity2, then change entity3
        var swapMove = Moves.swap(variableMetaModel, entity1, entity2);
        var changeMove = Moves.change(variableMetaModel, entity3, value3);
        var compositeMove = Moves.compose(swapMove, changeMove);

        // Act
        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            var context = runner.using(solution);
            context.execute(compositeMove);
        }

        // Assert
        assertThat(entity1.getValue()).isEqualTo(initialValue2);
        assertThat(entity2.getValue()).isEqualTo(initialValue1);
        assertThat(entity3.getValue()).isEqualTo(value3);
    }

    @Test
    void compositeMoveWithSingleSubMove() {
        // Arrange
        var solution = TestdataSolution.generateSolution(2, 1);
        var entity = solution.getEntityList().get(0);
        var newValue = solution.getValueList().get(1);

        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        // Composite with single move
        var changeMove = Moves.change(variableMetaModel, entity, newValue);
        var compositeMove = Moves.compose(changeMove);

        // Act
        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            var context = runner.using(solution);
            context.execute(compositeMove);
        }

        // Assert
        assertThat(entity.getValue()).isEqualTo(newValue);
    }

    @Test
    void nestedCompositeMoves() {
        // Arrange
        var solution = TestdataSolution.generateSolution(4, 4);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var entity3 = solution.getEntityList().get(2);
        var entity4 = solution.getEntityList().get(3);
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);
        var value3 = solution.getValueList().get(2);
        var value4 = solution.getValueList().get(3);

        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        // Create nested composite moves
        var move1 = Moves.change(variableMetaModel, entity1, value4);
        var move2 = Moves.change(variableMetaModel, entity2, value3);
        var composite1 = Moves.compose(move1, move2);

        var move3 = Moves.change(variableMetaModel, entity3, value2);
        var move4 = Moves.change(variableMetaModel, entity4, value1);
        var composite2 = Moves.compose(move3, move4);

        var nestedComposite = Moves.compose(composite1, composite2);

        // Act
        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            var context = runner.using(solution);
            context.execute(nestedComposite);
        }

        // Assert - all 4 changes should be applied
        assertThat(entity1.getValue()).isEqualTo(value4);
        assertThat(entity2.getValue()).isEqualTo(value3);
        assertThat(entity3.getValue()).isEqualTo(value2);
        assertThat(entity4.getValue()).isEqualTo(value1);
    }

    // ************************************************************************
    // Temporary execution tests (T039)
    // ************************************************************************

    @Test
    void compositeMoveExecutesTemporarilyWithUndo() {
        // Arrange
        var solution = TestdataSolution.generateSolution(3, 3);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var entity3 = solution.getEntityList().get(2);
        var originalValue1 = entity1.getValue();
        var originalValue2 = entity2.getValue();
        var originalValue3 = entity3.getValue();
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);
        var value3 = solution.getValueList().get(2);

        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var move1 = Moves.change(variableMetaModel, entity1, value3);
        var move2 = Moves.change(variableMetaModel, entity2, value1);
        var move3 = Moves.change(variableMetaModel, entity3, value2);
        var compositeMove = Moves.compose(move1, move2, move3);

        // Act & Assert
        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            var context = runner.using(solution);

            context.executeTemporarily(compositeMove, view -> {
                // All changes should be applied
                assertThat(entity1.getValue()).isEqualTo(value3);
                assertThat(entity2.getValue()).isEqualTo(value1);
                assertThat(entity3.getValue()).isEqualTo(value2);
            });

            // All changes should be undone
            assertThat(entity1.getValue()).isEqualTo(originalValue1);
            assertThat(entity2.getValue()).isEqualTo(originalValue2);
            assertThat(entity3.getValue()).isEqualTo(originalValue3);
        }
    }

    @Test
    void nestedCompositeMoveTemporary() {
        // Arrange
        var solution = TestdataSolution.generateSolution(4, 4);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var entity3 = solution.getEntityList().get(2);
        var entity4 = solution.getEntityList().get(3);
        var originalValue1 = entity1.getValue();
        var originalValue2 = entity2.getValue();
        var originalValue3 = entity3.getValue();
        var originalValue4 = entity4.getValue();
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);
        var value3 = solution.getValueList().get(2);
        var value4 = solution.getValueList().get(3);

        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var move1 = Moves.change(variableMetaModel, entity1, value4);
        var move2 = Moves.change(variableMetaModel, entity2, value3);
        var composite1 = Moves.compose(move1, move2);

        var move3 = Moves.change(variableMetaModel, entity3, value2);
        var move4 = Moves.change(variableMetaModel, entity4, value1);
        var composite2 = Moves.compose(move3, move4);

        var nestedComposite = Moves.compose(composite1, composite2);

        // Act & Assert
        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            var context = runner.using(solution);

            context.executeTemporarily(nestedComposite, view -> {
                // All 4 changes should be applied
                assertThat(entity1.getValue()).isEqualTo(value4);
                assertThat(entity2.getValue()).isEqualTo(value3);
                assertThat(entity3.getValue()).isEqualTo(value2);
                assertThat(entity4.getValue()).isEqualTo(value1);
            });

            // All 4 changes should be undone
            assertThat(entity1.getValue()).isEqualTo(originalValue1);
            assertThat(entity2.getValue()).isEqualTo(originalValue2);
            assertThat(entity3.getValue()).isEqualTo(originalValue3);
            assertThat(entity4.getValue()).isEqualTo(originalValue4);
        }
    }
}
