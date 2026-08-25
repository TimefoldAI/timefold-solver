package ai.timefold.solver.core.preview.api.move.builtin;

import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.TestdataPinnedListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.TestdataPinnedListSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;

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

        // We have 4 values. One is unassigned, therefore isn't included in the swaps.
        // Three other values can be mutually swapped, each pair produced in both directions:
        // - assignedValue1 (e1@0) <-> assignedValue2 (e2@0)
        // - assignedValue1 (e1@0) <-> assignedValue3 (e2@1)
        // - assignedValue2 (e2@0) <-> assignedValue3 (e2@1), within e2
        NeighborhoodTester.build(new ListSwapMoveProvider<>(variableMetaModel), solutionMetaModel)
                .using(solution)
                .producesAllOf(
                        Moves.swap(variableMetaModel, e1, 0, e2, 0),
                        Moves.swap(variableMetaModel, e2, 0, e1, 0),
                        Moves.swap(variableMetaModel, e1, 0, e2, 1),
                        Moves.swap(variableMetaModel, e2, 1, e1, 0),
                        Moves.swap(variableMetaModel, e2, 0, e2, 1),
                        Moves.swap(variableMetaModel, e2, 1, e2, 0));
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
