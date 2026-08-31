package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.pinned.unassignedvar.TestdataPinnedAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.unassignedvar.TestdataPinnedAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.unassignedvar.TestdataPinnedAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.pinned.TestdataPinnedUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.pinned.TestdataPinnedUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.pinned.TestdataPinnedUnassignedValuesListValue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class MassListUnassignMoveProviderTest {

    @Test
    void constructorRejectsNonUnassignedVariable() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable("valueList", TestdataListValue.class);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MassListUnassignMoveProvider<>(variableMetaModel, Samplers.exactly(2)));
    }

    @Test
    void everyMoveHasANullDestination() {
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
                .build(new MassListUnassignMoveProvider<>(variableMetaModel, Samplers.exactly(3)), solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream(
                move -> (MassListChangeMove<TestdataAllowsUnassignedValuesListSolution, TestdataAllowsUnassignedValuesListEntity, TestdataAllowsUnassignedValuesListValue>) move)
                .limit(50)
                .toList();
        assertThat(moves).isNotEmpty();
        assertThat(moves).allSatisfy(move -> assertThat(move.getDestination()).isNull());
    }

    @Test
    void sizeOneSampleStillYieldsAMoveAndTheProviderIsNotCutOffEarly() {
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

        // Samplers.exactly(1) stops right at the seed, so every sample this sampler produces is size-1;
        // the provider must not treat that as a rejection,
        // or the iterator would spin (or run dry) instead of steadily producing moves.
        var context = NeighborhoodTester
                .build(new MassListUnassignMoveProvider<>(variableMetaModel, Samplers.exactly(1)), solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream().limit(50).toList();
        assertThat(moves).hasSize(50);
    }

    @Test
    void pinnedValueNeverJoinsASample() {
        var solutionMetaModel = TestdataPinnedUnassignedValuesListSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedUnassignedValuesListEntity.class)
                .listVariable("valueList", TestdataPinnedUnassignedValuesListValue.class);

        var pinned1 = new TestdataPinnedUnassignedValuesListValue("pinned1");
        var pinned2 = new TestdataPinnedUnassignedValuesListValue("pinned2");
        var free1 = new TestdataPinnedUnassignedValuesListValue("free1");
        var free2 = new TestdataPinnedUnassignedValuesListValue("free2");
        var entity = new TestdataPinnedUnassignedValuesListEntity("A", pinned1, pinned2, free1, free2);
        entity.setPlanningPinToIndex(2);
        var solution = new TestdataPinnedUnassignedValuesListSolution();
        solution.setEntityList(List.of(entity));
        solution.setValueList(List.of(pinned1, pinned2, free1, free2));

        var context = NeighborhoodTester
                .build(new MassListUnassignMoveProvider<>(variableMetaModel, Samplers.exactly(2)), solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream(
                move -> (MassListChangeMove<TestdataPinnedUnassignedValuesListSolution, TestdataPinnedUnassignedValuesListEntity, TestdataPinnedUnassignedValuesListValue>) move)
                .limit(50)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> assertThat(move.getSample()).doesNotContain(pinned1, pinned2));
    }

    @Test
    void fullyPinnedEntityContributesNoMembers() {
        var solutionMetaModel = TestdataPinnedAllowsUnassignedValuesListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedAllowsUnassignedValuesListEntity.class)
                .listVariable("valueList", TestdataPinnedAllowsUnassignedValuesListValue.class);

        var pinnedValues = List.of(new TestdataPinnedAllowsUnassignedValuesListValue("p0"),
                new TestdataPinnedAllowsUnassignedValuesListValue("p1"));
        var pinnedEntity = new TestdataPinnedAllowsUnassignedValuesListEntity("pinned", pinnedValues);
        pinnedEntity.setPinned(true);

        var freeValues = List.of(new TestdataPinnedAllowsUnassignedValuesListValue("f0"),
                new TestdataPinnedAllowsUnassignedValuesListValue("f1"),
                new TestdataPinnedAllowsUnassignedValuesListValue("f2"));
        var freeEntity = new TestdataPinnedAllowsUnassignedValuesListEntity("free", freeValues);

        var solution = new TestdataPinnedAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(pinnedEntity, freeEntity));
        var allValues = new ArrayList<TestdataPinnedAllowsUnassignedValuesListValue>();
        allValues.addAll(pinnedValues);
        allValues.addAll(freeValues);
        solution.setValueList(allValues);

        var context = NeighborhoodTester
                .build(new MassListUnassignMoveProvider<>(variableMetaModel, Samplers.exactly(2)), solutionMetaModel)
                .using(solution);

        var moves = context.getMovesAsStream(
                move -> (MassListChangeMove<TestdataPinnedAllowsUnassignedValuesListSolution, TestdataPinnedAllowsUnassignedValuesListEntity, TestdataPinnedAllowsUnassignedValuesListValue>) move)
                .limit(50)
                .toList();
        assertThat(moves)
                .isNotEmpty()
                .allSatisfy(move -> assertThat(move.getSample()).doesNotContainAnyElementsOf(pinnedValues));
    }

}
