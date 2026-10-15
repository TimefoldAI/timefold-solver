package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import ai.timefold.solver.core.impl.move.builtin.SubListSwapMove;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListValue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class SubListSwapMoveProviderTest {

    @Test
    void swappedSpansNeverOverlapOnSameEntity() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var values = new TestdataListValue[10];
        for (var i = 0; i < 10; i++) {
            values[i] = new TestdataListValue("v" + i);
        }
        var entity = new TestdataListEntity("A", values);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(values));

        var context = NeighborhoodTester
                .build(new SubListSwapMoveProvider<>(variableMetaModel, 1, 4), solutionMetaModel)
                .using(solution);

        var moves = context
                .getMovesAsStream(
                        move -> (SubListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) move)
                .limit(300)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> {
                    var left = move.getLeftRange();
                    var right = move.getRightRange();
                    if (left.<TestdataListEntity> entity() == right.<TestdataListEntity> entity()) {
                        assertThat(left.toIndex() <= right.fromIndex() || right.toIndex() <= left.fromIndex()).isTrue();
                    }
                });
    }

    @Test
    void eachSideRespectsItsOwnSizeBounds() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var valuesA = new TestdataListValue[12];
        for (var i = 0; i < 12; i++) {
            valuesA[i] = new TestdataListValue("a" + i);
        }
        var entityA = new TestdataListEntity("A", valuesA);
        var valuesB = new TestdataListValue[12];
        for (var i = 0; i < 12; i++) {
            valuesB[i] = new TestdataListValue("b" + i);
        }
        var entityB = new TestdataListEntity("B", valuesB);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(entityA, entityB));
        var allValues = new ArrayList<TestdataListValue>();
        allValues.addAll(List.of(valuesA));
        allValues.addAll(List.of(valuesB));
        solution.setValueList(allValues);

        // One side is always drawn at length 1, the other always in [3, 5] -
        // but SubListSwapMove's constructor normalizes same-entity pairs
        // so that left precedes right, which can swap which getter reports which drawn side.
        // Assert on the unordered pair of lengths instead.
        var context = NeighborhoodTester
                .build(new SubListSwapMoveProvider<>(variableMetaModel, 1, 1, 3, 5), solutionMetaModel)
                .using(solution);

        var moves = context
                .getMovesAsStream(
                        move -> (SubListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) move)
                .limit(300)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> {
                    var lengths = List.of(move.getLeftRange().length(), move.getRightRange().length());
                    assertThat(lengths).anyMatch(length -> length == 1);
                    assertThat(lengths).anyMatch(length -> length >= 3 && length <= 5);
                });
    }

    @Test
    void differentDrawsProduceDifferentSwaps() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var values = new TestdataListValue[10];
        for (var i = 0; i < 10; i++) {
            values[i] = new TestdataListValue("v" + i);
        }
        var entity = new TestdataListEntity("A", values);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(values));

        var context = NeighborhoodTester
                .build(new SubListSwapMoveProvider<>(variableMetaModel, 1, 4), solutionMetaModel)
                .using(solution);

        var distinctSwaps = context
                .getMovesAsStream(
                        move -> (SubListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) move)
                .limit(300)
                .map(move -> move.getLeftRange() + "<->" + move.getRightRange())
                .collect(Collectors.toCollection(HashSet::new));
        assertThat(distinctSwaps).hasSizeGreaterThan(1);
    }

    @Test
    void reversingAppearsByDefaultAndCanBeDisabled() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var values = new TestdataListValue[10];
        for (var i = 0; i < 10; i++) {
            values[i] = new TestdataListValue("v" + i);
        }
        var entity = new TestdataListEntity("A", values);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(values));

        var contextWithReversing = NeighborhoodTester
                .build(new SubListSwapMoveProvider<>(variableMetaModel, 2, 4, 2, 4, true), solutionMetaModel)
                .using(solution);
        var movesWithReversing = contextWithReversing
                .getMovesAsStream(
                        move -> (SubListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) move)
                .limit(300)
                .toList();
        assertThat(movesWithReversing).anyMatch(SubListSwapMove::isReversing);

        var contextWithoutReversing = NeighborhoodTester
                .build(new SubListSwapMoveProvider<>(variableMetaModel, 2, 4, 2, 4, false), solutionMetaModel)
                .using(solution);
        var movesWithoutReversing = contextWithoutReversing
                .getMovesAsStream(
                        move -> (SubListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) move)
                .limit(300)
                .toList();
        assertThat(movesWithoutReversing).noneMatch(SubListSwapMove::isReversing);
    }

    @Test
    void reversingNeverHappensWhenBothSpansAreSingleElement() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var values = new TestdataListValue[8];
        for (var i = 0; i < 8; i++) {
            values[i] = new TestdataListValue("v" + i);
        }
        var entity = new TestdataListEntity("A", values);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(values));

        var context = NeighborhoodTester
                .build(new SubListSwapMoveProvider<>(variableMetaModel, 1, 1, 1, 1, true), solutionMetaModel)
                .using(solution);
        var moves = context
                .getMovesAsStream(
                        move -> (SubListSwapMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) move)
                .limit(300)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .noneMatch(SubListSwapMove::isReversing);
    }

    @Test
    void drawnSpansNeverTouchPinnedPrefix() {
        var solutionMetaModel = TestdataPinnedWithIndexListSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedWithIndexListEntity.class)
                .listVariable("valueList", TestdataPinnedWithIndexListValue.class);

        var values = new TestdataPinnedWithIndexListValue[9];
        for (var i = 0; i < 9; i++) {
            values[i] = new TestdataPinnedWithIndexListValue("v" + i);
        }
        var entity = new TestdataPinnedWithIndexListEntity("A", values);
        entity.setPinIndex(4);
        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(values));

        var context = NeighborhoodTester
                .build(new SubListSwapMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        var moves = context
                .getMovesAsStream(
                        move -> (SubListSwapMove<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListEntity, TestdataPinnedWithIndexListValue>) move)
                .limit(300)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> {
                    assertThat(move.getLeftRange().fromIndex()).isGreaterThanOrEqualTo(4);
                    assertThat(move.getRightRange().fromIndex()).isGreaterThanOrEqualTo(4);
                });
    }

    @Test
    void constructorRejectsInvalidSizes() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SubListSwapMoveProvider<>(variableMetaModel, 0, 5));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SubListSwapMoveProvider<>(variableMetaModel, 5, 2));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SubListSwapMoveProvider<>(variableMetaModel, 1, 5, 0, 5));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SubListSwapMoveProvider<>(variableMetaModel, 1, 5, 5, 2));
    }

}
