package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Range;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class ListTailSwapMoveProviderTest {

    @Test
    void producesTailSwapsAcrossEntities() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var valuesA = new TestdataListValue[] { new TestdataListValue("a0"), new TestdataListValue("a1"),
                new TestdataListValue("a2") };
        var entityA = new TestdataListEntity("A", valuesA);
        var valuesB = new TestdataListValue[] { new TestdataListValue("b0"), new TestdataListValue("b1") };
        var entityB = new TestdataListEntity("B", valuesB);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(entityA, entityB));
        var allValues = new ArrayList<TestdataListValue>();
        allValues.addAll(List.of(valuesA));
        allValues.addAll(List.of(valuesB));
        solution.setValueList(allValues);

        var context = NeighborhoodTester
                .build(new ListTailSwapMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(
                Moves.swap(variableMetaModel, new Range<>(entityA, 0, 3), new Range<>(entityB, 0, 2), false),
                Moves.swap(variableMetaModel, new Range<>(entityA, 2, 3), new Range<>(entityB, 1, 2), false));
    }

    @Test
    void reversingAppearsByDefaultAndCanBeDisabled() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var valuesA = new TestdataListValue[] { new TestdataListValue("a0"), new TestdataListValue("a1"),
                new TestdataListValue("a2") };
        var entityA = new TestdataListEntity("A", valuesA);
        var valuesB = new TestdataListValue[] { new TestdataListValue("b0"), new TestdataListValue("b1"),
                new TestdataListValue("b2") };
        var entityB = new TestdataListEntity("B", valuesB);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(entityA, entityB));
        var allValues = new ArrayList<TestdataListValue>();
        allValues.addAll(List.of(valuesA));
        allValues.addAll(List.of(valuesB));
        solution.setValueList(allValues);

        var contextWithReversing = NeighborhoodTester
                .build(new ListTailSwapMoveProvider<>(variableMetaModel, true), solutionMetaModel)
                .using(solution);
        var movesWithReversing = contextWithReversing
                .getMovesAsStream(
                        move -> (SubListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) move)
                .limit(300)
                .toList();
        assertThat(movesWithReversing).anyMatch(SubListSwapMove::isReversing);

        var contextWithoutReversing = NeighborhoodTester
                .build(new ListTailSwapMoveProvider<>(variableMetaModel, false), solutionMetaModel)
                .using(solution);
        var movesWithoutReversing = contextWithoutReversing
                .getMovesAsStream(
                        move -> (SubListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) move)
                .limit(300)
                .toList();
        assertThat(movesWithoutReversing).isNotEmpty();
        assertThat(movesWithoutReversing).noneMatch(SubListSwapMove::isReversing);
    }

    @Test
    void reversingNeverHappensWhenBothTailsAreSingleElement() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        // Each entity holds a single value, so its only possible tail has length 1.
        var entityA = new TestdataListEntity("A", new TestdataListValue("a0"));
        var entityB = new TestdataListEntity("B", new TestdataListValue("b0"));
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(entityA, entityB));
        solution.setValueList(List.of(entityA.getValueList().getFirst(), entityB.getValueList().getFirst()));

        var context = NeighborhoodTester
                .build(new ListTailSwapMoveProvider<>(variableMetaModel, true), solutionMetaModel)
                .using(solution);
        var moves = context
                .getMovesAsStream(
                        move -> (SubListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) move)
                .limit(300)
                .toList();
        assertThat(moves).isNotEmpty();
        assertThat(moves).noneMatch(SubListSwapMove::isReversing);
    }

    @Test
    void sameEntityNeverProducesAMove() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var values = new TestdataListValue[] { new TestdataListValue("v0"), new TestdataListValue("v1"),
                new TestdataListValue("v2") };
        var entity = new TestdataListEntity("A", values);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(values));

        var context = NeighborhoodTester
                .build(new ListTailSwapMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        assertThat(context.getMovesAsStream()).isEmpty();
    }

    @Test
    void valueRangeOnEntityRejectsInvalidSwap() {
        var solutionMetaModel = TestdataListEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntityProvidingEntity.class)
                .listVariable();

        // e1's range is [v1, v2], e2's range is [v1, v3]; v2 and v3 are each exclusive to one entity.
        var solution = TestdataListEntityProvidingSolution.generateSolution();
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        var v2 = e1.getValueRange().get(1);
        var v1 = e2.getValueRange().get(0);
        var v3 = e2.getValueRange().get(1);
        e1.getValueList().add(v2);
        e2.getValueList().add(v1);
        e2.getValueList().add(v3);
        SolutionManager.updateShadowVariables(solution);

        // Swapping the whole tails would move v2 into e2 (out of range) and v1,v3 into e1 (v3 out of range).
        var context = NeighborhoodTester
                .build(new ListTailSwapMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        context.producesNoneOf(
                Moves.swap(variableMetaModel, new Range<>(e1, 0, 1), new Range<>(e2, 0, 2), false),
                Moves.swap(variableMetaModel, new Range<>(e1, 0, 1), new Range<>(e2, 0, 2), true),
                Moves.swap(variableMetaModel, new Range<>(e2, 0, 2), new Range<>(e1, 0, 1), false),
                Moves.swap(variableMetaModel, new Range<>(e2, 0, 2), new Range<>(e1, 0, 1), true));
    }

}
