package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.preview.api.move.MoveTester;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

import org.junit.jupiter.api.Test;

class ListAssignMoveTest {

    @Test
    void listAssignMoveExecutesPermanently() {
        var solution = TestdataListSolution.generateUninitializedSolution(3, 2);
        var entity = solution.getEntityList().get(0);
        var value = solution.getValueList().get(0);

        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var assignMove = Moves.assign(variableMetaModel, value, entity, 0);

        MoveTester.build(solutionMetaModel).using(solution)
                .execute(assignMove);

        assertThat(entity.getValueList()).hasSize(1);
        assertThat(entity.getValueList().get(0)).isEqualTo(value);
    }

    @Test
    void listAssignMoveMultipleValues() {
        var solution = TestdataListSolution.generateUninitializedSolution(4, 1);
        var entity = solution.getEntityList().get(0);
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);
        var value3 = solution.getValueList().get(2);

        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var move1 = Moves.assign(variableMetaModel, value1, entity, 0);
        var move2 = Moves.assign(variableMetaModel, value2, entity, 1);
        var move3 = Moves.assign(variableMetaModel, value3, entity, 1); // Insert at position 1

        var context = MoveTester.build(solutionMetaModel)
                .using(solution);
        context.execute(move1);
        context.execute(move2);
        context.execute(move3);

        // Assert - order should be: value1, value3, value2
        assertThat(entity.getValueList()).hasSize(3);
        assertThat(entity.getValueList().get(0)).isEqualTo(value1);
        assertThat(entity.getValueList().get(1)).isEqualTo(value3);
        assertThat(entity.getValueList().get(2)).isEqualTo(value2);
    }

    @Test
    void listAssignMoveToMultipleEntities() {
        var solution = TestdataListSolution.generateUninitializedSolution(3, 3);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var entity3 = solution.getEntityList().get(2);
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);
        var value3 = solution.getValueList().get(2);

        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var move1 = Moves.assign(variableMetaModel, value1, entity1, 0);
        var move2 = Moves.assign(variableMetaModel, value2, entity2, 0);
        var move3 = Moves.assign(variableMetaModel, value3, entity3, 0);

        var context = MoveTester.build(solutionMetaModel)
                .using(solution);
        context.execute(move1);
        context.execute(move2);
        context.execute(move3);

        assertThat(entity1.getValueList()).containsExactly(value1);
        assertThat(entity2.getValueList()).containsExactly(value2);
        assertThat(entity3.getValueList()).containsExactly(value3);
    }

    @Test
    void listAssignMoveExecutesTemporarilyWithUndo() {
        var solution = TestdataListSolution.generateUninitializedSolution(2, 1);
        var entity = solution.getEntityList().get(0);
        var value = solution.getValueList().get(0);

        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var assignMove = Moves.assign(variableMetaModel, value, entity, 0);

        MoveTester.build(solutionMetaModel)
                .using(solution)
                .executeTemporarily(assignMove, view -> {
                    // During temporary execution, value should be assigned
                    assertThat(entity.getValueList()).hasSize(1);
                    assertThat(entity.getValueList().get(0)).isEqualTo(value);
                });

        // After undo, list should be empty again
        assertThat(entity.getValueList()).isEmpty();
    }

    @Test
    void listAssignMoveTemporaryMultipleValues() {
        var solution = TestdataListSolution.generateUninitializedSolution(3, 1);
        var entity = solution.getEntityList().get(0);
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);
        var value3 = solution.getValueList().get(2);

        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var move1 = Moves.assign(variableMetaModel, value1, entity, 0);
        var move2 = Moves.assign(variableMetaModel, value2, entity, 1);
        var move3 = Moves.assign(variableMetaModel, value3, entity, 1);
        var compositeMove = Moves.compose(move1, move2, move3);

        MoveTester.build(solutionMetaModel)
                .using(solution)
                .executeTemporarily(compositeMove, view -> {
                    // All assignments should be applied
                    assertThat(entity.getValueList()).hasSize(3);
                    assertThat(entity.getValueList().get(0)).isEqualTo(value1);
                    assertThat(entity.getValueList().get(1)).isEqualTo(value3);
                    assertThat(entity.getValueList().get(2)).isEqualTo(value2);
                });

        // All should be undone
        assertThat(entity.getValueList()).isEmpty();
    }
}
