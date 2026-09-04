package ai.timefold.solver.core.impl.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Range;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SubListChangeMoveTest {

    @Nested
    class MoveWithinSameEntity {

        @Test
        void moveSpanForward() {
            var value1 = new TestdataListValue("A");
            var value2 = new TestdataListValue("B");
            var value3 = new TestdataListValue("C");
            var value4 = new TestdataListValue("D");
            var value5 = new TestdataListValue("E");
            var entity = new TestdataListEntity("Entity", value1, value2, value3, value4, value5);
            var solution = new TestdataListSolution();
            solution.setEntityList(List.of(entity));
            solution.setValueList(List.of(value1, value2, value3, value4, value5));

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var move = Moves.change(variableMetaModel, new Range<>(entity, 1, 3), ElementPosition.of(entity, 3), false);

            MoveTester.build(solutionMetaModel)
                    .using(solution)
                    .execute(move);

            assertThat(entity.getValueList()).containsExactly(value1, value4, value5, value2, value3);
        }

        @Test
        void moveSpanBackwardReversing() {
            var value1 = new TestdataListValue("A");
            var value2 = new TestdataListValue("B");
            var value3 = new TestdataListValue("C");
            var value4 = new TestdataListValue("D");
            var value5 = new TestdataListValue("E");
            var entity = new TestdataListEntity("Entity", value1, value2, value3, value4, value5);
            var solution = new TestdataListSolution();
            solution.setEntityList(List.of(entity));
            solution.setValueList(List.of(value1, value2, value3, value4, value5));

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var move = Moves.change(variableMetaModel, new Range<>(entity, 3, 5), ElementPosition.of(entity, 0), true);

            MoveTester.build(solutionMetaModel)
                    .using(solution)
                    .execute(move);

            assertThat(entity.getValueList()).containsExactly(value5, value4, value1, value2, value3);
        }

    }

    @Nested
    class MoveBetweenEntities {

        @Test
        void moveSpanToOtherEntity() {
            var valueA1 = new TestdataListValue("A1");
            var valueA2 = new TestdataListValue("A2");
            var valueA3 = new TestdataListValue("A3");
            var valueA4 = new TestdataListValue("A4");
            var entityA = new TestdataListEntity("EntityA", valueA1, valueA2, valueA3, valueA4);
            var valueB1 = new TestdataListValue("B1");
            var valueB2 = new TestdataListValue("B2");
            var valueB3 = new TestdataListValue("B3");
            var entityB = new TestdataListEntity("EntityB", valueB1, valueB2, valueB3);
            var solution = new TestdataListSolution();
            solution.setEntityList(List.of(entityA, entityB));
            solution.setValueList(List.of(valueA1, valueA2, valueA3, valueA4, valueB1, valueB2, valueB3));

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var move =
                    Moves.change(variableMetaModel, new Range<>(entityA, 1, 3), ElementPosition.of(entityB, 1), false);

            MoveTester.build(solutionMetaModel)
                    .using(solution)
                    .execute(move);

            assertThat(entityA.getValueList()).containsExactly(valueA1, valueA4);
            assertThat(entityB.getValueList()).containsExactly(valueB1, valueA2, valueA3, valueB2, valueB3);
        }

        @Test
        void moveSpanToUnassignedIsSubListUnassignMove() {
            var value1 = new TestdataListValue("A");
            var value2 = new TestdataListValue("B");
            var value3 = new TestdataListValue("C");
            var entity = new TestdataListEntity("Entity", value1, value2, value3);
            var solution = new TestdataListSolution();
            solution.setEntityList(List.of(entity));
            solution.setValueList(List.of(value1, value2, value3));

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var move = Moves.unassign(variableMetaModel, new Range<>(entity, 0, 2));

            MoveTester.build(solutionMetaModel)
                    .using(solution)
                    .execute(move);

            assertThat(entity.getValueList()).containsExactly(value3);
        }

    }

    @Nested
    class TemporaryExecution {

        @Test
        void executeTemporarilyThenUndo() {
            var value1 = new TestdataListValue("A");
            var value2 = new TestdataListValue("B");
            var value3 = new TestdataListValue("C");
            var entity = new TestdataListEntity("Entity", value1, value2, value3);
            var solution = new TestdataListSolution();
            solution.setEntityList(List.of(entity));
            solution.setValueList(List.of(value1, value2, value3));

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            // destinationIndex is post-removal: after removing [A, B], the list is [C] (size 1), so index 1 appends.
            var move = Moves.change(variableMetaModel, new Range<>(entity, 0, 2), ElementPosition.of(entity, 1), false);

            MoveTester.build(solutionMetaModel)
                    .using(solution)
                    .executeTemporarily(move, view -> assertThat(entity.getValueList()).containsExactly(value3, value1,
                            value2));

            // Undone automatically after the callback returns.
            assertThat(entity.getValueList()).containsExactly(value1, value2, value3);
        }

    }

    @Nested
    class MoveProperties {

        @Test
        void getPlanningEntitiesForSameEntityMove() {
            var entity = new TestdataListEntity("Entity", new TestdataListValue("A"), new TestdataListValue("B"));
            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var move = (SubListChangeMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) Moves.change(
                    variableMetaModel, new Range<>(entity, 0, 1), ElementPosition.of(entity, 2), false);

            assertThat(move.getPlanningEntities()).containsExactly(entity);
        }

        @Test
        void getPlanningEntitiesForDifferentEntitiesMove() {
            var entityA = new TestdataListEntity("EntityA", new TestdataListValue("A"));
            var entityB = new TestdataListEntity("EntityB", new TestdataListValue("B"));
            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var move = (SubListChangeMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) Moves.change(
                    variableMetaModel, new Range<>(entityA, 0, 1), ElementPosition.of(entityB, 0), false);

            assertThat(move.getPlanningEntities()).containsExactlyInAnyOrder(entityA, entityB);
        }

        @Test
        void getPlanningValuesReturnsMovedValues() {
            var value1 = new TestdataListValue("A");
            var value2 = new TestdataListValue("B");
            var value3 = new TestdataListValue("C");
            var entity = new TestdataListEntity("Entity", value1, value2, value3);
            var solution = new TestdataListSolution();
            solution.setEntityList(List.of(entity));
            solution.setValueList(List.of(value1, value2, value3));

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var move = (SubListChangeMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) Moves.change(
                    variableMetaModel, new Range<>(entity, 0, 2), ElementPosition.of(entity, 1), false);

            MoveTester.build(solutionMetaModel)
                    .using(solution)
                    .execute(move);

            assertThat(move.getPlanningValues()).containsExactly(value1, value2);
        }

        @Test
        void equalsAndHashCode() {
            var entityA = new TestdataListEntity("EntityA", new TestdataListValue("A"));
            var entityB = new TestdataListEntity("EntityB", new TestdataListValue("B"));
            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var move1 = Moves.change(variableMetaModel, new Range<>(entityA, 0, 1), ElementPosition.of(entityB, 0), false);
            var move2 = Moves.change(variableMetaModel, new Range<>(entityA, 0, 1), ElementPosition.of(entityB, 0), false);
            var move3 = Moves.change(variableMetaModel, new Range<>(entityA, 0, 1), ElementPosition.of(entityB, 0), true);

            assertThat(move1).isEqualTo(move2);
            assertThat(move1.hashCode()).isEqualTo(move2.hashCode());
            assertThat(move1).isNotEqualTo(move3);
            assertThat(move1).isNotEqualTo(null);
            assertThat(move1).isNotEqualTo("not a move");
        }

        @Test
        void toStringContainsMoveDetails() {
            var entityA = new TestdataListEntity("EntityA", new TestdataListValue("A"), new TestdataListValue("B"));
            var entityB = new TestdataListEntity("EntityB", new TestdataListValue("C"));
            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var move = Moves.change(variableMetaModel, new Range<>(entityA, 0, 2), ElementPosition.of(entityB, 1), true);

            var toString = move.toString();
            assertThat(toString).contains("EntityA").contains("EntityB").contains("reversing-");
        }

    }

    @Nested
    class Rebase {

        @Test
        void rebaseCreatesNewMoveWithRebasedEntities() {
            var entityA = new TestdataListEntity("EntityA", new TestdataListValue("A"));
            var entityB = new TestdataListEntity("EntityB", new TestdataListValue("B"));
            var rebasedEntityA = new TestdataListEntity("EntityA", new TestdataListValue("A"));
            var rebasedEntityB = new TestdataListEntity("EntityB", new TestdataListValue("B"));

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var originalMove = (SubListChangeMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) Moves
                    .change(variableMetaModel, new Range<>(entityA, 0, 1), ElementPosition.of(entityB, 0), false);

            var rebasedMove = originalMove.rebase(new Lookup() {
                @Override
                @SuppressWarnings("unchecked")
                public <T> T lookUpWorkingObject(T object) {
                    if (object == entityA) {
                        return (T) rebasedEntityA;
                    } else if (object == entityB) {
                        return (T) rebasedEntityB;
                    }
                    return object;
                }
            });

            assertThat(rebasedMove.getSource().entity()).isEqualTo(rebasedEntityA);
            assertThat(rebasedMove.getDestination().<TestdataListEntity> entity()).isEqualTo(rebasedEntityB);
        }

    }

}
