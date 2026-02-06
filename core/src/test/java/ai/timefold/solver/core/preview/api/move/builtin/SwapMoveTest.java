package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.preview.api.move.MoveTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.junit.jupiter.api.Test;

class SwapMoveTest {

    @Test
    void swapMoveExecutesPermanently() {
        var solution = TestdataSolution.generateSolution(3, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value1 = entity1.getValue();
        var value2 = entity2.getValue();

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var swapMove = Moves.swap(variableMetaModel, entity1, entity2);

        MoveTester.build(solutionMetaModel)
                .using(solution)
                .execute(swapMove);

        // Assert - values should be swapped
        assertThat(entity1.getValue()).isEqualTo(value2);
        assertThat(entity2.getValue()).isEqualTo(value1);
    }

    @Test
    void swapMoveWithSameValue() {
        var solution = TestdataSolution.generateSolution(2, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value = solution.getValueList().get(0);

        entity1.setValue(value);
        entity2.setValue(value);

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var swapMove = Moves.swap(variableMetaModel, entity1, entity2);

        MoveTester.build(solutionMetaModel)
                .using(solution)
                .execute(swapMove);

        // Assert - both should still have the same value
        assertThat(entity1.getValue()).isEqualTo(value);
        assertThat(entity2.getValue()).isEqualTo(value);
    }

    @Test
    void multipleSwapMoves() {
        var solution = TestdataSolution.generateSolution(3, 3);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var entity3 = solution.getEntityList().get(2);
        var initialValue1 = entity1.getValue();
        var initialValue2 = entity2.getValue();
        var initialValue3 = entity3.getValue();

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var swap1 = Moves.swap(variableMetaModel, entity1, entity2);
        var swap2 = Moves.swap(variableMetaModel, entity2, entity3);

        var context = MoveTester.build(solutionMetaModel)
                .using(solution);
        context.execute(swap1);
        context.execute(swap2);

        // Assert - after swap1: e0=v1, e1=v0, e2=v2
        //          after swap2: e0=v1, e1=v2, e2=v0
        assertThat(entity1.getValue()).isEqualTo(initialValue2);
        assertThat(entity2.getValue()).isEqualTo(initialValue3);
        assertThat(entity3.getValue()).isEqualTo(initialValue1);
    }

    @Test
    void swapMoveExecutesTemporarilyWithUndo() {
        var solution = TestdataSolution.generateSolution(2, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var originalValue1 = entity1.getValue();
        var originalValue2 = entity2.getValue();

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var swapMove = Moves.swap(variableMetaModel, entity1, entity2);

        MoveTester.build(solutionMetaModel)
                .using(solution)
                .executeTemporarily(swapMove, view -> {
                    // During temporary execution, values should be swapped
                    assertThat(entity1.getValue()).isEqualTo(originalValue2);
                    assertThat(entity2.getValue()).isEqualTo(originalValue1);
                });

        // After undo, values should be restored
        assertThat(entity1.getValue()).isEqualTo(originalValue1);
        assertThat(entity2.getValue()).isEqualTo(originalValue2);
    }

    @Test
    void multipleSwapMovesTemporary() {
        var solution = TestdataSolution.generateSolution(3, 3);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var entity3 = solution.getEntityList().get(2);
        var initialValue1 = entity1.getValue();
        var initialValue2 = entity2.getValue();
        var initialValue3 = entity3.getValue();

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var swap1 = Moves.swap(variableMetaModel, entity1, entity2);
        var swap2 = Moves.swap(variableMetaModel, entity2, entity3);

        var context = MoveTester.build(solutionMetaModel)
                .using(solution);

        // Execute swap1 temporarily and verify
        context.executeTemporarily(swap1, view -> {
            assertThat(entity1.getValue()).isEqualTo(initialValue2);
            assertThat(entity2.getValue()).isEqualTo(initialValue1);
            assertThat(entity3.getValue()).isEqualTo(initialValue3);
        });

        // After swap1 undo, verify we're back to original state
        assertThat(entity1.getValue()).isEqualTo(initialValue1);
        assertThat(entity2.getValue()).isEqualTo(initialValue2);
        assertThat(entity3.getValue()).isEqualTo(initialValue3);

        // Execute swap2 temporarily and verify
        context.executeTemporarily(swap2, view -> {
            assertThat(entity1.getValue()).isEqualTo(initialValue1);
            assertThat(entity2.getValue()).isEqualTo(initialValue3);
            assertThat(entity3.getValue()).isEqualTo(initialValue2);
        });

        // After swap2 undo, verify we're back to original state again
        assertThat(entity1.getValue()).isEqualTo(initialValue1);
        assertThat(entity2.getValue()).isEqualTo(initialValue2);
        assertThat(entity3.getValue()).isEqualTo(initialValue3);
    }
}