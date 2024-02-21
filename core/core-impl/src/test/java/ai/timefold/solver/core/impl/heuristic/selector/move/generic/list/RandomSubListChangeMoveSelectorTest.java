package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.phaseStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.solvingStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.stepStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.list.TriangularNumbers.nthTriangle;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getAllowsUnassignedvaluesListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.listSize;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockEntitySelector;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockNeverEndingDestinationSelector;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockNeverEndingEntityIndependentValueSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCodesOfNeverEndingMoveSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertEmptyNeverEndingMoveSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.verifyPhaseLifecycle;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.impl.heuristic.selector.list.ElementLocation;
import ai.timefold.solver.core.impl.heuristic.selector.list.LocationInList;
import ai.timefold.solver.core.impl.heuristic.selector.list.RandomSubListSelector;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.impl.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class RandomSubListChangeMoveSelectorTest {

    @Test
    void randomUnrestricted() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var v4 = new TestdataListValue("4");
        var a = TestdataListEntity.createWithValues("A", v1, v2, v3, v4);
        var b = TestdataListEntity.createWithValues("B");
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(List.of(v1, v2, v3, v4));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var minimumSubListSize = 1;
        var maximumSubListSize = Integer.MAX_VALUE;
        var subListCount = 10;
        var destinationSize = 3; // arbitrary

        // The number of subLists of [1, 2, 3, 4] is the 4th triangular number (10).
        assertThat(subListCount).isEqualTo(nthTriangle(listSize(a)) + nthTriangle(listSize(b)));

        var moveSelector = new RandomSubListChangeMoveSelector<>(
                new RandomSubListSelector<>(
                        mockEntitySelector(a, b),
                        mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v1),
                        minimumSubListSize,
                        maximumSubListSize),
                mockNeverEndingDestinationSelector(destinationSize, new LocationInList(b, 0)),
                false);

        var random = new TestRandom(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, -1);

        solvingStarted(moveSelector, scoreDirector, random);

        // Every possible subList is selected.
        assertCodesOfNeverEndingMoveSelector(moveSelector, subListCount * destinationSize,
                "|4| {A[0..4]->B[0]}",
                "|3| {A[0..3]->B[0]}",
                "|3| {A[1..4]->B[0]}",
                "|2| {A[0..2]->B[0]}",
                "|2| {A[1..3]->B[0]}",
                "|2| {A[2..4]->B[0]}",
                "|1| {A[0..1]->B[0]}",
                "|1| {A[1..2]->B[0]}",
                "|1| {A[2..3]->B[0]}",
                "|1| {A[3..4]->B[0]}");

        random.assertIntBoundJustRequested(subListCount);
    }

    @Test
    void randomAllowsUnassignedValues() {
        var v1 = new TestdataAllowsUnassignedValuesListValue("1");
        var v2 = new TestdataAllowsUnassignedValuesListValue("2");
        var v3 = new TestdataAllowsUnassignedValuesListValue("3");
        var v4 = new TestdataAllowsUnassignedValuesListValue("4");
        var a = TestdataAllowsUnassignedValuesListEntity.createWithValues("A", v1, v2);
        var b = TestdataAllowsUnassignedValuesListEntity.createWithValues("B", v3);
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(List.of(v1, v2, v3, v4));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var minimumSubListSize = 1;
        var maximumSubListSize = Integer.MAX_VALUE;
        var subListCount = 4;
        var destinationSize = 6; // arbitrary

        // The number of subLists of [1, 2] is the 2nd triangular number (3).
        // The number of subLists of [3] is the 1st triangular number (1).
        assertThat(subListCount).isEqualTo(nthTriangle(listSize(a)) + nthTriangle(listSize(b)));

        var moveSelector = new RandomSubListChangeMoveSelector<>(
                new RandomSubListSelector<>(
                        mockEntitySelector(a, b),
                        mockNeverEndingEntityIndependentValueSelector(
                                getAllowsUnassignedvaluesListVariableDescriptor(scoreDirector),
                                v1, v1, v1, v1, v1, v1,
                                v3, v3, v3, v3, v3, v3,
                                v1, v1, v1, v1, v1, v1,
                                v1, v1, v1, v1, v1, v1,
                                v1, v1, v1, v1, v1, v1),
                        minimumSubListSize,
                        maximumSubListSize),
                mockNeverEndingDestinationSelector(destinationSize,
                        new LocationInList(a, 0),
                        new LocationInList(a, 1),
                        new LocationInList(a, 2),
                        new LocationInList(b, 0),
                        new LocationInList(b, 1),
                        ElementLocation.unassigned()),
                false);

        var random = new TestRandom(
                0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                1, 1, 1, 1, 1, 1,
                2, 2, 2, 2, 2, 2,
                0);

        solvingStarted(moveSelector, scoreDirector, random);

        // Every possible subList is selected.
        assertCodesOfNeverEndingMoveSelector(moveSelector, subListCount * destinationSize,
                "|2| {A[0..2]->A[0]}",
                "|2| {A[0..2]->A[1]}",
                "|2| {A[0..2]->A[2]}",
                "|2| {A[0..2]->B[0]}",
                "|2| {A[0..2]->B[1]}",
                "|2| {A[0..2]->null}",
                "|1| {B[0..1]->A[0]}",
                "|1| {B[0..1]->A[1]}",
                "|1| {B[0..1]->A[2]}",
                "|1| {B[0..1]->B[0]}",
                "|1| {B[0..1]->B[1]}",
                "|1| {B[0..1]->null}",
                "|1| {A[0..1]->A[0]}",
                "|1| {A[0..1]->A[1]}",
                "|1| {A[0..1]->A[2]}",
                "|1| {A[0..1]->B[0]}",
                "|1| {A[0..1]->B[1]}",
                "|1| {A[0..1]->null}",
                "|1| {A[1..2]->A[0]}",
                "|1| {A[1..2]->A[1]}",
                "|1| {A[1..2]->A[2]}",
                "|1| {A[1..2]->B[0]}",
                "|1| {A[1..2]->B[1]}",
                "|1| {A[1..2]->null}");

        random.assertIntBoundJustRequested(3);
    }

    @Test
    void randomReversing() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var v4 = new TestdataListValue("4");
        var a = TestdataListEntity.createWithValues("A", v1, v2, v3, v4);
        var b = TestdataListEntity.createWithValues("B");
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(List.of(v1, v2, v3, v4));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var minimumSubListSize = 1;
        var maximumSubListSize = Integer.MAX_VALUE;
        var subListCount = 10;
        var destinationSize = 13; // arbitrary
        // Selecting reversing moves doubles the total number of selected elements (move selector size).
        var moveSelectorSize = 2 * subListCount * destinationSize;

        var moveSelector = new RandomSubListChangeMoveSelector<>(
                new RandomSubListSelector<>(
                        mockEntitySelector(a, b),
                        mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v1),
                        minimumSubListSize,
                        maximumSubListSize),
                mockNeverEndingDestinationSelector(destinationSize, new LocationInList(b, 0)),
                true);

        // Each row is consumed by 1 createUpcomingSelection() call.
        // Columns are: subList index, reversing flag.
        var random = new TestRandom(
                0, 1, // reversing
                1, 0,
                2, 1, // reversing
                3, 1, // reversing
                4, 0,
                5, 1, // reversing
                6, 0,
                7, 1, // reversing
                8, 1, // reversing
                9, 0,
                -1, -1);

        solvingStarted(moveSelector, scoreDirector, random);

        // Every possible subList is selected; some moves are reversing.
        assertCodesOfNeverEndingMoveSelector(moveSelector, moveSelectorSize,
                "|4| {A[0..4]-reversing->B[0]}",
                "|3| {A[0..3]->B[0]}",
                "|3| {A[1..4]-reversing->B[0]}",
                "|2| {A[0..2]-reversing->B[0]}",
                "|2| {A[1..3]->B[0]}",
                "|2| {A[2..4]-reversing->B[0]}",
                "|1| {A[0..1]->B[0]}",
                "|1| {A[1..2]-reversing->B[0]}",
                "|1| {A[2..3]-reversing->B[0]}",
                "|1| {A[3..4]->B[0]}");
    }

    @Test
    void randomWithSubListSizeBounds() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var v4 = new TestdataListValue("4");
        var a = TestdataListEntity.createWithValues("A", v1, v2, v3, v4);
        var b = TestdataListEntity.createWithValues("B");
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(List.of(v1, v2, v3, v4));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var minimumSubListSize = 2;
        var maximumSubListSize = 3;
        var subListCount = 5;
        var destinationSize = 51; // arbitrary

        var moveSelector = new RandomSubListChangeMoveSelector<>(
                new RandomSubListSelector<>(
                        mockEntitySelector(a, b),
                        mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v1),
                        minimumSubListSize,
                        maximumSubListSize),
                mockNeverEndingDestinationSelector(destinationSize, new LocationInList(b, 0)),
                false);

        var random = new TestRandom(0, 1, 2, 3, 4, -1);

        solvingStarted(moveSelector, scoreDirector, random);

        // Only subLists bigger than 1 and smaller than 4 are selected.
        assertCodesOfNeverEndingMoveSelector(moveSelector, subListCount * destinationSize,
                "|3| {A[0..3]->B[0]}",
                "|3| {A[1..4]->B[0]}",
                "|2| {A[0..2]->B[0]}",
                "|2| {A[1..3]->B[0]}",
                "|2| {A[2..4]->B[0]}");

        random.assertIntBoundJustRequested(subListCount);
    }

    @Test
    void emptyWhenMinimumSubListSizeGreaterThanListSize() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var a = TestdataListEntity.createWithValues("A", v1, v2, v3);

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());

        var minimumSubListSize = 100;
        var maximumSubListSize = Integer.MAX_VALUE;

        var moveSelector = new RandomSubListChangeMoveSelector<>(
                new RandomSubListSelector<>(
                        mockEntitySelector(a),
                        mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v1),
                        minimumSubListSize,
                        maximumSubListSize),
                mockNeverEndingDestinationSelector(),
                false);

        solvingStarted(moveSelector, scoreDirector);

        assertEmptyNeverEndingMoveSelector(moveSelector);
    }

    @Test
    void skipSubListsSmallerThanMinimumSize() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var v4 = new TestdataListValue("4");
        var a = TestdataListEntity.createWithValues("A", v1, v2, v3);
        var b = TestdataListEntity.createWithValues("B");
        var c = TestdataListEntity.createWithValues("C", v4);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3, v4));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var minimumSubListSize = 2;
        var maximumSubListSize = 2;
        var subListCount = 2;
        var destinationSize = 13; // arbitrary

        var moveSelector = new RandomSubListChangeMoveSelector<>(
                new RandomSubListSelector<>(
                        mockEntitySelector(a, b, c),
                        mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v4, v1),
                        minimumSubListSize,
                        maximumSubListSize),
                mockNeverEndingDestinationSelector(destinationSize, new LocationInList(b, 0)),
                false);

        var random = new TestRandom(0, 1, -1);

        solvingStarted(moveSelector, scoreDirector, random);

        // Only subLists of size 2 are selected.
        assertCodesOfNeverEndingMoveSelector(moveSelector,
                "|2| {A[0..2]->B[0]}",
                "|2| {A[1..3]->B[0]}");

        random.assertIntBoundJustRequested(subListCount);
    }

    @Test
    void sizeUnrestricted() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var v4 = new TestdataListValue("4");
        var v5 = new TestdataListValue("5");
        var a = TestdataListEntity.createWithValues("A", v1, v2, v3);
        var b = TestdataListEntity.createWithValues("B");
        var c = TestdataListEntity.createWithValues("C", v4, v5);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b, c));
        solution.setValueList(List.of(v1, v2, v3, v4, v5));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var minimumSubListSize = 1;
        var maximumSubListSize = Integer.MAX_VALUE;
        var subListCount = 9;
        var destinationSize = 25; // arbitrary

        assertThat(subListCount).isEqualTo(nthTriangle(listSize(a)) + nthTriangle(listSize(b)) + nthTriangle(listSize(c)));

        var moveSelector = new RandomSubListChangeMoveSelector<>(
                new RandomSubListSelector<>(
                        mockEntitySelector(a, b, c), // affects subList calculation and the move selector size
                        mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector),
                                v1, v2, v3, v4, v5),
                        minimumSubListSize,
                        maximumSubListSize),
                mockNeverEndingDestinationSelector(destinationSize, ElementLocation.unassigned()),
                false);

        var random = new TestRandom(0, 0);

        solvingStarted(moveSelector, scoreDirector, random);

        assertCodesOfNeverEndingMoveSelector(moveSelector, subListCount * destinationSize);
    }

    @Test
    void sizeWithBounds() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var v4 = new TestdataListValue("4");
        var v5 = new TestdataListValue("5");
        var v6 = new TestdataListValue("6");
        var v7 = new TestdataListValue("7");
        var v11 = new TestdataListValue("11");
        var v12 = new TestdataListValue("12");
        var v13 = new TestdataListValue("13");
        var v21 = new TestdataListValue("21");
        var v22 = new TestdataListValue("22");
        var v23 = new TestdataListValue("23");
        var v24 = new TestdataListValue("24");
        var a = TestdataListEntity.createWithValues("A", v1, v2, v3, v4, v5, v6, v7);
        var b = TestdataListEntity.createWithValues("B");
        var c = TestdataListEntity.createWithValues("C", v11, v12, v13);
        var d = TestdataListEntity.createWithValues("D", v21, v22, v23, v24);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(List.of(v1, v2, v3, v4, v5, v6, v7, v11, v12, v13, v21, v22, v23, v24));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var minimumSubListSize = 3;
        var maximumSubListSize = 5;
        var subListCount = 16;
        var destinationSize = 7; // arbitrary

        var moveSelector = new RandomSubListChangeMoveSelector<>(
                new RandomSubListSelector<>(
                        mockEntitySelector(a, b, c, d), // affects subList calculation and the move selector size
                        mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v1),
                        minimumSubListSize,
                        maximumSubListSize),
                mockNeverEndingDestinationSelector(destinationSize, new LocationInList(b, 0)),
                false);

        var random = new TestRandom(0);

        solvingStarted(moveSelector, scoreDirector, random);

        assertCodesOfNeverEndingMoveSelector(moveSelector, subListCount * destinationSize);
    }

    @Test
    void phaseLifecycle() {
        var minimumSubListSize = 1;
        var maximumSubListSize = Integer.MAX_VALUE;

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());

        var entitySelector = mockEntitySelector(new TestdataListEntity[0]);
        var valueSelector =
                mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector));
        var destinationSelector = mockNeverEndingDestinationSelector();

        var moveSelector = new RandomSubListChangeMoveSelector<>(
                new RandomSubListSelector<>(
                        entitySelector,
                        valueSelector,
                        minimumSubListSize,
                        maximumSubListSize),
                destinationSelector,
                false);

        var solverScope = solvingStarted(moveSelector, scoreDirector);
        var phaseScope = phaseStarted(moveSelector, solverScope);

        var stepScope1 = stepStarted(moveSelector, phaseScope);
        moveSelector.stepEnded(stepScope1);

        var stepScope2 = stepStarted(moveSelector, phaseScope);
        moveSelector.stepEnded(stepScope2);

        moveSelector.phaseEnded(phaseScope);
        moveSelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(entitySelector, 1, 1, 2);
        verifyPhaseLifecycle(valueSelector, 1, 1, 2);
        verifyPhaseLifecycle(destinationSelector, 1, 1, 2);
    }
}
