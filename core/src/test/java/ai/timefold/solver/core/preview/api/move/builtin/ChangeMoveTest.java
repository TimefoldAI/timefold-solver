package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.preview.api.move.MoveRunner;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.junit.jupiter.api.Test;

class ChangeMoveTest {

    @Test
    void changeMoveExecutesPermanently() {
        var solution = TestdataSolution.generateSolution(3, 2);
        var entity = solution.getEntityList().get(0);
        var newValue = solution.getValueList().get(2); // Different value

        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class).basicVariable("value", TestdataValue.class);

        var changeMove = Moves.change(variableMetaModel, entity, newValue);

        MoveRunner.build(TestdataSolution.class, TestdataEntity.class).using(solution).execute(changeMove);

        assertThat(entity.getValue()).isEqualTo(newValue);
    }

    @Test
    void changeMoveToNull() {
        var solution = TestdataSolution.generateSolution(2, 1);
        var entity = solution.getEntityList().get(0);

        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class).basicVariable("value", TestdataValue.class);

        var changeMove = Moves.change(variableMetaModel, entity, null);

        MoveRunner.build(TestdataSolution.class, TestdataEntity.class).using(solution).execute(changeMove);

        assertThat(entity.getValue()).isNull();
    }

    @Test
    void changeMoveMultipleEntities() {
        var solution = TestdataSolution.generateSolution(3, 3);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var entity3 = solution.getEntityList().get(2);
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);

        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class).basicVariable("value", TestdataValue.class);

        var move1 = Moves.change(variableMetaModel, entity1, value2);
        var move2 = Moves.change(variableMetaModel, entity2, value1);
        var move3 = Moves.change(variableMetaModel, entity3, value2);

        var context = MoveRunner.build(TestdataSolution.class, TestdataEntity.class).using(solution);
        context.execute(move1);
        context.execute(move2);
        context.execute(move3);

        assertThat(entity1.getValue()).isEqualTo(value2);
        assertThat(entity2.getValue()).isEqualTo(value1);
        assertThat(entity3.getValue()).isEqualTo(value2);
    }

    @Test
    void changeMoveExecutesTemporarilyWithUndo() {
        var solution = TestdataSolution.generateSolution(3, 1);
        var entity = solution.getEntityList().get(0);
        var originalValue = entity.getValue();
        var newValue = solution.getValueList().get(2);

        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class).basicVariable("value", TestdataValue.class);

        var changeMove = Moves.change(variableMetaModel, entity, newValue);

        MoveRunner.build(TestdataSolution.class, TestdataEntity.class)
                .using(solution)
                .executeTemporarily(changeMove, view -> {
                    // During temporary execution, value should be changed
                    assertThat(entity.getValue()).isEqualTo(newValue);
                });

        // After undo, value should be restored
        assertThat(entity.getValue()).isEqualTo(originalValue);
    }

    @Test
    void changeMoveTemporaryMultipleEntities() {
        var solution = TestdataSolution.generateSolution(3, 3);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var entity3 = solution.getEntityList().get(2);
        var originalValue1 = entity1.getValue();
        var originalValue2 = entity2.getValue();
        var originalValue3 = entity3.getValue();
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);

        var solutionMetaModel = TestdataSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.entity(TestdataEntity.class).basicVariable("value", TestdataValue.class);

        var move1 = Moves.change(variableMetaModel, entity1, value2);
        var move2 = Moves.change(variableMetaModel, entity2, value1);
        var move3 = Moves.change(variableMetaModel, entity3, value2);
        var compositeMove = Moves.compose(move1, move2, move3);

        MoveRunner.build(TestdataSolution.class, TestdataEntity.class)
                .using(solution)
                .executeTemporarily(compositeMove, view -> {
                    // All changes should be applied
                    assertThat(entity1.getValue()).isEqualTo(value2);
                    assertThat(entity2.getValue()).isEqualTo(value1);
                    assertThat(entity3.getValue()).isEqualTo(value2);
                });

        // All changes should be undone
        assertThat(entity1.getValue()).isEqualTo(originalValue1);
        assertThat(entity2.getValue()).isEqualTo(originalValue2);
        assertThat(entity3.getValue()).isEqualTo(originalValue3);

    }
}
