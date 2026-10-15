package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.TestdataPinnedListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.TestdataPinnedListSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.TestdataListUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.TestdataListUnassignedEntityProvidingSolution;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class ListSwapMoveProviderTest {

    @Test
    void pinnedEntitySkipped() {
        var solutionMetaModel = TestdataPinnedListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataPinnedListEntity.class)
                .listVariable();

        var solution = TestdataPinnedListSolution.generateInitializedSolution(2, 2);
        var firstEntity = solution.getEntityList().getFirst();
        var secondEntity = solution.getEntityList().get(1);
        firstEntity.setPinned(true);

        // firstEntity is pinned -> no swaps involving its values.
        // secondEntity's only potential swap partner is the pinned firstEntity -> 0 moves.
        NeighborhoodTester.build(new ListSwapMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .producesNoneOf(
                        Moves.swap(variableMetaModel, secondEntity, 0, firstEntity, 0),
                        Moves.swap(variableMetaModel, firstEntity, 0, secondEntity, 0));
    }

    @Test
    void fromSolution() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable();

        var solution = TestdataListSolution.generateUninitializedSolution(4, 2);
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        var assignedValue1 = solution.getValueList().get(1);
        var assignedValue2 = solution.getValueList().get(2);
        var assignedValue3 = solution.getValueList().get(3);
        e1.getValueList().add(assignedValue1);
        e2.getValueList().add(assignedValue2);
        e2.getValueList().add(assignedValue3);
        SolutionManager.updateShadowVariables(solution);

        var unassignedValue = solution.getValueList().getFirst();

        // Three assigned values can be mutually swapped, each pair produced in both directions:
        // - assignedValue1 (e1@0) <-> assignedValue2 (e2@0)
        // - assignedValue1 (e1@0) <-> assignedValue3 (e2@1)
        // - assignedValue2 (e2@0) <-> assignedValue3 (e2@1), within e2
        // The fourth value is unassigned; this domain disallows unassigned values,
        // so it must never enter a swap at all - not even as a composite unassign-then-assign.
        var context = NeighborhoodTester.build(new ListSwapMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(
                Moves.swap(variableMetaModel, e1, 0, e2, 0),
                Moves.swap(variableMetaModel, e2, 0, e1, 0),
                Moves.swap(variableMetaModel, e1, 0, e2, 1),
                Moves.swap(variableMetaModel, e2, 1, e1, 0),
                Moves.swap(variableMetaModel, e2, 0, e2, 1),
                Moves.swap(variableMetaModel, e2, 1, e2, 0));
        context.producesNoneOf(
                Moves.compose(
                        Moves.unassign(variableMetaModel, e1, 0),
                        Moves.assign(variableMetaModel, unassignedValue, e1, 0)),
                Moves.compose(
                        Moves.unassign(variableMetaModel, e2, 0),
                        Moves.assign(variableMetaModel, unassignedValue, e2, 0)),
                Moves.compose(
                        Moves.unassign(variableMetaModel, e2, 1),
                        Moves.assign(variableMetaModel, unassignedValue, e2, 1)));
    }

    @Test
    void crossingNullProducesCompositeWhenValueInRange() {
        var solutionMetaModel = TestdataListUnassignedEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListUnassignedEntityProvidingEntity.class)
                .listVariable();
        var solution = TestdataListUnassignedEntityProvidingSolution.generateSolution(); // e1:[v1,v2], e2:[v1,v3]
        var e2 = solution.getEntityList().get(1);
        var v1 = e2.getValueRange().get(0); // Shared with e1; will be assigned to e2.
        var v3 = e2.getValueRange().get(1); // In e2's own range; stays unassigned.
        e2.getValueList().add(v1);
        SolutionManager.updateShadowVariables(solution);

        // v1 is assigned at e2@0. v3 is unassigned but within e2's own range, so it may take v1's place.
        NeighborhoodTester.build(new ListSwapMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .producesAllOf(Moves.compose(
                        Moves.unassign(variableMetaModel, e2, 0),
                        Moves.assign(variableMetaModel, v3, e2, 0)));
    }

    @Test
    void crossingNullSkipsWhenValueOutOfRange() {
        var solutionMetaModel = TestdataListUnassignedEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListUnassignedEntityProvidingEntity.class)
                .listVariable();
        var solution = TestdataListUnassignedEntityProvidingSolution.generateSolution(); // e1:[v1,v2], e2:[v1,v3]
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        var v1 = e2.getValueRange().getFirst();
        var v2 = e1.getValueRange().get(1); // In e1's range only, not e2's.
        e2.getValueList().add(v1);
        SolutionManager.updateShadowVariables(solution);

        // v2 is unassigned, but out of range for e2 - no composite is produced for it.
        NeighborhoodTester.build(new ListSwapMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .producesNoneOf(Moves.compose(
                        Moves.unassign(variableMetaModel, e2, 0),
                        Moves.assign(variableMetaModel, v2, e2, 0)));
    }

    @Test
    void bothUnassignedProducesNoMoves() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .listVariable();
        var solution = TestdataListSolution.generateUninitializedSolution(2, 2); // Nothing assigned.

        // Every value is unassigned: a swap needs at least one assigned side.
        var context = NeighborhoodTester.build(new ListSwapMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution);
        assertThat(context.getMovesAsStream()).isEmpty();
    }

    @Test
    void fromEntity() {
        var solutionMetaModel = TestdataListEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntityProvidingEntity.class)
                .listVariable();

        var solution = TestdataListEntityProvidingSolution.generateSolution();
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        e1.getValueList().clear();
        var initiallyAssignedValue = e2.getValueRange().getFirst();
        e2.getValueList().add(initiallyAssignedValue);
        SolutionManager.updateShadowVariables(solution);

        // Only one value is assigned in total (in e2); a swap needs two assigned positions.
        NeighborhoodTester.build(new ListSwapMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .producesNoneOf(Moves.swap(variableMetaModel, e2, 0, e2, 0));
    }

}
