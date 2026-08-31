package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.SubListSampler;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class SubListChangeMoveProviderTest {

    @Test
    void drawnSpanLengthsRespectMinAndMax() {
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
                .build(new SubListChangeMoveProvider<>(variableMetaModel, 2, 5), solutionMetaModel)
                .using(solution);

        var moves = context
                .getMovesAsStream(
                        move -> (SubListChangeMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) move)
                .limit(300)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> assertThat(move.getSource().length()).isBetween(2, 5));
    }

    @Test
    void noArgConstructorNeverExceedsDefaultMaximumSubListSize() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var listSize = SubListSampler.DEFAULT_MAXIMUM_SUB_LIST_SIZE * 3;
        var values = new TestdataListValue[listSize];
        for (var i = 0; i < listSize; i++) {
            values[i] = new TestdataListValue("v" + i);
        }
        var entity = new TestdataListEntity("A", values);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(values));

        var context = NeighborhoodTester
                .build(new SubListChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);

        var moves = context
                .getMovesAsStream(
                        move -> (SubListChangeMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) move)
                .limit(300)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> assertThat(move.getSource().length())
                        .isLessThanOrEqualTo(SubListSampler.DEFAULT_MAXIMUM_SUB_LIST_SIZE));

        // The explicit constructor is untouched by the no-arg default: a larger maximum still applies.
        var contextWithLargerMax = NeighborhoodTester
                .build(new SubListChangeMoveProvider<>(variableMetaModel, 1, listSize), solutionMetaModel)
                .using(solution);
        var movesWithLargerMax = contextWithLargerMax
                .getMovesAsStream(
                        move -> (SubListChangeMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) move)
                .limit(300)
                .toList();
        assertThat(movesWithLargerMax)
                .anyMatch(move -> move.getSource().length() > SubListSampler.DEFAULT_MAXIMUM_SUB_LIST_SIZE);
    }

    @Test
    void drawnSpanNeverTouchesPinnedPrefix() {
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
                .build(new SubListChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);

        var moves = context
                .getMovesAsStream(
                        move -> (SubListChangeMove<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListEntity, TestdataPinnedWithIndexListValue>) move)
                .limit(300)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> {
                    assertThat(move.getSource().fromIndex()).isGreaterThanOrEqualTo(3);
                    assertThat(move.getDestination().index()).isGreaterThanOrEqualTo(3);
                });
    }

    @Test
    void fullyPinnedEntityNeverDrawnFromOrInto() {
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
                .build(new SubListChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);

        var moves = context
                .getMovesAsStream(
                        move -> (SubListChangeMove<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListEntity, TestdataPinnedWithIndexListValue>) move)
                .limit(300)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> {
                    assertThat(move.getSource().<TestdataPinnedWithIndexListEntity> entity()).isNotEqualTo(pinnedEntity);
                    assertThat(move.getDestination().<TestdataPinnedWithIndexListEntity> entity()).isNotEqualTo(pinnedEntity);
                });
    }

    @Test
    void differentDrawsProduceDifferentSpans() {
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
                .build(new SubListChangeMoveProvider<>(variableMetaModel, 1, 5), solutionMetaModel)
                .using(solution);

        var distinctSpans = context
                .getMovesAsStream(
                        move -> (SubListChangeMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) move)
                .limit(300)
                .map(move -> move.getSource().fromIndex() + ".." + move.getSource().toIndex())
                .collect(Collectors.toCollection(HashSet::new));
        assertThat(distinctSpans).hasSizeGreaterThan(1);
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
                .build(new SubListChangeMoveProvider<>(variableMetaModel, 2, 5, true, false), solutionMetaModel)
                .using(solution);
        var movesWithReversing = contextWithReversing
                .getMovesAsStream(
                        move -> (SubListChangeMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) move)
                .limit(300)
                .toList();
        assertThat(movesWithReversing).anyMatch(SubListChangeMove::isReversing);

        var contextWithoutReversing = NeighborhoodTester
                .build(new SubListChangeMoveProvider<>(variableMetaModel, 2, 5, false, false), solutionMetaModel)
                .using(solution);
        var movesWithoutReversing = contextWithoutReversing
                .getMovesAsStream(
                        move -> (SubListChangeMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) move)
                .limit(300)
                .toList();
        assertThat(movesWithoutReversing).noneMatch(SubListChangeMove::isReversing);
    }

    @Test
    void reversingNeverHappensForSingleElementSpan() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);

        var values = new TestdataListValue[6];
        for (var i = 0; i < 6; i++) {
            values[i] = new TestdataListValue("v" + i);
        }
        var entity = new TestdataListEntity("A", values);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(values));

        var context = NeighborhoodTester
                .build(new SubListChangeMoveProvider<>(variableMetaModel, 1, 1, true, false), solutionMetaModel)
                .using(solution);
        var moves = context
                .getMovesAsStream(
                        move -> (SubListChangeMove<TestdataListSolution, TestdataListEntity, TestdataListValue>) move)
                .limit(300)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .noneMatch(SubListChangeMove::isReversing);
    }

    @Test
    void crossingNullDefaultTrueAlsoUnassignsSpan() {
        var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                .listVariable("valueList", TestdataAllowsUnassignedValuesListValue.class);

        var values = new TestdataAllowsUnassignedValuesListValue[8];
        for (var i = 0; i < 8; i++) {
            values[i] = new TestdataAllowsUnassignedValuesListValue("v" + i);
        }
        var entity = new TestdataAllowsUnassignedValuesListEntity("A", values);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(values));

        var context = NeighborhoodTester
                .build(new SubListChangeMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        var moves = context.getMovesAsStream().limit(1000).toList();
        assertThat(moves).anyMatch(move -> move instanceof SubListUnassignMove);
    }

    @Test
    void crossingNullFalseNeverUnassignsSpan() {
        var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                .listVariable("valueList", TestdataAllowsUnassignedValuesListValue.class);

        var values = new TestdataAllowsUnassignedValuesListValue[8];
        for (var i = 0; i < 8; i++) {
            values[i] = new TestdataAllowsUnassignedValuesListValue("v" + i);
        }
        var entity = new TestdataAllowsUnassignedValuesListEntity("A", values);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(values));

        var context = NeighborhoodTester
                .build(new SubListChangeMoveProvider<>(variableMetaModel, 1, Integer.MAX_VALUE, true, false),
                        solutionMetaModel)
                .using(solution);
        var moves = context.getMovesAsStream().limit(500).toList();
        assertThat(moves)
                .isNotEmpty()
                .noneMatch(move -> move instanceof SubListUnassignMove);
    }

    @Test
    void constructorRejectsExplicitCrossingNullOnVariableWithoutUnassignedValues() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SubListChangeMoveProvider<>(variableMetaModel, 1, 5, true, true));
    }

    @Test
    void constructorRejectsInvalidSizes() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SubListChangeMoveProvider<>(variableMetaModel, 0, 5));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SubListChangeMoveProvider<>(variableMetaModel, 5, 2));
    }

}
