package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.solvingStarted;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getPinnedListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockEntityIndependentValueSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertAllCodesOfMoveSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertAllCodesOfMoveSelectorWithoutSize;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCodesOfNeverEndingMoveSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockScoreDirector;

import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListValue;

import org.junit.jupiter.api.Test;

class ListSwapMoveSelectorTest {

    @Test
    void original() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        TestdataListEntity.createWithValues("A", v2, v1);
        TestdataListEntity.createWithValues("B");
        TestdataListEntity.createWithValues("C", v3);

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
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
                "3 {C[0]} <-> 3 {C[0]}", // undoable
                "3 {C[0]} <-> 1 {A[1]}",
                "3 {C[0]} <-> 2 {A[0]}",
                "1 {A[1]} <-> 3 {C[0]}", // redundant
                "1 {A[1]} <-> 1 {A[1]}", // undoable
                "1 {A[1]} <-> 2 {A[0]}",
                "2 {A[0]} <-> 3 {C[0]}", // redundant
                "2 {A[0]} <-> 1 {A[1]}", // redundant
                "2 {A[0]} <-> 2 {A[0]}" // undoable
        );
    }

    @Test
    void originalWithPinning() {
        var v1 = new TestdataPinnedWithIndexListValue("1");
        var v2 = new TestdataPinnedWithIndexListValue("2");
        var v3 = new TestdataPinnedWithIndexListValue("3");
        var a = TestdataPinnedWithIndexListEntity.createWithValues("A", v2, v1);
        a.setPlanningPinToIndex(1); // Ignore v2
        var b = TestdataPinnedWithIndexListEntity.createWithValues("B");
        b.setPinned(true); // Ignore entirely.
        TestdataPinnedWithIndexListEntity.createWithValues("C", v3);

        var scoreDirector = mockScoreDirector(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        var listVariableDescriptor = getPinnedListVariableDescriptor(scoreDirector);
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

        assertAllCodesOfMoveSelectorWithoutSize(moveSelector,
                "3 {C[0]} <-> 3 {C[0]}", // undoable
                "3 {C[0]} <-> 1 {A[1]}",
                "1 {A[1]} <-> 3 {C[0]}", // redundant
                "1 {A[1]} <-> 1 {A[1]}" // undoable
        );
    }

    @Test
    void random() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        TestdataListEntity.createWithValues("A", v1, v2);
        TestdataListEntity.createWithValues("B");
        TestdataListEntity.createWithValues("C", v3);

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
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
                "2 {A[1]} <-> 2 {A[1]}",
                "3 {C[0]} <-> 3 {C[0]}",
                "1 {A[0]} <-> 1 {A[0]}",
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
        TestdataPinnedWithIndexListEntity.createWithValues("C", v3);

        var scoreDirector = mockScoreDirector(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        var listVariableDescriptor = getPinnedListVariableDescriptor(scoreDirector);
        var moveSelector = new ListSwapMoveSelector<>(
                // Value selectors are longer than the number of expected codes because they're expected
                // to be never ending, so they must not be exhausted after the last asserted code.
                mockEntityIndependentValueSelector(listVariableDescriptor, v1, v2, v3, v1, v2, v3, v1, v2, v3),
                mockEntityIndependentValueSelector(listVariableDescriptor, v1, v3, v2, v1, v3, v2, v1, v3, v2),
                true);

        solvingStarted(moveSelector, scoreDirector);

        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "2 {A[1]} <-> 3 {C[0]}",
                "3 {C[0]} <-> 2 {A[1]}",
                "2 {A[1]} <-> 3 {C[0]}",
                "3 {C[0]} <-> 2 {A[1]}",
                "2 {A[1]} <-> 3 {C[0]}");
    }
}
