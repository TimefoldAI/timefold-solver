package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Collections;

import ai.timefold.solver.core.preview.api.move.MoveRunner;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SwapMove} using the {@link MoveRunner} API.
 * Validates permanent execution of SwapMove on basic planning variables.
 */
class SwapMoveTest {

    @Test
    void swapMoveExecutesPermanently() {
        // Arrange
        var solution = TestdataSolution.generateSolution(3, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value1 = entity1.getValue();
        var value2 = entity2.getValue();
        
        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor().getMetaModel();
        @SuppressWarnings("unchecked")
        var variableMetaModel = (ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel<TestdataSolution, TestdataEntity, Object>)
                (Object) solutionMetaModel.entity(TestdataEntity.class).basicVariable("value", TestdataValue.class);
        
        var swapMove = Moves.swap(Collections.singletonList(variableMetaModel), entity1, entity2);
        
        // Act
        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            var context = runner.using(solution);
            context.execute(swapMove);
        }
        
        // Assert - values should be swapped
        assertThat(entity1.getValue()).isEqualTo(value2);
        assertThat(entity2.getValue()).isEqualTo(value1);
    }

    @Test
    void swapMoveWithSameValue() {
        // Arrange
        var solution = TestdataSolution.generateSolution(2, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var value = solution.getValueList().get(0);
        
        // Set both entities to the same value
        entity1.setValue(value);
        entity2.setValue(value);
        
        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor().getMetaModel();
        @SuppressWarnings("unchecked")
        var variableMetaModel = (ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel<TestdataSolution, TestdataEntity, Object>)
                (Object) solutionMetaModel.entity(TestdataEntity.class).basicVariable("value", TestdataValue.class);
        
        var swapMove = Moves.swap(Collections.singletonList(variableMetaModel), entity1, entity2);
        
        // Act
        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            var context = runner.using(solution);
            context.execute(swapMove);
        }
        
        // Assert - both should still have the same value
        assertThat(entity1.getValue()).isEqualTo(value);
        assertThat(entity2.getValue()).isEqualTo(value);
    }
    
    @Test
    void multipleSwapMoves() {
        // Arrange - 3 entities with different values
        var solution = TestdataSolution.generateSolution(3, 3);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var entity3 = solution.getEntityList().get(2);
        var initialValue2 = entity2.getValue();
        var initialValue3 = entity3.getValue();
        
        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class).basicVariable("value", TestdataValue.class);
        
        var swap1 = Moves.swap(variableMetaModel, entity1, entity2);
        var swap2 = Moves.swap(variableMetaModel, entity2, entity3);
        
        // Act - swap 1-2, then swap 2-3
        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            var context = runner.using(solution);
            context.execute(swap1);
            context.execute(swap2);
        }
        
        // Assert - entity1 has value2, entity2 has value3, entity3 has value2
        assertThat(entity1.getValue()).isEqualTo(initialValue2);
        assertThat(entity2.getValue()).isEqualTo(initialValue3);
        assertThat(entity3.getValue()).isEqualTo(initialValue2);
    }
    
    // ************************************************************************
    // Temporary execution tests (T035)
    // ************************************************************************
    
    @Test
    void swapMoveExecutesTemporarilyWithUndo() {
        // Arrange
        var solution = TestdataSolution.generateSolution(2, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var originalValue1 = entity1.getValue();
        var originalValue2 = entity2.getValue();
        
        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor().getMetaModel();
        @SuppressWarnings("unchecked")
        var variableMetaModel = (ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel<TestdataSolution, TestdataEntity, Object>)
                (Object) solutionMetaModel.entity(TestdataEntity.class).basicVariable("value", TestdataValue.class);
        
        var swapMove = Moves.swap(Collections.singletonList(variableMetaModel), entity1, entity2);
        
        // Act & Assert
        try (var runner = MoveRunner.build(TestdataSolution.class, TestdataEntity.class)) {
            var context = runner.using(solution);
            
            context.executeTemporarily(swapMove, view -> {
                // During temporary execution, values should be swapped
                assertThat(entity1.getValue()).isEqualTo(originalValue2);
                assertThat(entity2.getValue()).isEqualTo(originalValue1);
            });
            
            // After undo, values should be restored
            assertThat(entity1.getValue()).isEqualTo(originalValue1);
            assertThat(entity2.getValue()).isEqualTo(originalValue2);
        }
    }
    
}
