package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.pinned.TestdataPinnedUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.pinned.TestdataPinnedUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.pinned.TestdataPinnedUnassignedValuesListValue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class SubListUnassignMoveProviderTest {

    @Test
    void unassignsSpansOfBoundedLength() {
        var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                .listVariable("valueList", TestdataAllowsUnassignedValuesListValue.class);

        var values = new TestdataAllowsUnassignedValuesListValue[10];
        for (var i = 0; i < 10; i++) {
            values[i] = new TestdataAllowsUnassignedValuesListValue("v" + i);
        }
        var entity = new TestdataAllowsUnassignedValuesListEntity("A", values);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(values));

        var context = NeighborhoodTester
                .build(new SubListUnassignMoveProvider<>(variableMetaModel, 2, 5), solutionMetaModel)
                .using(solution);

        var moves = context
                .getMovesAsStream(
                        move -> (SubListUnassignMove<TestdataAllowsUnassignedValuesListSolution, TestdataAllowsUnassignedValuesListEntity, TestdataAllowsUnassignedValuesListValue>) move)
                .limit(300)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> assertThat(move.getRange().length()).isBetween(2, 5));
    }

    @Test
    void differentDrawsProduceDifferentSpans() {
        var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                .listVariable("valueList", TestdataAllowsUnassignedValuesListValue.class);

        var values = new TestdataAllowsUnassignedValuesListValue[10];
        for (var i = 0; i < 10; i++) {
            values[i] = new TestdataAllowsUnassignedValuesListValue("v" + i);
        }
        var entity = new TestdataAllowsUnassignedValuesListEntity("A", values);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(values));

        var context = NeighborhoodTester
                .build(new SubListUnassignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);

        var distinctSpans = context
                .getMovesAsStream(
                        move -> (SubListUnassignMove<TestdataAllowsUnassignedValuesListSolution, TestdataAllowsUnassignedValuesListEntity, TestdataAllowsUnassignedValuesListValue>) move)
                .limit(300)
                .map(move -> move.getRange().fromIndex() + ".." + move.getRange().toIndex())
                .collect(Collectors.toCollection(HashSet::new));
        assertThat(distinctSpans).hasSizeGreaterThan(1);
    }

    @Test
    void drawnSpanNeverTouchesPinnedPrefix() {
        var solutionMetaModel = TestdataPinnedUnassignedValuesListSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedUnassignedValuesListEntity.class)
                .listVariable("valueList", TestdataPinnedUnassignedValuesListValue.class);

        var values = new TestdataPinnedUnassignedValuesListValue[8];
        for (var i = 0; i < 8; i++) {
            values[i] = new TestdataPinnedUnassignedValuesListValue("v" + i);
        }
        var entity = new TestdataPinnedUnassignedValuesListEntity("A", values);
        entity.setPlanningPinToIndex(3);
        var solution = new TestdataPinnedUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(values));

        var context = NeighborhoodTester
                .build(new SubListUnassignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);

        var moves = context
                .getMovesAsStream(
                        move -> (SubListUnassignMove<TestdataPinnedUnassignedValuesListSolution, TestdataPinnedUnassignedValuesListEntity, TestdataPinnedUnassignedValuesListValue>) move)
                .limit(300)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> assertThat(move.getRange().fromIndex()).isGreaterThanOrEqualTo(3));
    }

    @Test
    void constructorRequiresAllowsUnassignedValues() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SubListUnassignMoveProvider<>(variableMetaModel));
    }

    @Test
    void constructorRejectsInvalidSizes() {
        var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                .listVariable("valueList", TestdataAllowsUnassignedValuesListValue.class);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SubListUnassignMoveProvider<>(variableMetaModel, 0, 5));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SubListUnassignMoveProvider<>(variableMetaModel, 5, 2));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SubListUnassignMoveProvider<>(variableMetaModel, 1, 5));
    }

}
