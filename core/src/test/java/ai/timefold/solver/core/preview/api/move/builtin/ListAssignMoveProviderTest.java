package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.unassignedvar.TestdataPinnedAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.unassignedvar.TestdataPinnedAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.TestdataListUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.TestdataListUnassignedEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.pinned.TestdataListUnassignedPinnedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.pinned.TestdataListUnassignedPinnedEntityProvidingSolution;
import ai.timefold.solver.core.testutil.MoveProviderAssertions;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class ListAssignMoveProviderTest {

    @Test
    void constructorRejectsNonUnassignedVariable() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ListAssignMoveProvider<>(variableMetaModel));
    }

    @Test
    void pinnedEntitySkipped() {
        var solutionMetaModel = TestdataPinnedAllowsUnassignedValuesListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedAllowsUnassignedValuesListEntity.class)
                .listVariable();

        var solution = TestdataPinnedAllowsUnassignedValuesListSolution.generateUninitializedSolution(2, 2);
        var firstEntity = solution.getEntityList().get(0);
        var secondEntity = solution.getEntityList().get(1);
        firstEntity.setPinned(true);
        // firstValue and secondValue are both unassigned.
        var firstValue = solution.getValueList().get(0);
        var secondValue = solution.getValueList().get(1);

        // firstEntity is pinned -> no values can be assigned to it.
        // Both values can only be assigned to secondEntity.
        var context = NeighborhoodTester.build(new ListAssignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(
                Moves.assign(variableMetaModel, firstValue, secondEntity, 0),
                Moves.assign(variableMetaModel, secondValue, secondEntity, 0));
        context.producesNoneOf(
                Moves.assign(variableMetaModel, firstValue, firstEntity, 0),
                Moves.assign(variableMetaModel, secondValue, firstEntity, 0));
    }

    @Test
    void fromEntity() {
        var solutionMetaModel = TestdataListUnassignedEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListUnassignedEntityProvidingEntity.class)
                .listVariable();

        // e1: range={v1,v2}, empty; e2: range={v1,v3}, empty.
        var solution = TestdataListUnassignedEntityProvidingSolution.generateSolution();
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        // getValueList() returns distinct values from entity ranges: {v1, v2, v3}.
        var v2 = solution.getValueList().get(1);
        var v3 = solution.getValueList().get(2);
        e2.getValueList().add(solution.getValueList().get(0));

        // v2 unassigned, only in e1's range -> e1@0 -> 1 move.
        // v3 unassigned, only in e2's range -> e2@0, e2@1 (before/after v1) -> 2 moves.
        var context = NeighborhoodTester.build(new ListAssignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(
                Moves.assign(variableMetaModel, v2, e1, 0),
                Moves.assign(variableMetaModel, v3, e2, 0),
                Moves.assign(variableMetaModel, v3, e2, 1));
        context.producesNoneOf(
                Moves.assign(variableMetaModel, v2, e2, 0), // v2 not in e2's range.
                Moves.assign(variableMetaModel, v3, e1, 0)); // v3 not in e1's range.
    }

    @Test
    void fromSolution() {
        var solutionMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataAllowsUnassignedValuesListEntity.class)
                .listVariable();

        // e1 is empty; e2 has v1 assigned; v2 is unassigned.
        var solution = TestdataAllowsUnassignedValuesListSolution.generateUninitializedSolution(2, 2);
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        var v1 = solution.getValueList().get(0);
        var v2 = solution.getValueList().get(1);
        e2.getValueList().add(v1);
        SolutionManager.updateShadowVariables(solution);

        // v1 is assigned -> not picked. v2 is unassigned -> picked.
        // Destinations for v2: e1@0, e2@0, e2@1 -> 3 moves.
        NeighborhoodTester.build(new ListAssignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .producesAllOf(
                        Moves.assign(variableMetaModel, v2, e1, 0),
                        Moves.assign(variableMetaModel, v2, e2, 0),
                        Moves.assign(variableMetaModel, v2, e2, 1));
    }

    @Test
    void id() {
        var variableMetaModel = TestdataAllowsUnassignedValuesListSolution.buildMetaModel()
                .genuineEntity(TestdataAllowsUnassignedValuesListEntity.class).listVariable("valueList");
        assertThat(new ListAssignMoveProvider<>(variableMetaModel).getId())
                .isEqualTo("ListAssignMoveProvider(TestdataAllowsUnassignedValuesListEntity.valueList)");
    }

    @Test
    void everyDrawnMoveKeepsTheSolutionValidWithEntityRangeAndUnassignedValues() {
        var solutionMetaModel = TestdataListUnassignedPinnedEntityProvidingSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel =
                solutionMetaModel.genuineEntity(TestdataListUnassignedPinnedEntityProvidingEntity.class).listVariable();
        var solution = MoveProviderAssertions.listUnassignedPinnedSolution(MoveProviderAssertions.listValues());
        var context =
                NeighborhoodTester.build(new ListAssignMoveProvider<>(variableMetaModel), solutionMetaModel).using(solution);
        MoveProviderAssertions.assertDrawnListMovesKeepSolutionValid(context, variableMetaModel, solution.getEntityList(),
                TestdataListUnassignedPinnedEntityProvidingEntity::isPinned, 2_000);
    }

    @Test
    void everyValidDestinationIsDrawnEquallyOften() {
        var solutionMetaModel = TestdataListUnassignedPinnedEntityProvidingSolution.buildSolutionDescriptor().getMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListUnassignedPinnedEntityProvidingEntity.class)
                .listVariable();
        var values = MoveProviderAssertions.listValues();
        var solution = MoveProviderAssertions.listUnassignedPinnedSolution(values);
        var e1 = solution.getEntityList().get(1);
        var e2 = solution.getEntityList().get(2);
        var v14 = values.get(14);
        // e1 is pinned up to index 2: index 2 is the first valid destination there.
        var expectedMoves = List.of(Moves.assign(variableMetaModel, v14, e1, 2), Moves.assign(variableMetaModel, v14, e1, 3),
                Moves.assign(variableMetaModel, v14, e2, 0), Moves.assign(variableMetaModel, v14, e2, 1),
                Moves.assign(variableMetaModel, v14, e2, 2), Moves.assign(variableMetaModel, v14, e2, 3));
        var context = NeighborhoodTester.build(new ListAssignMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        var moves = context.getMovesAsStream().limit(40_000).filter(move -> move.getPlanningValues().contains(v14)).toList();
        MoveProviderAssertions.assertEquallyOften(moves, move -> move, expectedMoves, 0.3);
    }

}
