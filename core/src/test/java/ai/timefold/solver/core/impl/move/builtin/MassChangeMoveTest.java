package ai.timefold.solver.core.impl.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.junit.jupiter.api.Test;

class MassChangeMoveTest {

    @Test
    void executeSetsEveryMemberToDestinationValue() {
        var solution = TestdataSolution.generateSolution(3, 3);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var newValue = solution.getValueList().get(2); // Different value

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var sample = Sample.of(List.of(entity1, entity2));
        var massChangeMove = Moves.massChange(variableMetaModel, sample, newValue);

        MoveTester.build(solutionMetaModel)
                .using(solution)
                .execute(massChangeMove);

        assertThat(entity1.getValue()).isEqualTo(newValue);
        assertThat(entity2.getValue()).isEqualTo(newValue);
    }

    @Test
    void executeToNullUnassignsEveryMember() {
        var solution = TestdataSolution.generateSolution(3, 2);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var sample = Sample.of(List.of(entity1, entity2));
        var massChangeMove = Moves.massChange(variableMetaModel, sample, null);

        MoveTester.build(solutionMetaModel)
                .using(solution)
                .execute(massChangeMove);

        assertThat(entity1.getValue()).isNull();
        assertThat(entity2.getValue()).isNull();
    }

    @Test
    void undoRestoresEveryMembersOriginalValue() {
        var solution = TestdataSolution.generateSolution(3, 3);
        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);
        var originalValue1 = entity1.getValue();
        var originalValue2 = entity2.getValue();
        var newValue = solution.getValueList().get(2);

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var sample = Sample.of(List.of(entity1, entity2));
        var massChangeMove = Moves.massChange(variableMetaModel, sample, newValue);

        MoveTester.build(solutionMetaModel)
                .using(solution)
                .executeTemporarily(massChangeMove, view -> {
                    assertThat(entity1.getValue()).isEqualTo(newValue);
                    assertThat(entity2.getValue()).isEqualTo(newValue);
                });

        assertThat(entity1.getValue()).isEqualTo(originalValue1);
        assertThat(entity2.getValue()).isEqualTo(originalValue2);
    }

    @Test
    void getPlanningEntitiesReturnsEveryMember() {
        var entity1 = new TestdataEntity("e1");
        var entity2 = new TestdataEntity("e2");
        var value = new TestdataValue("v");

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var sample = Sample.of(List.of(entity1, entity2));
        var massChangeMove = Moves.massChange(variableMetaModel, sample, value);

        assertThat(massChangeMove.getPlanningEntities()).containsExactlyInAnyOrder(entity1, entity2);
    }

    @Test
    void getPlanningValuesReturnsDestination() {
        var entity1 = new TestdataEntity("e1");
        var value = new TestdataValue("v");

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var sample = Sample.of(List.of(entity1));
        var massChangeMove = Moves.massChange(variableMetaModel, sample, value);

        assertThat(massChangeMove.getPlanningValues()).containsExactly(value);
    }

    @Test
    void equalsAndHashCodeIgnoreMemberDrawOrder() {
        var entity1 = new TestdataEntity("e1");
        var entity2 = new TestdataEntity("e2");
        var value1 = new TestdataValue("v1");
        var value2 = new TestdataValue("v2");

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var move1 = Moves.massChange(variableMetaModel, Sample.of(List.of(entity1, entity2)), value1);
        var move2 = Moves.massChange(variableMetaModel, Sample.of(List.of(entity2, entity1)), value1);
        var move3 = Moves.massChange(variableMetaModel, Sample.of(List.of(entity1, entity2)), value2);

        // Same members in a different draw order plus the same destination are equal.
        assertThat(move1).isEqualTo(move2);
        assertThat(move1.hashCode()).isEqualTo(move2.hashCode());

        // A different destination is not equal.
        assertThat(move1).isNotEqualTo(move3);

        assertThat(move1).isNotEqualTo(null);
        assertThat(move1).isNotEqualTo("not a move");
    }

    @Test
    void describeIncludesEntityAndVariableName() {
        var entity = new TestdataEntity("e1");
        var value = new TestdataValue("v");

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var massChangeMove = Moves.massChange(variableMetaModel, Sample.of(List.of(entity)), value);

        assertThat(massChangeMove.describe()).isEqualTo("MassChangeMove(TestdataEntity.value)");
    }

    @Test
    void rebaseReturnsMoveWhoseSampleHoldsWorkingSolutionCopies() {
        var entity1 = new TestdataEntity("e1");
        var entity2 = new TestdataEntity("e2");
        var rebasedEntity1 = new TestdataEntity("e1");
        var rebasedEntity2 = new TestdataEntity("e2");
        var value = new TestdataValue("v");

        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var originalMove =
                (MassChangeMove<TestdataSolution, TestdataEntity, TestdataValue>) Moves.massChange(variableMetaModel,
                        Sample.of(List.of(entity1, entity2)), value);

        var rebasedMove = originalMove.rebase(new Lookup() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T lookUpWorkingObject(T object) {
                if (object == entity1) {
                    return (T) rebasedEntity1;
                } else if (object == entity2) {
                    return (T) rebasedEntity2;
                }
                return object;
            }
        });

        assertThat(rebasedMove.getPlanningEntities()).containsExactlyInAnyOrder(rebasedEntity1, rebasedEntity2);
        assertThat(rebasedMove.getPlanningValues()).containsExactly(value);
    }

}
