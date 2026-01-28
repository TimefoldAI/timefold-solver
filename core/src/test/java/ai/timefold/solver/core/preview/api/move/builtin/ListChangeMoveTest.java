package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.preview.api.move.MoveRunner;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

import org.junit.jupiter.api.Test;

class ListChangeMoveTest {

    @Test
    void listChangeMoveWithinSameEntity() {
        var solution = TestdataListSolution.generateInitializedSolution(3, 1);
        var entity = solution.getEntityList().get(0);
        var value1 = entity.getValueList().get(0);
        var value2 = entity.getValueList().get(1);
        var value3 = entity.getValueList().get(2);

        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var changeMove = Moves.change(variableMetaModel, entity, 0, entity, 2);

        MoveRunner.build(solutionMetaModel)
                .using(solution)
                .execute(changeMove);

        // Assert - order should be: value2, value3, value1
        assertThat(entity.getValueList()).hasSize(3);
        assertThat(entity.getValueList().get(0)).isEqualTo(value2);
        assertThat(entity.getValueList().get(1)).isEqualTo(value3);
        assertThat(entity.getValueList().get(2)).isEqualTo(value1);
    }

    @Test
    void listChangeMoveBetweenEntities() {
        var solution = TestdataListSolution.generateInitializedSolution(4, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);

        var initialEntity1Size = entity1.getValueList().size();
        var initialEntity2Size = entity2.getValueList().size();
        var valueToMove = entity1.getValueList().get(0);

        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var changeMove = Moves.change(variableMetaModel, entity1, 0, entity2, 0);

        MoveRunner.build(solutionMetaModel)
                .using(solution)
                .execute(changeMove);

        // Assert
        assertThat(entity1.getValueList()).hasSize(initialEntity1Size - 1);
        assertThat(entity2.getValueList()).hasSize(initialEntity2Size + 1);
        assertThat(entity2.getValueList().get(0)).isEqualTo(valueToMove);
    }

    @Test
    void multipleListChangeMoves() {
        var solution = TestdataListSolution.generateInitializedSolution(6, 3);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var entity3 = solution.getEntityList().get(2);

        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var move1 = Moves.change(variableMetaModel, entity1, 0, entity2, 0);
        var move2 = Moves.change(variableMetaModel, entity2, 0, entity3, 0);

        var valueToMove = entity1.getValueList().get(0);

        var context = MoveRunner.build(solutionMetaModel)
                .using(solution);
        context.execute(move1);
        context.execute(move2);

        // Assert - value ends up in entity3
        assertThat(entity3.getValueList().get(0)).isEqualTo(valueToMove);
    }

    @Test
    void listChangeMoveExecutesTemporarilyWithUndo() {
        var solution = TestdataListSolution.generateInitializedSolution(3, 1);
        var entity = solution.getEntityList().get(0);
        var originalList = entity.getValueList().stream().toList(); // Copy list

        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var changeMove = Moves.change(variableMetaModel, entity, 0, entity, 2);

        MoveRunner.build(solutionMetaModel)
                .using(solution)
                .executeTemporarily(changeMove, view -> {
                    // During temporary execution, order should be changed
                    assertThat(entity.getValueList()).hasSize(3);
                    assertThat(entity.getValueList().get(0)).isEqualTo(originalList.get(1));
                    assertThat(entity.getValueList().get(1)).isEqualTo(originalList.get(2));
                    assertThat(entity.getValueList().get(2)).isEqualTo(originalList.get(0));
                });

        // After undo, list should be restored
        assertThat(entity.getValueList()).containsExactlyElementsOf(originalList);
    }

    @Test
    void listChangeMoveTemporaryBetweenEntities() {
        var solution = TestdataListSolution.generateInitializedSolution(4, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var originalList1 = entity1.getValueList().stream().toList();
        var originalList2 = entity2.getValueList().stream().toList();

        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var changeMove = Moves.change(variableMetaModel, entity1, 0, entity2, 0);

        MoveRunner.build(solutionMetaModel)
                .using(solution)
                .executeTemporarily(changeMove, view -> {
                    // Value should be moved from entity1 to entity2
                    assertThat(entity1.getValueList()).hasSize(originalList1.size() - 1);
                    assertThat(entity2.getValueList()).hasSize(originalList2.size() + 1);
                    assertThat(entity2.getValueList().get(0)).isEqualTo(originalList1.get(0));
                });

        // Both lists should be restored
        assertThat(entity1.getValueList()).containsExactlyElementsOf(originalList1);
        assertThat(entity2.getValueList()).containsExactlyElementsOf(originalList2);
    }
}
