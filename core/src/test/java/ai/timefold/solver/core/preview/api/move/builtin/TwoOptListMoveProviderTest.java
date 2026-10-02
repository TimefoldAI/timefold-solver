package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.impl.move.builtin.SubListChangeMove;
import ai.timefold.solver.core.impl.move.builtin.SubListSwapMove;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Range;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListValue;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class TwoOptListMoveProviderTest {

    @Test
    void sameEntityAlwaysProducesReversal() {
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
                .build(new TwoOptListMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);

        var moves = context
                .getMovesAsStream(
                        move -> (SubListChangeMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) move)
                .limit(300)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> {
                    assertThat(move.getDestination().index()).isEqualTo(move.getSource().fromIndex());
                    assertThat(move.isReversing()).isTrue();
                    assertThat(move.getSource().length()).isGreaterThanOrEqualTo(2);
                });
    }

    @Test
    void crossingEntityFalseNeverProducesTailSwap() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var solution = twoEntitySolution();

        var context = NeighborhoodTester
                .build(new TwoOptListMoveProvider<>(variableMetaModel, false), solutionMetaModel)
                .using(solution);
        var moves = context.getMovesAsStream().limit(500).toList();
        assertThat(moves)
                .isNotEmpty()
                .noneMatch(move -> move instanceof SubListSwapMove);
    }

    @Test
    void crossingEntityTrueProducesNonReversingTailSwapToo() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var solution = twoEntitySolution();

        var context = NeighborhoodTester
                .build(new TwoOptListMoveProvider<>(variableMetaModel, true), solutionMetaModel)
                .using(solution);
        var moves = context.getMovesAsStream()
                .filter(SubListSwapMove.class::isInstance)
                .map(SubListSwapMove.class::cast)
                .limit(300)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .noneMatch(SubListSwapMove::isReversing);
    }

    private static TestdataListSolution twoEntitySolution() {
        var valuesA = new TestdataListValue[5];
        for (var i = 0; i < 5; i++) {
            valuesA[i] = new TestdataListValue("a" + i);
        }
        var entityA = new TestdataListEntity("A", valuesA);
        var valuesB = new TestdataListValue[5];
        for (var i = 0; i < 5; i++) {
            valuesB[i] = new TestdataListValue("b" + i);
        }
        var entityB = new TestdataListEntity("B", valuesB);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(entityA, entityB));
        var allValues = new ArrayList<TestdataListValue>();
        allValues.addAll(List.of(valuesA));
        allValues.addAll(List.of(valuesB));
        solution.setValueList(allValues);
        return solution;
    }

    @Test
    void pinnedPrefixNeverTouched() {
        var solutionMetaModel = TestdataPinnedWithIndexListSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedWithIndexListEntity.class)
                .listVariable("valueList", TestdataPinnedWithIndexListValue.class);

        var values = new TestdataPinnedWithIndexListValue[8];
        for (var i = 0; i < 8; i++) {
            values[i] = new TestdataPinnedWithIndexListValue("v" + i);
        }
        var entity = new TestdataPinnedWithIndexListEntity("A", values);
        entity.setPinIndex(3);
        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(values));

        var context = NeighborhoodTester
                .build(new TwoOptListMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        var moves = context
                .getMovesAsStream(
                        move -> (SubListChangeMove<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListEntity, TestdataPinnedWithIndexListValue>) move)
                .limit(300)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> assertThat(move.getSource().fromIndex()).isGreaterThanOrEqualTo(3));
    }

    @Test
    void fullyPinnedEntityNeverInvolved() {
        var solutionMetaModel = TestdataPinnedWithIndexListSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedWithIndexListEntity.class)
                .listVariable("valueList", TestdataPinnedWithIndexListValue.class);

        var pinnedValues = new TestdataPinnedWithIndexListValue[3];
        for (var i = 0; i < 3; i++) {
            pinnedValues[i] = new TestdataPinnedWithIndexListValue("p" + i);
        }
        var pinnedEntity = new TestdataPinnedWithIndexListEntity("pinned", pinnedValues);
        pinnedEntity.setPinned(true);

        var freeValues = new TestdataPinnedWithIndexListValue[5];
        for (var i = 0; i < 5; i++) {
            freeValues[i] = new TestdataPinnedWithIndexListValue("f" + i);
        }
        var freeEntity = new TestdataPinnedWithIndexListEntity("free", freeValues);

        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(List.of(pinnedEntity, freeEntity));
        var allValues = new ArrayList<TestdataPinnedWithIndexListValue>();
        allValues.addAll(List.of(pinnedValues));
        allValues.addAll(List.of(freeValues));
        solution.setValueList(allValues);

        var context = NeighborhoodTester
                .build(new TwoOptListMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        var moves = context.getMovesAsStream().limit(300).toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> {
                    if (move instanceof SubListChangeMove<?, ?, ?> reversal) {
                        assertThat(reversal.getSource().entity())
                                .isNotEqualTo(pinnedEntity);
                    } else if (move instanceof SubListSwapMove<?, ?, ?> swap) {
                        assertThat(swap.getLeftRange().entity()).isNotEqualTo(pinnedEntity);
                        assertThat(swap.getRightRange().entity())
                                .isNotEqualTo(pinnedEntity);
                    }
                });
    }

    @Test
    void valueRangeOnEntityRejectsTailSwap() {
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
                .build(new TwoOptListMoveProvider<>(variableMetaModel, true), solutionMetaModel)
                .using(solution);
        context.producesNoneOf(
                Moves.swap(variableMetaModel, new Range<>(e1, 0, 1), new Range<>(e2, 0, 2), false),
                Moves.swap(variableMetaModel, new Range<>(e2, 0, 2), new Range<>(e1, 0, 1), false));
    }

    @Test
    void executingReversalPinsTheIndexMath() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var v0 = new TestdataListValue("v0");
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");
        var v4 = new TestdataListValue("v4");
        var entity = new TestdataListEntity("A", v0, v1, v2, v3, v4);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(v0, v1, v2, v3, v4));

        var move = Moves.reverse(variableMetaModel, new Range<>(entity, 1, 4));
        MoveTester.build(solutionMetaModel)
                .using(solution)
                .execute(move);

        assertThat(entity.getValueList()).containsExactly(v0, v3, v2, v1, v4);
    }

}
