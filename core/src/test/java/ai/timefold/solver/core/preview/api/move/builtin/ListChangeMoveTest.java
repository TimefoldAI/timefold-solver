package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.preview.api.move.MoveTester;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ListChangeMoveTest {

    /**
     * Tests for moving a value within the same entity's list.
     * As per {@link MutableSolutionView#moveValueInList}:
     * "Behaves as if the value is first removed from the source index,
     * shifting all later values to the left,
     * and then inserted at the destination index."
     */
    @Nested
    class MoveWithinSameEntity {

        /**
         * Moving from index 0 to index 2 in list [A, B, C].
         * After removal of A: [B, C]
         * After insertion at index 2: [B, C, A]
         */
        @Test
        void moveFromFirstToLast() {
            var solution = TestdataListSolution.generateInitializedSolution(3, 1);
            var entity = solution.getEntityList().get(0);
            var value1 = entity.getValueList().get(0);
            var value2 = entity.getValueList().get(1);
            var value3 = entity.getValueList().get(2);

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var changeMove = Moves.change(variableMetaModel, entity, 0, entity, 2);

            MoveTester.build(solutionMetaModel)
                    .using(solution)
                    .execute(changeMove);

            // After removal: [B, C], then insert at 2: [B, C, A]
            assertThat(entity.getValueList()).containsExactly(value2, value3, value1);
        }

        /**
         * Moving from index 2 to index 0 in list [A, B, C].
         * After removal of C: [A, B]
         * After insertion at index 0: [C, A, B]
         */
        @Test
        void moveFromLastToFirst() {
            var solution = TestdataListSolution.generateInitializedSolution(3, 1);
            var entity = solution.getEntityList().get(0);
            var value1 = entity.getValueList().get(0);
            var value2 = entity.getValueList().get(1);
            var value3 = entity.getValueList().get(2);

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var changeMove = Moves.change(variableMetaModel, entity, 2, entity, 0);

            MoveTester.build(solutionMetaModel)
                    .using(solution)
                    .execute(changeMove);

            // After removal: [A, B], then insert at 0: [C, A, B]
            assertThat(entity.getValueList()).containsExactly(value3, value1, value2);
        }

        /**
         * Moving from index 1 to index 0 in list [A, B, C].
         * After removal of B: [A, C]
         * After insertion at index 0: [B, A, C]
         */
        @Test
        void moveMiddleToFirst() {
            var solution = TestdataListSolution.generateInitializedSolution(3, 1);
            var entity = solution.getEntityList().get(0);
            var value1 = entity.getValueList().get(0);
            var value2 = entity.getValueList().get(1);
            var value3 = entity.getValueList().get(2);

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var changeMove = Moves.change(variableMetaModel, entity, 1, entity, 0);

            MoveTester.build(solutionMetaModel)
                    .using(solution)
                    .execute(changeMove);

            // After removal: [A, C], then insert at 0: [B, A, C]
            assertThat(entity.getValueList()).containsExactly(value2, value1, value3);
        }

        /**
         * Moving from index 1 to index 2 in list [A, B, C].
         * After removal of B: [A, C]
         * After insertion at index 2: [A, C, B]
         */
        @Test
        void moveMiddleToLast() {
            var solution = TestdataListSolution.generateInitializedSolution(3, 1);
            var entity = solution.getEntityList().get(0);
            var value1 = entity.getValueList().get(0);
            var value2 = entity.getValueList().get(1);
            var value3 = entity.getValueList().get(2);

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var changeMove = Moves.change(variableMetaModel, entity, 1, entity, 2);

            MoveTester.build(solutionMetaModel)
                    .using(solution)
                    .execute(changeMove);

            // After removal: [A, C], then insert at 2: [A, C, B]
            assertThat(entity.getValueList()).containsExactly(value1, value3, value2);
        }

    }

    /**
     * Tests for moving a value between different entities' lists.
     * As per {@link MutableSolutionView#moveValueBetweenLists}:
     * "Moves a value from one entity's planning list variable to another."
     */
    @Nested
    class MoveBetweenEntities {

        @Test
        void moveFromFirstEntityToSecondAtBeginning() {
            var solution = TestdataListSolution.generateInitializedSolution(4, 2);
            var entity1 = solution.getEntityList().get(0);
            var entity2 = solution.getEntityList().get(1);

            var initialEntity1Size = entity1.getValueList().size();
            var initialEntity2Size = entity2.getValueList().size();
            var valueToMove = entity1.getValueList().get(0);
            var entity2FirstValue = entity2.getValueList().get(0);

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var changeMove = Moves.change(variableMetaModel, entity1, 0, entity2, 0);

            MoveTester.build(solutionMetaModel)
                    .using(solution)
                    .execute(changeMove);

            // Source entity loses one value
            assertThat(entity1.getValueList()).hasSize(initialEntity1Size - 1);
            // Destination entity gains one value at the beginning
            assertThat(entity2.getValueList()).hasSize(initialEntity2Size + 1);
            assertThat(entity2.getValueList().get(0)).isEqualTo(valueToMove);
            assertThat(entity2.getValueList().get(1)).isEqualTo(entity2FirstValue);
        }

        @Test
        void moveFromFirstEntityToSecondAtEnd() {
            var value1 = new TestdataListValue("A1");
            var value2 = new TestdataListValue("A2");
            var value3 = new TestdataListValue("B1");
            var value4 = new TestdataListValue("B2");
            var entity1 = new TestdataListEntity("Entity1", value1, value2);
            var entity2 = new TestdataListEntity("Entity2", value3, value4);
            var solution = new TestdataListSolution();
            solution.setEntityList(List.of(entity1, entity2));
            solution.setValueList(List.of(value1, value2, value3, value4));

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            // Move value1 from entity1[0] to entity2[2] (end of entity2)
            var changeMove = Moves.change(variableMetaModel, entity1, 0, entity2, 2);

            MoveTester.build(solutionMetaModel)
                    .using(solution)
                    .execute(changeMove);

            assertThat(entity1.getValueList()).containsExactly(value2);
            assertThat(entity2.getValueList()).containsExactly(value3, value4, value1);
        }

        @Test
        void moveFromFirstEntityToSecondInMiddle() {
            var value1 = new TestdataListValue("A1");
            var value2 = new TestdataListValue("B1");
            var value3 = new TestdataListValue("B2");
            var value4 = new TestdataListValue("B3");
            var entity1 = new TestdataListEntity("Entity1", value1);
            var entity2 = new TestdataListEntity("Entity2", value2, value3, value4);
            var solution = new TestdataListSolution();
            solution.setEntityList(List.of(entity1, entity2));
            solution.setValueList(List.of(value1, value2, value3, value4));

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            // Move value1 from entity1[0] to entity2[1] (middle of entity2)
            var changeMove = Moves.change(variableMetaModel, entity1, 0, entity2, 1);

            MoveTester.build(solutionMetaModel)
                    .using(solution)
                    .execute(changeMove);

            assertThat(entity1.getValueList()).isEmpty();
            assertThat(entity2.getValueList()).containsExactly(value2, value1, value3, value4);
        }

        @Test
        void moveToEmptyEntity() {
            var value1 = new TestdataListValue("A1");
            var value2 = new TestdataListValue("A2");
            var entity1 = new TestdataListEntity("Entity1", value1, value2);
            var entity2 = new TestdataListEntity("Entity2");
            var solution = new TestdataListSolution();
            solution.setEntityList(List.of(entity1, entity2));
            solution.setValueList(List.of(value1, value2));

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            // Move value1 from entity1[0] to empty entity2[0]
            var changeMove = Moves.change(variableMetaModel, entity1, 0, entity2, 0);

            MoveTester.build(solutionMetaModel)
                    .using(solution)
                    .execute(changeMove);

            assertThat(entity1.getValueList()).containsExactly(value2);
            assertThat(entity2.getValueList()).containsExactly(value1);
        }

        @Test
        void moveLastValueLeavingEntityEmpty() {
            var value1 = new TestdataListValue("A1");
            var value2 = new TestdataListValue("B1");
            var entity1 = new TestdataListEntity("Entity1", value1);
            var entity2 = new TestdataListEntity("Entity2", value2);
            var solution = new TestdataListSolution();
            solution.setEntityList(List.of(entity1, entity2));
            solution.setValueList(List.of(value1, value2));

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            // Move value1 from entity1[0] to entity2[1] (end)
            var changeMove = Moves.change(variableMetaModel, entity1, 0, entity2, 1);

            MoveTester.build(solutionMetaModel)
                    .using(solution)
                    .execute(changeMove);

            assertThat(entity1.getValueList()).isEmpty();
            assertThat(entity2.getValueList()).containsExactly(value2, value1);
        }

    }

    /**
     * Tests for move properties: equals, hashCode, toString, getPlanningEntities, getPlanningValues.
     */
    @Nested
    class MoveProperties {

        @Test
        void getPlanningEntitiesForSameEntityMove() {
            var value1 = new TestdataListValue("A");
            var entity = new TestdataListEntity("Entity", value1);

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            // Move within same entity
            var changeMove =
                    (ListChangeMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) Moves.change(
                            variableMetaModel,
                            entity, 0, entity, 0);

            // Should return single entity
            assertThat(changeMove.getPlanningEntities()).containsExactly(entity);
        }

        @Test
        void getPlanningEntitiesForDifferentEntitiesMove() {
            var value1 = new TestdataListValue("A");
            var value2 = new TestdataListValue("B");
            var entity1 = new TestdataListEntity("Entity1", value1);
            var entity2 = new TestdataListEntity("Entity2", value2);

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var changeMove =
                    (ListChangeMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) Moves.change(
                            variableMetaModel,
                            entity1, 0, entity2, 0);

            // Should return both entities
            assertThat(changeMove.getPlanningEntities()).containsExactlyInAnyOrder(entity1, entity2);
        }

        @Test
        void getPlanningValuesReturnsMovedValue() {
            var value1 = new TestdataListValue("A");
            var value2 = new TestdataListValue("B");
            var entity = new TestdataListEntity("Entity", value1, value2);
            var solution = new TestdataListSolution();
            solution.setEntityList(List.of(entity));
            solution.setValueList(List.of(value1, value2));

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var changeMove =
                    (ListChangeMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) Moves.change(
                            variableMetaModel,
                            entity, 0, entity, 1);

            // getPlanningValues requires the move to be executed first to know the value
            MoveTester.build(solutionMetaModel)
                    .using(solution)
                    .execute(changeMove);

            assertThat(changeMove.getPlanningValues()).containsExactly(value1);
        }

        @Test
        void equalsAndHashCode() {
            var value1 = new TestdataListValue("A");
            var value2 = new TestdataListValue("B");
            var entity1 = new TestdataListEntity("Entity1", value1);
            var entity2 = new TestdataListEntity("Entity2", value2);

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var move1 = Moves.change(variableMetaModel, entity1, 0, entity2, 0);
            var move2 = Moves.change(variableMetaModel, entity1, 0, entity2, 0);
            var move3 = Moves.change(variableMetaModel, entity1, 0, entity2, 1);
            var move4 = Moves.change(variableMetaModel, entity2, 0, entity1, 0);

            // Same parameters should be equal
            assertThat(move1).isEqualTo(move2);
            assertThat(move1.hashCode()).isEqualTo(move2.hashCode());

            // Different destination index should not be equal
            assertThat(move1).isNotEqualTo(move3);

            // Reversed entities should not be equal
            assertThat(move1).isNotEqualTo(move4);

            // Should not equal null or other types
            assertThat(move1).isNotEqualTo(null);
            assertThat(move1).isNotEqualTo("not a move");
        }

        @Test
        void toStringContainsMoveDetails() {
            var value1 = new TestdataListValue("A");
            var value2 = new TestdataListValue("B");
            var entity1 = new TestdataListEntity("Entity1", value1);
            var entity2 = new TestdataListEntity("Entity2", value2);
            var solution = new TestdataListSolution();
            solution.setEntityList(List.of(entity1, entity2));
            solution.setValueList(List.of(value1, value2));

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var changeMove = Moves.change(variableMetaModel, entity1, 0, entity2, 1);

            // Execute to populate the planning value
            MoveTester.build(solutionMetaModel)
                    .using(solution)
                    .execute(changeMove);

            var toString = changeMove.toString();
            assertThat(toString).contains("Entity1");
            assertThat(toString).contains("Entity2");
            assertThat(toString).contains("0");
            assertThat(toString).contains("1");
        }

        @Test
        void getSourceAndDestinationProperties() {
            var value1 = new TestdataListValue("A");
            var value2 = new TestdataListValue("B");
            var entity1 = new TestdataListEntity("Entity1", value1);
            var entity2 = new TestdataListEntity("Entity2", value2);

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var changeMove =
                    (ListChangeMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) Moves.change(
                            variableMetaModel,
                            entity1, 0, entity2, 1);

            assertThat(changeMove.getSourceEntity()).isEqualTo(entity1);
            assertThat(changeMove.getSourceIndex()).isEqualTo(0);
            assertThat(changeMove.getDestinationEntity()).isEqualTo(entity2);
            assertThat(changeMove.getDestinationIndex()).isEqualTo(1);
        }
    }

    /**
     * Tests for the rebase functionality.
     */
    @Nested
    class Rebase {

        @Test
        void rebaseCreatesNewMoveWithRebasedEntities() {
            var value1 = new TestdataListValue("A");
            var value2 = new TestdataListValue("B");
            var entity1 = new TestdataListEntity("Entity1", value1);
            var entity2 = new TestdataListEntity("Entity2", value2);

            // Create clones that represent rebased entities
            var rebasedEntity1 = new TestdataListEntity("Entity1", value1);
            var rebasedEntity2 = new TestdataListEntity("Entity2", value2);

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var originalMove =
                    (ListChangeMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) Moves.change(
                            variableMetaModel,
                            entity1, 0, entity2, 1);

            // Look up new entities
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

            // Verify the rebased move has the new entities
            assertThat(rebasedMove.getSourceEntity()).isEqualTo(rebasedEntity1);
            assertThat(rebasedMove.getDestinationEntity()).isEqualTo(rebasedEntity2);
            // Indices should remain the same
            assertThat(rebasedMove.getSourceIndex()).isEqualTo(0);
            assertThat(rebasedMove.getDestinationIndex()).isEqualTo(1);
        }

        @Test
        void rebaseWithSameEntityForSourceAndDestination() {
            var value1 = new TestdataListValue("A");
            var entity = new TestdataListEntity("Entity", value1);
            var rebasedEntity = new TestdataListEntity("Entity", value1);

            var solutionMetaModel = TestdataListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                    .listVariable("valueList", TestdataListValue.class);

            var originalMove =
                    (ListChangeMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) Moves.change(
                            variableMetaModel,
                            entity, 0, entity, 0);

            var rebasedMove = originalMove.rebase(new Lookup() {
                @Override
                @SuppressWarnings("unchecked")
                public <T> T lookUpWorkingObject(T object) {
                    if (object == entity) {
                        return (T) rebasedEntity;
                    }
                    return object;
                }

            });

            // Both source and destination should be rebased to the same entity
            assertThat(rebasedMove.getSourceEntity()).isEqualTo(rebasedEntity);
            assertThat(rebasedMove.getDestinationEntity()).isEqualTo(rebasedEntity);
        }
    }

}
