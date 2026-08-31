package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Range;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SubListUnassignMoveTest {

    @Nested
    class Unassign {

        @Test
        void unassignsSpan() {
            var value1 = new TestdataAllowsUnassignedValuesListValue("A");
            var value2 = new TestdataAllowsUnassignedValuesListValue("B");
            var value3 = new TestdataAllowsUnassignedValuesListValue("C");
            var value4 = new TestdataAllowsUnassignedValuesListValue("D");
            var entity = new TestdataAllowsUnassignedValuesListEntity("Entity", value1, value2, value3, value4);
            var solution = new TestdataAllowsUnassignedValuesListSolution();
            solution.setEntityList(List.of(entity));
            solution.setValueList(List.of(value1, value2, value3, value4));

            var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                    .listVariable("valueList", TestdataAllowsUnassignedValuesListValue.class);

            var move = Moves.unassign(variableMetaModel, new Range<>(entity, 1, 3));

            MoveTester.build(solutionMetaModel)
                    .using(solution)
                    .execute(move);

            assertThat(entity.getValueList()).containsExactly(value1, value4);
        }

    }

    @Nested
    class TemporaryExecution {

        @Test
        void executeTemporarilyThenUndo() {
            var value1 = new TestdataAllowsUnassignedValuesListValue("A");
            var value2 = new TestdataAllowsUnassignedValuesListValue("B");
            var value3 = new TestdataAllowsUnassignedValuesListValue("C");
            var entity = new TestdataAllowsUnassignedValuesListEntity("Entity", value1, value2, value3);
            var solution = new TestdataAllowsUnassignedValuesListSolution();
            solution.setEntityList(List.of(entity));
            solution.setValueList(List.of(value1, value2, value3));

            var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                    .listVariable("valueList", TestdataAllowsUnassignedValuesListValue.class);

            var move = Moves.unassign(variableMetaModel, new Range<>(entity, 0, 2));

            MoveTester.build(solutionMetaModel)
                    .using(solution)
                    .executeTemporarily(move, view -> assertThat(entity.getValueList()).containsExactly(value3));

            assertThat(entity.getValueList()).containsExactly(value1, value2, value3);
        }

    }

    @Nested
    class MoveProperties {

        @Test
        void getPlanningEntitiesReturnsOwningEntity() {
            var entity =
                    new TestdataAllowsUnassignedValuesListEntity("Entity", new TestdataAllowsUnassignedValuesListValue("A"),
                            new TestdataAllowsUnassignedValuesListValue("B"));
            var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                    .listVariable("valueList", TestdataAllowsUnassignedValuesListValue.class);

            var move =
                    (SubListUnassignMove<TestdataAllowsUnassignedValuesListSolution, TestdataAllowsUnassignedValuesListEntity, TestdataAllowsUnassignedValuesListValue>) Moves
                            .unassign(variableMetaModel, new Range<>(entity, 0, 1));

            assertThat(move.getPlanningEntities()).containsExactly(entity);
        }

        @Test
        void getPlanningValuesReturnsUnassignedValues() {
            var value1 = new TestdataAllowsUnassignedValuesListValue("A");
            var value2 = new TestdataAllowsUnassignedValuesListValue("B");
            var entity = new TestdataAllowsUnassignedValuesListEntity("Entity", value1, value2);
            var solution = new TestdataAllowsUnassignedValuesListSolution();
            solution.setEntityList(List.of(entity));
            solution.setValueList(List.of(value1, value2));

            var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                    .listVariable("valueList", TestdataAllowsUnassignedValuesListValue.class);

            var move =
                    (SubListUnassignMove<TestdataAllowsUnassignedValuesListSolution, TestdataAllowsUnassignedValuesListEntity, TestdataAllowsUnassignedValuesListValue>) Moves
                            .unassign(variableMetaModel, new Range<>(entity, 0, 2));

            MoveTester.build(solutionMetaModel)
                    .using(solution)
                    .execute(move);

            assertThat(move.getPlanningValues()).containsExactly(value1, value2);
        }

        @Test
        void equalsAndHashCode() {
            var entity =
                    new TestdataAllowsUnassignedValuesListEntity("Entity", new TestdataAllowsUnassignedValuesListValue("A"),
                            new TestdataAllowsUnassignedValuesListValue("B"));
            var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                    .listVariable("valueList", TestdataAllowsUnassignedValuesListValue.class);

            var move1 = Moves.unassign(variableMetaModel, new Range<>(entity, 0, 1));
            var move2 = Moves.unassign(variableMetaModel, new Range<>(entity, 0, 1));
            var move3 = Moves.unassign(variableMetaModel, new Range<>(entity, 0, 2));

            assertThat(move1).isEqualTo(move2);
            assertThat(move1.hashCode()).isEqualTo(move2.hashCode());
            assertThat(move1).isNotEqualTo(move3);
            assertThat(move1).isNotEqualTo(null);
            assertThat(move1).isNotEqualTo("not a move");
        }

    }

    @Nested
    class Rebase {

        @Test
        void rebaseCreatesNewMoveWithRebasedEntity() {
            var entity =
                    new TestdataAllowsUnassignedValuesListEntity("Entity", new TestdataAllowsUnassignedValuesListValue("A"));
            var rebasedEntity =
                    new TestdataAllowsUnassignedValuesListEntity("Entity", new TestdataAllowsUnassignedValuesListValue("A"));

            var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
            var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                    .listVariable("valueList", TestdataAllowsUnassignedValuesListValue.class);

            var originalMove =
                    (SubListUnassignMove<TestdataAllowsUnassignedValuesListSolution, TestdataAllowsUnassignedValuesListEntity, TestdataAllowsUnassignedValuesListValue>) Moves
                            .unassign(variableMetaModel, new Range<>(entity, 0, 1));

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

            assertThat(rebasedMove.getRange().entity()).isEqualTo(rebasedEntity);
        }

    }

}
