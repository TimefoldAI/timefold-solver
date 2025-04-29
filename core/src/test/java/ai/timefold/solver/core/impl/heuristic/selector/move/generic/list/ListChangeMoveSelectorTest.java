package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.phaseStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.solvingStarted;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getAllowsUnassignedvaluesListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.getPinnedListVariableDescriptor;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.mockAllowsUnassignedValuesNeverEndingDestinationSelector;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.mockDestinationSelector;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.mockEntityIndependentValueSelector;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.mockNeverEndingDestinationSelector;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.mockNeverEndingEntityIndependentValueSelector;
import static ai.timefold.solver.core.testdomain.list.TestdataListUtils.mockPinnedNeverEndingDestinationSelector;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfMoveSelector;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfMoveSelectorWithoutSize;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertCodesOfNeverEndingMoveSelector;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;

import java.util.List;

import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;

import org.junit.jupiter.api.Test;

class ListChangeMoveSelectorTest {

    @Test
    void original() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var a = TestdataListEntity.createWithValues("A", v2, v1);
        var b = TestdataListEntity.createWithValues("B");
        var c = TestdataListEntity.createWithValues("C", v3);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var moveSelector = new ListChangeMoveSelector<>(
                mockEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v3, v1, v2),
                mockDestinationSelector(
                        ElementPosition.of(a, 0),
                        ElementPosition.of(b, 0),
                        ElementPosition.of(c, 0),
                        ElementPosition.of(c, 1),
                        ElementPosition.of(a, 2),
                        ElementPosition.of(a, 1)),
                false);

        solvingStarted(moveSelector, scoreDirector);

        // Value order: [3, 1, 2]
        // Entity order: [A, B, C]
        // Initial state:
        // - A [2, 1]
        // - B []
        // - C [3]

        assertAllCodesOfMoveSelector(moveSelector,
                // Moving 3 from C[0]
                "3 {C[0]->A[0]}",
                "3 {C[0]->B[0]}",
                "3 {C[0]->C[0]}", // ephemeral
                "3 {C[0]->C[1]}", // ephemeral
                "3 {C[0]->A[2]}",
                "3 {C[0]->A[1]}",
                // Moving 1 from A[1]
                "1 {A[1]->A[0]}",
                "1 {A[1]->B[0]}",
                "1 {A[1]->C[0]}",
                "1 {A[1]->C[1]}",
                "1 {A[1]->A[2]}", // ephemeral
                "1 {A[1]->A[1]}", // ephemeral
                // Moving 2 from A[0]
                "2 {A[0]->A[0]}", // ephemeral
                "2 {A[0]->B[0]}",
                "2 {A[0]->C[0]}",
                "2 {A[0]->C[1]}",
                "2 {A[0]->A[2]}", // ephemeral
                "2 {A[0]->A[1]}");
    }

    @Test
    void originalWithPinning() {
        var v1 = new TestdataPinnedWithIndexListValue("1");
        var v2 = new TestdataPinnedWithIndexListValue("2");
        var v3 = new TestdataPinnedWithIndexListValue("3");
        var v4 = new TestdataPinnedWithIndexListValue("4");
        var a = TestdataPinnedWithIndexListEntity.createWithValues("A", v2, v1);
        a.setPlanningPinToIndex(1); // Ignore v2.
        var b = TestdataPinnedWithIndexListEntity.createWithValues("B", v4);
        b.setPinned(true); // Ignore entirely.
        var c = TestdataPinnedWithIndexListEntity.createWithValues("C", v3);
        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3, v4));

        var scoreDirector = mockScoreDirector(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var moveSelector = new ListChangeMoveSelector<>(
                mockEntityIndependentValueSelector(getPinnedListVariableDescriptor(scoreDirector), v4, v3, v1, v2),
                mockDestinationSelector(
                        ElementPosition.of(c, 0),
                        ElementPosition.of(c, 1),
                        ElementPosition.of(a, 2),
                        ElementPosition.of(a, 1)),
                false);

        var solverScope = solvingStarted(moveSelector, scoreDirector);
        phaseStarted(moveSelector, solverScope);

        // Value order: [3, 1, 2]
        // Entity order: [A, B, C]
        // Initial state:
        // - A [2, 1]
        // - B []
        // - C [3]

        // Not testing size; filtering selector doesn't and can't report correct size unless iterating over all values.
        assertAllCodesOfMoveSelectorWithoutSize(moveSelector,
                // Moving 3 from C[0]
                "3 {C[0]->C[0]}", // ephemeral
                "3 {C[0]->C[1]}", // ephemeral
                "3 {C[0]->A[2]}",
                "3 {C[0]->A[1]}",
                // Moving 1 from A[1]
                "1 {A[1]->C[0]}",
                "1 {A[1]->C[1]}",
                "1 {A[1]->A[2]}", // ephemeral
                "1 {A[1]->A[1]}" // ephemeral
        );
    }

    @Test
    void originalAllowsUnassignedValues() {
        var v1 = new TestdataAllowsUnassignedValuesListValue("1");
        var v2 = new TestdataAllowsUnassignedValuesListValue("2");
        var v3 = new TestdataAllowsUnassignedValuesListValue("3");
        var v4 = new TestdataAllowsUnassignedValuesListValue("4");
        var a = TestdataAllowsUnassignedValuesListEntity.createWithValues("A", v2, v1);
        var b = TestdataAllowsUnassignedValuesListEntity.createWithValues("B");
        var c = TestdataAllowsUnassignedValuesListEntity.createWithValues("C", v3);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3, v4));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var moveSelector = new ListChangeMoveSelector<>(
                mockEntityIndependentValueSelector(getAllowsUnassignedvaluesListVariableDescriptor(scoreDirector), v3, v1, v4,
                        v2),
                mockDestinationSelector(
                        ElementPosition.of(a, 0),
                        ElementPosition.of(b, 0),
                        ElementPosition.of(c, 0),
                        ElementPosition.of(c, 1),
                        ElementPosition.of(a, 2),
                        ElementPosition.of(a, 1),
                        ElementPosition.unassigned()),
                false);

        solvingStarted(moveSelector, scoreDirector);

        // First try all destinations for v3 (which is originally at C[0]),
        // then v1 (originally at A[1]),
        // then v4 (unassigned),
        // then v2 (originally at A[0]).
        assertAllCodesOfMoveSelector(moveSelector,
                "3 {C[0]->A[0]}",
                "3 {C[0]->B[0]}",
                "3 {C[0]->C[0]}", // ephemeral
                "3 {C[0]->C[1]}",
                "3 {C[0]->A[2]}",
                "3 {C[0]->A[1]}",
                "3 {C[0]->null}",
                "1 {A[1]->A[0]}",
                "1 {A[1]->B[0]}",
                "1 {A[1]->C[0]}",
                "1 {A[1]->C[1]}",
                "1 {A[1]->A[2]}",
                "1 {A[1]->A[1]}", // ephemeral
                "1 {A[1]->null}",
                "4 {null->A[0]}",
                "4 {null->B[0]}",
                "4 {null->C[0]}",
                "4 {null->C[1]}",
                "4 {null->A[2]}",
                "4 {null->A[1]}",
                "No change",
                "2 {A[0]->A[0]}", // ephemeral
                "2 {A[0]->B[0]}",
                "2 {A[0]->C[0]}",
                "2 {A[0]->C[1]}",
                "2 {A[0]->A[2]}",
                "2 {A[0]->A[1]}",
                "2 {A[0]->null}");
    }

    @Test
    void random() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var a = TestdataListEntity.createWithValues("A", v1, v2);
        var b = TestdataListEntity.createWithValues("B");
        var c = TestdataListEntity.createWithValues("C", v3);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var moveSelector = new ListChangeMoveSelector<>(
                mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v2, v1, v3, v3, v3),
                mockNeverEndingDestinationSelector(
                        ElementPosition.of(b, 0),
                        ElementPosition.of(a, 2),
                        ElementPosition.of(a, 0),
                        ElementPosition.of(a, 1),
                        ElementPosition.of(a, 2)),
                true);

        solvingStarted(moveSelector, scoreDirector);

        // Initial state:
        // - A [1, 2]
        // - B []
        // - C [3]

        // The moved values (2, 1, 3, 3, 3) are supplied by the source value selector and their source positions
        // are deduced using inverse relation and index supplies. The destinations (B[0], A[2], ...) are supplied
        // by the destination selector.
        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "2 {A[1]->B[0]}",
                "1 {A[0]->A[2]}",
                "3 {C[0]->A[0]}",
                "3 {C[0]->A[1]}",
                "3 {C[0]->A[2]}");
    }

    @Test
    void randomWithPinning() {
        var v1 = new TestdataPinnedWithIndexListValue("1");
        var v2 = new TestdataPinnedWithIndexListValue("2");
        var v3 = new TestdataPinnedWithIndexListValue("3");
        var v4 = new TestdataPinnedWithIndexListValue("4");
        var a = TestdataPinnedWithIndexListEntity.createWithValues("A", v1, v2);
        a.setPlanningPinToIndex(1); // Ignore v1.
        var b = TestdataPinnedWithIndexListEntity.createWithValues("B", v4);
        b.setPinned(true); // Ignore entirely.
        var c = TestdataPinnedWithIndexListEntity.createWithValues("C", v3);
        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3, v4));

        var scoreDirector = mockScoreDirector(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var moveSelector = new ListChangeMoveSelector<>(
                mockNeverEndingEntityIndependentValueSelector(getPinnedListVariableDescriptor(scoreDirector), v2, v1, v4, v3,
                        v3),
                mockPinnedNeverEndingDestinationSelector(
                        ElementPosition.of(c, 0),
                        ElementPosition.of(a, 2),
                        ElementPosition.of(a, 1),
                        ElementPosition.of(c, 0)),
                true);

        var solverScope = solvingStarted(moveSelector, scoreDirector);
        phaseStarted(moveSelector, solverScope);

        // Initial state:
        // - A [1, 2]
        // - B []
        // - C [3]

        // The moved values (2, 3, 2, 2) are supplied by the source value selector,
        // and their source positions are deduced using inverse relation and index supplies.
        // The destinations (A[1], A[2], ...) are supplied by the destination selector.
        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "2 {A[1]->C[0]}",
                "3 {C[0]->A[2]}",
                "3 {C[0]->A[1]}",
                "2 {A[1]->C[0]}");
    }

    @Test
    void randomAllowsUnassignedValues() {
        var v1 = new TestdataAllowsUnassignedValuesListValue("1");
        var v2 = new TestdataAllowsUnassignedValuesListValue("2");
        var v3 = new TestdataAllowsUnassignedValuesListValue("3");
        var v4 = new TestdataAllowsUnassignedValuesListValue("4");
        var a = TestdataAllowsUnassignedValuesListEntity.createWithValues("A", v1, v2);
        var b = TestdataAllowsUnassignedValuesListEntity.createWithValues("B");
        var c = TestdataAllowsUnassignedValuesListEntity.createWithValues("C", v3);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3, v4));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var moveSelector = new ListChangeMoveSelector<>(
                mockNeverEndingEntityIndependentValueSelector(getAllowsUnassignedvaluesListVariableDescriptor(scoreDirector),
                        v2, v1, v4, v3,
                        v3, v3),
                mockAllowsUnassignedValuesNeverEndingDestinationSelector(
                        ElementPosition.of(b, 0),
                        ElementPosition.of(a, 2),
                        ElementPosition.of(a, 0),
                        ElementPosition.of(a, 1),
                        ElementPosition.of(a, 2),
                        ElementPosition.unassigned()),
                true);

        solvingStarted(moveSelector, scoreDirector);

        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "2 {A[1]->B[0]}",
                "1 {A[0]->A[2]}",
                "4 {null->A[0]}",
                "3 {C[0]->A[1]}",
                "3 {C[0]->A[2]}");
    }

}
