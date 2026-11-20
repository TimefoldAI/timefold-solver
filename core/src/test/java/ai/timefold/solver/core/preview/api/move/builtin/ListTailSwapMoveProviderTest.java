package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.impl.move.builtin.SubListSwapMove;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Range;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.pinned.TestdataListPinnedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.pinned.TestdataListPinnedEntityProvidingSolution;
import ai.timefold.solver.core.testutil.MoveProviderAssertions;

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

    @Test
    void id() {
        var variableMetaModel = TestdataListSolution.buildMetaModel().genuineEntity(TestdataListEntity.class)
                .listVariable("valueList");
        assertThat(new ListTailSwapMoveProvider<>(variableMetaModel, false).getId())
                .isEqualTo("ListTailSwapMoveProvider(TestdataListEntity.valueList,noReversing)");
    }

    @Test
    void everyDrawnMoveKeepsTheSolutionValidWithEntityRange() {
        var solutionMetaModel = TestdataListPinnedEntityProvidingSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListPinnedEntityProvidingEntity.class).listVariable();
        var solution = tailSwapSolution();
        var context =
                NeighborhoodTester.build(new ListTailSwapMoveProvider<>(variableMetaModel), solutionMetaModel).using(solution);
        MoveProviderAssertions.assertDrawnListMovesKeepSolutionValid(context, variableMetaModel, solution.getEntityList(),
                TestdataListPinnedEntityProvidingEntity::isPinned, 2_000);
    }

    @Test
    void everyDrawnMoveKeepsTheSolutionValidWithSolutionRange() {
        var solutionMetaModel = TestdataPinnedWithIndexListSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedWithIndexListEntity.class).listVariable();
        var solution = MoveProviderAssertions.listSolutionRangeSolution();
        var context =
                NeighborhoodTester.build(new ListTailSwapMoveProvider<>(variableMetaModel), solutionMetaModel).using(solution);
        MoveProviderAssertions.assertDrawnListMovesKeepSolutionValid(context, variableMetaModel, solution.getEntityList(),
                TestdataPinnedWithIndexListEntity::isPinned, 2_000);
    }

    @Test
    void everyValidPartnerIsDrawnEquallyOftenWithEntityRange() {
        var solutionMetaModel = TestdataListPinnedEntityProvidingSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListPinnedEntityProvidingEntity.class).listVariable();
        var solution = tailSwapSolution();
        var movableEntities = solution.getEntityList().stream().filter(entity -> !entity.isPinned()).toList();
        // The left value with the most valid partners; a partner accepts the left tail, and the left entity its tail.
        List<Move<TestdataListPinnedEntityProvidingSolution>> expectedMoves = List.of();
        for (var left : movableEntities) {
            var leftList = left.getValueList();
            for (var leftIndex = left.getPinIndex(); leftIndex < leftList.size(); leftIndex++) {
                var leftTail = leftList.subList(leftIndex, leftList.size());
                var moves = new ArrayList<Move<TestdataListPinnedEntityProvidingSolution>>();
                for (var right : movableEntities) {
                    var rightList = right.getValueList();
                    for (var rightIndex = right.getPinIndex(); rightIndex < rightList.size(); rightIndex++) {
                        if (right != left && right.getValueRange().containsAll(leftTail)
                                && left.getValueRange().containsAll(rightList.subList(rightIndex, rightList.size()))) {
                            moves.add(Moves.swap(variableMetaModel, new Range<>(left, leftIndex, leftList.size()),
                                    new Range<>(right, rightIndex, rightList.size()), false));
                        }
                    }
                }
                if (moves.size() > expectedMoves.size()) {
                    expectedMoves = moves;
                }
            }
        }
        assertThat(expectedMoves).hasSizeGreaterThan(1);
        var context = NeighborhoodTester.build(new ListTailSwapMoveProvider<>(variableMetaModel, false), solutionMetaModel)
                .using(solution);
        var expected = expectedMoves;
        var moves = context.getMovesAsStream().limit(200_000).filter(expected::contains).toList();
        MoveProviderAssertions.assertEquallyOften(moves, move -> move, expected, 0.3);
    }

    /**
     * Partners of v3 lie in 3 entities with 2, 4 and 1 valid tails;
     * e3 and e5 accept only part of some tails, and v0 is pinned by index.
     */
    private static TestdataListPinnedEntityProvidingSolution tailSwapSolution() {
        var v = IntStream.range(0, 12).mapToObj(i -> new TestdataValue("v" + i)).toList();
        Function<int[], List<TestdataValue>> pick = indices -> Arrays.stream(indices).mapToObj(v::get).toList();
        var solution = new TestdataListPinnedEntityProvidingSolution();
        solution.setEntityList(List.of(
                tailSwapEntity("e0", pick.apply(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 9 }), pick.apply(new int[] { 0, 1, 2 }),
                        false, 1),
                tailSwapEntity("e1", pick.apply(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 9 }), pick.apply(new int[] { 3 }), false,
                        0),
                tailSwapEntity("e2", pick.apply(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }),
                        pick.apply(new int[] { 4, 5, 6, 7 }), false, 0),
                tailSwapEntity("e3", pick.apply(new int[] { 0, 1, 2, 3, 4, 5, 8, 9, 10 }), pick.apply(new int[] { 8, 9 }),
                        false, 0),
                tailSwapEntity("e4", pick.apply(new int[] { 11 }), pick.apply(new int[] { 11 }), true, 0),
                tailSwapEntity("e5", pick.apply(new int[] { 0, 1, 2, 3, 8, 10 }), pick.apply(new int[] { 10 }), false, 0)));
        return solution;
    }

    private static TestdataListPinnedEntityProvidingEntity tailSwapEntity(String code, List<TestdataValue> range,
            List<TestdataValue> list, boolean pinned, int pinIndex) {
        var entity = new TestdataListPinnedEntityProvidingEntity(code, range);
        entity.setValueList(new ArrayList<>(list));
        entity.setPinned(pinned);
        entity.setPinIndex(pinIndex);
        return entity;
    }

}
