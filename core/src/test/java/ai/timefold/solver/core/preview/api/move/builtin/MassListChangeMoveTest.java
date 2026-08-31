package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;

import org.junit.jupiter.api.Test;

class MassListChangeMoveTest {

    @Test
    void executeGathersEveryMemberConsecutivelyAtDestination() {
        var a0 = new TestdataListValue("a0");
        var a1 = new TestdataListValue("a1");
        var b0 = new TestdataListValue("b0");
        var b1 = new TestdataListValue("b1");
        var c0 = new TestdataListValue("c0");
        var c1 = new TestdataListValue("c1");
        var c2 = new TestdataListValue("c2");
        var entityA = new TestdataListEntity("A", a0, a1);
        var entityB = new TestdataListEntity("B", b0, b1);
        var entityC = new TestdataListEntity("C", c0, c1, c2);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(entityA, entityB, entityC));
        solution.setValueList(List.of(a0, a1, b0, b1, c0, c1, c2));

        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        // Draw order a1, b0 - the destination gathers them in that order, not sorted by origin.
        var sample = Sample.of(List.of(a1, b0));
        var move = Moves.massChange(variableMetaModel, sample, ElementPosition.of(entityC, 1));

        MoveTester.build(solutionMetaModel)
                .using(solution)
                .execute(move);

        assertThat(entityA.getValueList()).containsExactly(a0);
        assertThat(entityB.getValueList()).containsExactly(b1);
        assertThat(entityC.getValueList()).containsExactly(c0, a1, b0, c1, c2);
    }

    @Test
    void destinationIndexIsAdjustedOnlyByMembersOriginallyBeforeIt() {
        // A single-pass (mutate-while-reading) implementation double-counts c and d
        // (which sit AT and AFTER the destination index, not before it)
        // once a has already been removed and their live indices have shifted down:
        // it would count all three of a, c, d as "before" (removedBeforeDestination = 3),
        // driving the adjusted index to -1 - an out-of-bounds insertion.
        // The two-pass fix reads every member's ORIGINAL position first,
        // correctly counting only a (removedBeforeDestination = 1),
        // and produces a real, verifiable rearrangement rather than a coincidental no-op:
        // b - the one untouched value before the destination - ends up first, with a, c, d gathered right after it.
        var a = new TestdataListValue("a");
        var b = new TestdataListValue("b");
        var c = new TestdataListValue("c");
        var d = new TestdataListValue("d");
        var e = new TestdataListValue("e");
        var entity = new TestdataListEntity("C", a, b, c, d, e);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(a, b, c, d, e));

        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var sample = Sample.of(List.of(a, c, d));
        var move = Moves.massChange(variableMetaModel, sample, ElementPosition.of(entity, 2));

        MoveTester.build(solutionMetaModel)
                .using(solution)
                .execute(move);

        assertThat(entity.getValueList()).containsExactly(b, a, c, d, e);
    }

    @Test
    void executeAssignsCurrentlyUnassignedMembersToo() {
        var assigned = new TestdataAllowsUnassignedValuesListValue("assigned");
        var unassigned = new TestdataAllowsUnassignedValuesListValue("unassigned");
        var entityA = new TestdataAllowsUnassignedValuesListEntity("A", assigned);
        var entityB = new TestdataAllowsUnassignedValuesListEntity("B");
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entityA, entityB));
        solution.setValueList(List.of(assigned, unassigned));

        var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                .listVariable("valueList", TestdataAllowsUnassignedValuesListValue.class);

        var sample = Sample.of(List.of(unassigned, assigned));
        var move = Moves.massChange(variableMetaModel, sample, ElementPosition.of(entityB, 0));

        MoveTester.build(solutionMetaModel)
                .using(solution)
                .execute(move);

        assertThat(entityA.getValueList()).isEmpty();
        assertThat(entityB.getValueList()).containsExactly(unassigned, assigned);
    }

    @Test
    void executeToNullDestinationUnassignsEveryMember() {
        var value1 = new TestdataAllowsUnassignedValuesListValue("v1");
        var value2 = new TestdataAllowsUnassignedValuesListValue("v2");
        var entity = new TestdataAllowsUnassignedValuesListEntity("A", value1, value2);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(value1, value2));

        var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                .listVariable("valueList", TestdataAllowsUnassignedValuesListValue.class);

        var sample = Sample.of(List.of(value1, value2));
        var move = Moves.massChange(variableMetaModel, sample, null);

        MoveTester.build(solutionMetaModel)
                .using(solution)
                .execute(move);

        assertThat(entity.getValueList()).isEmpty();
    }

    @Test
    void undoRestoresEveryMembersOriginalPosition() {
        var a0 = new TestdataListValue("a0");
        var a1 = new TestdataListValue("a1");
        var b0 = new TestdataListValue("b0");
        var entityA = new TestdataListEntity("A", a0, a1);
        var entityB = new TestdataListEntity("B", b0);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(entityA, entityB));
        solution.setValueList(List.of(a0, a1, b0));

        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var sample = Sample.of(List.of(a1));
        var move = Moves.massChange(variableMetaModel, sample, ElementPosition.of(entityB, 0));

        MoveTester.build(solutionMetaModel)
                .using(solution)
                .executeTemporarily(move,
                        view -> assertThat(entityB.getValueList()).containsExactly(a1, b0));

        assertThat(entityA.getValueList()).containsExactly(a0, a1);
        assertThat(entityB.getValueList()).containsExactly(b0);
    }

    @Test
    void equalsAndHashCodeIgnoreMemberDrawOrderWhenUnassigning() {
        var value1 = new TestdataListValue("v1");
        var value2 = new TestdataListValue("v2");

        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        // A null destination unassigns every member; order does not affect the resulting solution.
        var move1 = Moves.massChange(variableMetaModel, Sample.of(List.of(value1, value2)), null);
        var move2 = Moves.massChange(variableMetaModel, Sample.of(List.of(value2, value1)), null);

        assertThat(move1).isEqualTo(move2);
        assertThat(move1.hashCode()).isEqualTo(move2.hashCode());
        assertThat(move1).isNotEqualTo(null);
        assertThat(move1).isNotEqualTo("not a move");
    }

    @Test
    void equalsAndHashCodeRespectMemberDrawOrderWhenInserting() {
        var value1 = new TestdataListValue("v1");
        var value2 = new TestdataListValue("v2");
        var entity = new TestdataListEntity("A", value1, value2);

        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var destination = ElementPosition.of(entity, 0);
        var otherDestination = ElementPosition.of(entity, 1);
        var move1 = Moves.massChange(variableMetaModel, Sample.of(List.of(value1, value2)), destination);
        var move1SameOrder = Moves.massChange(variableMetaModel, Sample.of(List.of(value1, value2)), destination);
        // Insertion is order-sensitive, so equality must be too:
        // Move requires that equal moves produce the exact same solution.
        var move2DifferentOrder = Moves.massChange(variableMetaModel, Sample.of(List.of(value2, value1)), destination);
        var move3 = Moves.massChange(variableMetaModel, Sample.of(List.of(value1, value2)), otherDestination);
        var move4 = Moves.massChange(variableMetaModel, Sample.of(List.of(value1, value2)), null);

        assertThat(move1).isEqualTo(move1SameOrder);
        assertThat(move1.hashCode()).isEqualTo(move1SameOrder.hashCode());
        assertThat(move1).isNotEqualTo(move2DifferentOrder);
        assertThat(move1).isNotEqualTo(move3);
        assertThat(move1).isNotEqualTo(move4);
        assertThat(move1).isNotEqualTo(null);
        assertThat(move1).isNotEqualTo("not a move");
    }

    @Test
    void rebaseReturnsMoveWhoseSampleHoldsWorkingSolutionCopies() {
        var value1 = new TestdataListValue("v1");
        var value2 = new TestdataListValue("v2");
        var rebasedValue1 = new TestdataListValue("v1");
        var rebasedValue2 = new TestdataListValue("v2");
        var entity = new TestdataListEntity("A", value1, value2);
        var rebasedEntity = new TestdataListEntity("A", rebasedValue1, rebasedValue2);

        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var originalMove =
                (MassListChangeMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) Moves.massChange(
                        variableMetaModel, Sample.of(List.of(value1, value2)), ElementPosition.of(entity, 0));

        var rebasedMove = originalMove.rebase(new Lookup() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T lookUpWorkingObject(T object) {
                if (object == value1) {
                    return (T) rebasedValue1;
                } else if (object == value2) {
                    return (T) rebasedValue2;
                } else if (object == entity) {
                    return (T) rebasedEntity;
                }
                return object;
            }
        });

        var rebasedSample = new ArrayList<>();
        for (var value : rebasedMove.getSample()) {
            rebasedSample.add(value);
        }
        assertThat(rebasedSample).containsExactlyInAnyOrder(rebasedValue1, rebasedValue2);
        assertThat(rebasedMove.getDestination()).isNotNull();
        assertThat(rebasedMove.getDestination().<TestdataListEntity> entity()).isEqualTo(rebasedEntity);
    }

}
