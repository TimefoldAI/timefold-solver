package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.solvingStarted;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getPinnedListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockDestinationSelector;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockEntityIndependentValueSelector;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockNeverEndingDestinationSelector;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockNeverEndingEntityIndependentValueSelector;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockPinnedNeverEndingDestinationSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertAllCodesOfMoveSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertAllCodesOfMoveSelectorWithoutSize;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCodesOfNeverEndingMoveSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockScoreDirector;

import java.util.List;

import ai.timefold.solver.core.impl.heuristic.selector.list.ElementRef;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListValue;

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

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());

        var moveSelector = new ListChangeMoveSelector<>(
                mockEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v3, v1, v2),
                mockDestinationSelector(
                        new ElementRef(a, 0),
                        new ElementRef(b, 0),
                        new ElementRef(c, 0),
                        new ElementRef(c, 1),
                        new ElementRef(a, 2),
                        new ElementRef(a, 1)),
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
                "3 {C[0]->C[0]}", // noop
                "3 {C[0]->C[1]}", // undoable
                "3 {C[0]->A[2]}",
                "3 {C[0]->A[1]}",
                // Moving 1 from A[1]
                "1 {A[1]->A[0]}",
                "1 {A[1]->B[0]}",
                "1 {A[1]->C[0]}",
                "1 {A[1]->C[1]}",
                "1 {A[1]->A[2]}", // undoable
                "1 {A[1]->A[1]}", // noop
                // Moving 2 from A[0]
                "2 {A[0]->A[0]}", // noop
                "2 {A[0]->B[0]}",
                "2 {A[0]->C[0]}",
                "2 {A[0]->C[1]}",
                "2 {A[0]->A[2]}", // undoable
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
                        new ElementRef(c, 0),
                        new ElementRef(c, 1),
                        new ElementRef(a, 2),
                        new ElementRef(a, 1)),
                false);

        solvingStarted(moveSelector, scoreDirector);

        // Value order: [3, 1, 2]
        // Entity order: [A, B, C]
        // Initial state:
        // - A [2, 1]
        // - B []
        // - C [3]

        // Not testing size; filtering selector doesn't and can't report correct size unless iterating over all values.
        assertAllCodesOfMoveSelectorWithoutSize(moveSelector,
                // Moving 3 from C[0]
                "3 {C[0]->C[0]}", // noop
                "3 {C[0]->C[1]}", // undoable
                "3 {C[0]->A[2]}",
                "3 {C[0]->A[1]}",
                // Moving 1 from A[1]
                "1 {A[1]->C[0]}",
                "1 {A[1]->C[1]}",
                "1 {A[1]->A[2]}", // undoable
                "1 {A[1]->A[1]}" // noop
        );
    }

    @Test
    void random() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var a = TestdataListEntity.createWithValues("A", v1, v2);
        var b = TestdataListEntity.createWithValues("B");
        var c = TestdataListEntity.createWithValues("C", v3);

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());

        var moveSelector = new ListChangeMoveSelector<>(
                mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v2, v1, v3, v3, v3),
                mockNeverEndingDestinationSelector(
                        new ElementRef(b, 0),
                        new ElementRef(a, 2),
                        new ElementRef(a, 0),
                        new ElementRef(a, 1),
                        new ElementRef(a, 2)),
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
                        new ElementRef(c, 0),
                        new ElementRef(a, 2),
                        new ElementRef(a, 1),
                        new ElementRef(c, 0)),
                true);

        solvingStarted(moveSelector, scoreDirector);

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
    void constructionHeuristic() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var v4 = new TestdataListValue("4");
        var v5 = new TestdataListValue("5");
        var a = new TestdataListEntity("A");
        var b = new TestdataListEntity("B");
        var c = TestdataListEntity.createWithValues("C", v5);

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());

        var moveSelector = new ListChangeMoveSelector<>(
                mockEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v3, v1, v4, v2, v5),
                mockDestinationSelector(
                        new ElementRef(a, 0),
                        new ElementRef(b, 0),
                        new ElementRef(c, 0),
                        new ElementRef(c, 1)),
                false);

        solvingStarted(moveSelector, scoreDirector);

        assertAllCodesOfMoveSelector(moveSelector,
                // Assigning 3
                "3 {null->A[0]}",
                "3 {null->B[0]}",
                "3 {null->C[0]}",
                "3 {null->C[1]}",
                // Assigning 1
                "1 {null->A[0]}",
                "1 {null->B[0]}",
                "1 {null->C[0]}",
                "1 {null->C[1]}",
                // Assigning 4
                "4 {null->A[0]}",
                "4 {null->B[0]}",
                "4 {null->C[0]}",
                "4 {null->C[1]}",
                // Assigning 2
                "2 {null->A[0]}",
                "2 {null->B[0]}",
                "2 {null->C[0]}",
                "2 {null->C[1]}",
                // 5 is already assigned, so ListChangeMoves are selected.
                "5 {C[0]->A[0]}",
                "5 {C[0]->B[0]}",
                "5 {C[0]->C[0]}",
                "5 {C[0]->C[1]}");
    }

    @Test
    void constructionHeuristicWithPinning() {
        var v1 = new TestdataPinnedWithIndexListValue("1");
        var v2 = new TestdataPinnedWithIndexListValue("2");
        var v3 = new TestdataPinnedWithIndexListValue("3");
        var v4 = new TestdataPinnedWithIndexListValue("4");
        var v5 = new TestdataPinnedWithIndexListValue("5");
        var a = new TestdataPinnedWithIndexListEntity("A");
        var b = new TestdataPinnedWithIndexListEntity("B");
        b.setPinned(true); // Ignore entirely.
        var c = TestdataPinnedWithIndexListEntity.createWithValues("C", v5);
        c.setPlanningPinToIndex(1); // Ignore v5.
        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3, v4, v5));

        var scoreDirector = mockScoreDirector(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var moveSelector = new ListChangeMoveSelector<>(
                mockEntityIndependentValueSelector(getPinnedListVariableDescriptor(scoreDirector), v3, v1, v4, v2, v5),
                mockDestinationSelector(
                        new ElementRef(a, 0),
                        new ElementRef(a, 1),
                        new ElementRef(c, 1),
                        new ElementRef(c, 2)),
                false);

        solvingStarted(moveSelector, scoreDirector);

        assertAllCodesOfMoveSelectorWithoutSize(moveSelector,
                // Assigning 3
                "3 {null->A[0]}",
                "3 {null->A[1]}",
                "3 {null->C[1]}",
                "3 {null->C[2]}",
                // Assigning 1
                "1 {null->A[0]}",
                "1 {null->A[1]}",
                "1 {null->C[1]}",
                "1 {null->C[2]}",
                // Assigning 4
                "4 {null->A[0]}",
                "4 {null->A[1]}",
                "4 {null->C[1]}",
                "4 {null->C[2]}",
                // Assigning 2
                "2 {null->A[0]}",
                "2 {null->A[1]}",
                "2 {null->C[1]}",
                "2 {null->C[2]}");
    }
}
