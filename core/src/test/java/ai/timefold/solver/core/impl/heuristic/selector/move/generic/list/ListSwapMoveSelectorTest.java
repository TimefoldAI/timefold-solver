package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.phaseStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.solvingStarted;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getAllowsUnassignedvaluesListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getPinnedListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockEntityIndependentValueSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertAllCodesOfMoveSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertAllCodesOfMoveSelectorWithoutSize;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCodesOfNeverEndingMoveSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockScoreDirector;

import java.util.List;

import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListValue;

import org.junit.jupiter.api.Test;

class ListSwapMoveSelectorTest {

    @Test
    void original() {
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");
        var e1 = TestdataListEntity.createWithValues("A", v2, v1);
        var e2 = TestdataListEntity.createWithValues("B");
        var e3 = TestdataListEntity.createWithValues("C", v3);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1, e2, e3));
        solution.setValueList(List.of(v1, v2, v3));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var listVariableDescriptor = getListVariableDescriptor(scoreDirector);
        var moveSelector = new ListSwapMoveSelector<>(
                mockEntityIndependentValueSelector(listVariableDescriptor, v3, v1, v2),
                mockEntityIndependentValueSelector(listVariableDescriptor, v3, v1, v2),
                false);

        solvingStarted(moveSelector, scoreDirector);

        // Value order: [3, 1, 2]
        // Entity order: [A, B, C]
        // Initial state:
        // - A [2, 1]
        // - B []
        // - C [3]

        assertAllCodesOfMoveSelector(moveSelector,
                "No change", // undoable
                "v3 {C[0]} <-> v1 {A[1]}",
                "v3 {C[0]} <-> v2 {A[0]}",
                "v1 {A[1]} <-> v3 {C[0]}", // redundant
                "No change", // undoable
                "v1 {A[1]} <-> v2 {A[0]}",
                "v2 {A[0]} <-> v3 {C[0]}", // redundant
                "v2 {A[0]} <-> v1 {A[1]}", // redundant
                "No change" // undoable
        );
    }

    @Test
    void originalWithPinning() {
        var v1 = new TestdataPinnedWithIndexListValue("v1");
        var v2 = new TestdataPinnedWithIndexListValue("v2");
        var v3 = new TestdataPinnedWithIndexListValue("v3");
        var a = TestdataPinnedWithIndexListEntity.createWithValues("A", v2, v1);
        a.setPlanningPinToIndex(1); // Ignore v2
        var b = TestdataPinnedWithIndexListEntity.createWithValues("B");
        b.setPinned(true); // Ignore entirely.
        var c = TestdataPinnedWithIndexListEntity.createWithValues("C", v3);
        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3));

        var scoreDirector = mockScoreDirector(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);
        var listVariableDescriptor = getPinnedListVariableDescriptor(scoreDirector);
        var moveSelector = new ListSwapMoveSelector<>(
                mockEntityIndependentValueSelector(listVariableDescriptor, v3, v1, v2),
                mockEntityIndependentValueSelector(listVariableDescriptor, v3, v1, v2),
                false);

        var solverScope = solvingStarted(moveSelector, scoreDirector);
        phaseStarted(moveSelector, solverScope);

        // Value order: [3, 1, 2]
        // Entity order: [A, B, C]
        // Initial state:
        // - A [2, 1]
        // - B []
        // - C [3]

        assertAllCodesOfMoveSelectorWithoutSize(moveSelector,
                "No change", // undoable
                "v3 {C[0]} <-> v1 {A[1]}",
                "v1 {A[1]} <-> v3 {C[0]}", // redundant
                "No change" // undoable
        );
    }

    @Test
    void originalAllowsUnassignedValues() {
        var v1 = new TestdataAllowsUnassignedValuesListValue("v1");
        var v2 = new TestdataAllowsUnassignedValuesListValue("v2");
        var v3 = new TestdataAllowsUnassignedValuesListValue("v3");
        var v4 = new TestdataAllowsUnassignedValuesListValue("v4");
        var a = TestdataAllowsUnassignedValuesListEntity.createWithValues("A", v2, v1);
        var b = TestdataAllowsUnassignedValuesListEntity.createWithValues("B", v3);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(List.of(v1, v2, v3, v4));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        // swap moves do not support uninitialized entities
        var listVariableDescriptor = getAllowsUnassignedvaluesListVariableDescriptor(scoreDirector);
        var moveSelector = new ListSwapMoveSelector<>(
                mockEntityIndependentValueSelector(listVariableDescriptor, v1, v2, v3, v4),
                mockEntityIndependentValueSelector(listVariableDescriptor, v4, v3, v2, v1),
                false);

        solvingStarted(moveSelector, scoreDirector);

        // Tests each move from the product of the two value selectors.
        assertAllCodesOfMoveSelectorWithoutSize(moveSelector,
                "v1 {A[1]->null}+v4 {null->A[1]}",
                "v1 {A[1]} <-> v3 {B[0]}",
                "v1 {A[1]} <-> v2 {A[0]}",
                "No change",
                "v2 {A[0]->null}+v4 {null->A[0]}",
                "v2 {A[0]} <-> v3 {B[0]}",
                "No change",
                "v2 {A[0]} <-> v1 {A[1]}",
                "v3 {B[0]->null}+v4 {null->B[0]}",
                "No change",
                "v3 {B[0]} <-> v2 {A[0]}",
                "v3 {B[0]} <-> v1 {A[1]}",
                "No change",
                "v3 {B[0]->null}+v4 {null->B[0]}",
                "v2 {A[0]->null}+v4 {null->A[0]}",
                "v1 {A[1]->null}+v4 {null->A[1]}");
    }

    @Test
    void random() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var e1 = TestdataListEntity.createWithValues("A", v1, v2);
        var e2 = TestdataListEntity.createWithValues("B");
        var e3 = TestdataListEntity.createWithValues("C", v3);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(e1, e2, e3));
        solution.setValueList(List.of(v1, v2, v3));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var listVariableDescriptor = getListVariableDescriptor(scoreDirector);
        var moveSelector = new ListSwapMoveSelector<>(
                // Value selectors are longer than the number of expected codes because they're expected
                // to be never ending, so they must not be exhausted after the last asserted code.
                mockEntityIndependentValueSelector(listVariableDescriptor, v2, v3, v2, v3, v2, v3, v1, v1, v1, v1),
                mockEntityIndependentValueSelector(listVariableDescriptor, v1, v2, v3, v1, v2, v3, v1, v2, v3, v1),
                true);

        solvingStarted(moveSelector, scoreDirector);

        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "2 {A[1]} <-> 1 {A[0]}",
                "3 {C[0]} <-> 2 {A[1]}",
                "2 {A[1]} <-> 3 {C[0]}",
                "3 {C[0]} <-> 1 {A[0]}",
                "No change",
                "No change",
                "No change",
                "1 {A[0]} <-> 2 {A[1]}",
                "1 {A[0]} <-> 3 {C[0]}");
    }

    @Test
    void randomWithPinning() {
        var v1 = new TestdataPinnedWithIndexListValue("1");
        var v2 = new TestdataPinnedWithIndexListValue("2");
        var v3 = new TestdataPinnedWithIndexListValue("3");
        var a = TestdataPinnedWithIndexListEntity.createWithValues("A", v1, v2);
        a.setPlanningPinToIndex(1); // Ignore v1
        var b = TestdataPinnedWithIndexListEntity.createWithValues("B");
        b.setPinned(true); // Ignore entirely.
        var c = TestdataPinnedWithIndexListEntity.createWithValues("C", v3);
        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3));

        var scoreDirector = mockScoreDirector(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);
        var listVariableDescriptor = getPinnedListVariableDescriptor(scoreDirector);
        var moveSelector = new ListSwapMoveSelector<>(
                // Value selectors are longer than the number of expected codes because they're expected
                // to be never ending, so they must not be exhausted after the last asserted code.
                mockEntityIndependentValueSelector(listVariableDescriptor, v1, v2, v3, v1, v2, v3, v1, v2, v3),
                mockEntityIndependentValueSelector(listVariableDescriptor, v1, v3, v2, v1, v3, v2, v1, v3, v2),
                true);

        var solverScope = solvingStarted(moveSelector, scoreDirector);
        phaseStarted(moveSelector, solverScope);

        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "2 {A[1]} <-> 3 {C[0]}",
                "3 {C[0]} <-> 2 {A[1]}",
                "2 {A[1]} <-> 3 {C[0]}",
                "3 {C[0]} <-> 2 {A[1]}",
                "2 {A[1]} <-> 3 {C[0]}");
    }

    @Test
    void randomAllowsUnassignedValues() {
        var v1 = new TestdataAllowsUnassignedValuesListValue("1");
        var v2 = new TestdataAllowsUnassignedValuesListValue("2");
        var v3 = new TestdataAllowsUnassignedValuesListValue("3");
        var v4 = new TestdataAllowsUnassignedValuesListValue("4");
        var e1 = TestdataAllowsUnassignedValuesListEntity.createWithValues("A", v2, v1);
        var e2 = TestdataAllowsUnassignedValuesListEntity.createWithValues("B", v3);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(e1, e2));
        solution.setValueList(List.of(v1, v2, v3, v4));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        // swap moves do not support uninitialized entities
        var listVariableDescriptor = getAllowsUnassignedvaluesListVariableDescriptor(scoreDirector);
        var moveSelector = new ListSwapMoveSelector<>(
                // Value selectors are longer than the number of expected codes because they're expected
                // to be never ending, so they must not be exhausted after the last asserted code.
                mockEntityIndependentValueSelector(listVariableDescriptor, v2, v3, v4, v2, v3, v4, v2, v3, v4, v1, v1, v1, v1),
                mockEntityIndependentValueSelector(listVariableDescriptor, v1, v2, v3, v4, v1, v2, v3, v4, v1, v2, v3, v1, v4),
                true);

        solvingStarted(moveSelector, scoreDirector);

        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "2 {A[0]} <-> 1 {A[1]}",
                "3 {B[0]} <-> 2 {A[0]}",
                "3 {B[0]->null}+4 {null->B[0]}",
                "2 {A[0]->null}+4 {null->A[0]}",
                "3 {B[0]} <-> 1 {A[1]}",
                "2 {A[0]->null}+4 {null->A[0]}",
                "2 {A[0]} <-> 3 {B[0]}",
                "3 {B[0]->null}+4 {null->B[0]}",
                "1 {A[1]->null}+4 {null->A[1]}");
    }
}
