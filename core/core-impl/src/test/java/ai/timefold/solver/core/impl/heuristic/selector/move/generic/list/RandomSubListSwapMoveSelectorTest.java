package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.phaseStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.solvingStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.stepStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.list.TriangularNumbers.nthTriangle;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getAllowsUnassignedvaluesListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.listSize;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockEntitySelector;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockNeverEndingEntityIndependentValueSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCodesOfNeverEndingMoveSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertEmptyNeverEndingMoveSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.verifyPhaseLifecycle;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockScoreDirector;

import java.util.List;

import ai.timefold.solver.core.impl.heuristic.selector.list.RandomSubListSelector;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.impl.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class RandomSubListSwapMoveSelectorTest {

    @Test
    void sameEntityUnrestricted() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var v4 = new TestdataListValue("4");
        var a = TestdataListEntity.createWithValues("A", v1, v2, v3, v4);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a));
        solution.setValueList(List.of(v1, v2, v3, v4));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var minimumSubListSize = 1;
        var maximumSubListSize = Integer.MAX_VALUE;
        var subListCount = 10;

        var entitySelector = mockEntitySelector(a);
        var valueSelector = mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v1);
        var moveSelector = new RandomSubListSwapMoveSelector<>(
                new RandomSubListSelector<>(
                        entitySelector,
                        valueSelector,
                        minimumSubListSize,
                        maximumSubListSize),
                new RandomSubListSelector<>(
                        entitySelector,
                        valueSelector,
                        minimumSubListSize,
                        maximumSubListSize),
                false);

        // Alternating left and right subList indexes.
        //      L, R
        var random = new TestRandom(
                0, 0,
                0, 1,
                0, 2,
                0, 3,
                0, 4,
                0, 5,
                0, 6,
                0, 7,
                0, 8,
                0, 9,
                1, 8,
                2, 7,
                3, 6,
                4, 5,
                5, 4,
                6, 3,
                7, 2,
                8, 1,
                9, 0,
                0, 0);

        solvingStarted(moveSelector, scoreDirector, random);

        assertCodesOfNeverEndingMoveSelector(moveSelector, subListCount * subListCount,
                "{A[0+4]} <-> {A[0+4]}",
                "{A[0+4]} <-> {A[0+3]}",
                "{A[0+4]} <-> {A[1+3]}",
                "{A[0+4]} <-> {A[0+2]}",
                "{A[0+4]} <-> {A[1+2]}",
                "{A[0+4]} <-> {A[2+2]}",
                "{A[0+4]} <-> {A[0+1]}",
                "{A[0+4]} <-> {A[1+1]}",
                "{A[0+4]} <-> {A[2+1]}",
                "{A[0+4]} <-> {A[3+1]}",
                "{A[0+3]} <-> {A[2+1]}",
                "{A[1+3]} <-> {A[1+1]}",
                "{A[0+2]} <-> {A[0+1]}",
                "{A[1+2]} <-> {A[2+2]}",
                "{A[1+2]} <-> {A[2+2]}", // equivalent to {A[2+2]} <-> {A[1+2]}
                "{A[0+1]} <-> {A[0+2]}",
                "{A[1+1]} <-> {A[1+3]}",
                "{A[0+3]} <-> {A[2+1]}", // equivalent to {A[2+1]} <-> {A[0+3]}
                "{A[0+4]} <-> {A[3+1]}"); // equivalent to {A[3+1]} <-> {A[0+4]}

        random.assertIntBoundJustRequested(subListCount);
    }

    @Test
    void reversing() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var v5 = new TestdataListValue("5");
        var v6 = new TestdataListValue("6");
        var a = TestdataListEntity.createWithValues("A", v1, v2, v3);
        var b = TestdataListEntity.createWithValues("B", v5, v6);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(List.of(v1, v2, v3, v5, v6));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var minimumSubListSize = 1;
        var maximumSubListSize = Integer.MAX_VALUE;
        var subListCount = 6 + 3;

        var listVariableDescriptor = getListVariableDescriptor(scoreDirector);
        var entitySelector = mockEntitySelector(a, b);
        var moveSelector = new RandomSubListSwapMoveSelector<>(
                new RandomSubListSelector<>(
                        entitySelector,
                        mockNeverEndingEntityIndependentValueSelector(listVariableDescriptor, v1),
                        minimumSubListSize,
                        maximumSubListSize),
                new RandomSubListSelector<>(
                        entitySelector,
                        mockNeverEndingEntityIndependentValueSelector(listVariableDescriptor, v5),
                        minimumSubListSize,
                        maximumSubListSize),
                true);

        // Each row is consumed by 1 createUpcomingSelection() call.
        // Columns are: left subList index, right subList index, reversing flag.
        var random = new TestRandom(
                0, 2, 1,
                0, 1, 0,
                0, 0, 1,
                1, 0, 0,
                2, 0, 1,
                3, 0, 1,
                0, 0, 0);

        solvingStarted(moveSelector, scoreDirector, random);

        assertCodesOfNeverEndingMoveSelector(moveSelector, subListCount * subListCount * 2,
                "{A[0+3]} <-reversing-> {B[1+1]}",
                "{A[0+3]} <-> {B[0+1]}",
                "{A[0+3]} <-reversing-> {B[0+2]}",
                "{A[0+2]} <-> {B[0+2]}",
                "{A[1+2]} <-reversing-> {B[0+2]}",
                "{A[0+1]} <-reversing-> {B[0+2]}");
    }

    @Test
    void sameEntityWithSubListSizeBounds() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var v4 = new TestdataListValue("4");
        var a = TestdataListEntity.createWithValues("A", v1, v2, v3, v4);
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a));
        solution.setValueList(List.of(v1, v2, v3, v4));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var minimumSubListSize = 2;
        var maximumSubListSize = 3;
        var subListCount = 5;

        var entitySelector = mockEntitySelector(a);
        var valueSelector = mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v1);
        var moveSelector = new RandomSubListSwapMoveSelector<>(
                new RandomSubListSelector<>(
                        entitySelector,
                        valueSelector,
                        minimumSubListSize,
                        maximumSubListSize),
                new RandomSubListSelector<>(
                        entitySelector,
                        valueSelector,
                        minimumSubListSize,
                        maximumSubListSize),
                false);

        // Alternating left and right subList indexes.
        //      L, R
        var random = new TestRandom(
                0, 0,
                0, 1,
                0, 2,
                0, 3,
                0, 4,
                1, 3,
                2, 2,
                3, 1,
                4, 0,
                0, 0);

        solvingStarted(moveSelector, scoreDirector, random);

        assertCodesOfNeverEndingMoveSelector(moveSelector, subListCount * subListCount,
                "{A[0+3]} <-> {A[0+3]}",
                "{A[0+3]} <-> {A[1+3]}",
                "{A[0+3]} <-> {A[0+2]}",
                "{A[0+3]} <-> {A[1+2]}",
                "{A[0+3]} <-> {A[2+2]}",
                "{A[1+3]} <-> {A[1+2]}",
                "{A[0+2]} <-> {A[0+2]}",
                "{A[1+2]} <-> {A[1+3]}",
                "{A[0+3]} <-> {A[2+2]}"); // equivalent to {A[2+2]} <-> {A[0+3]}

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

        var entitySelector = mockEntitySelector(a);
        var valueSelector = mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v1);
        var moveSelector = new RandomSubListSwapMoveSelector<>(
                new RandomSubListSelector<>(
                        entitySelector,
                        valueSelector,
                        minimumSubListSize,
                        maximumSubListSize),
                new RandomSubListSelector<>(
                        entitySelector,
                        valueSelector,
                        minimumSubListSize,
                        maximumSubListSize),
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

        var listVariableDescriptor = getListVariableDescriptor(scoreDirector);
        var entitySelector = mockEntitySelector(a, b, c);
        var moveSelector = new RandomSubListSwapMoveSelector<>(
                new RandomSubListSelector<>(
                        entitySelector,
                        mockNeverEndingEntityIndependentValueSelector(listVariableDescriptor, v1, v4),
                        minimumSubListSize,
                        maximumSubListSize),
                new RandomSubListSelector<>(
                        entitySelector,
                        mockNeverEndingEntityIndependentValueSelector(listVariableDescriptor, v4, v1),
                        minimumSubListSize,
                        maximumSubListSize),
                false);

        // Alternating left and right subList indexes.
        //      L, R
        var random = new TestRandom(
                0, 0,
                0, 0,
                0, 1,
                1, 0,
                1, 1,
                0, 0);

        solvingStarted(moveSelector, scoreDirector, random);

        assertCodesOfNeverEndingMoveSelector(moveSelector, subListCount * subListCount,
                "{A[0+2]} <-> {A[0+2]}",
                "{A[0+2]} <-> {A[0+2]}",
                "{A[0+2]} <-> {A[1+2]}",
                "{A[0+2]} <-> {A[1+2]}", // normalized from {A[1+2]} <-> {A[0+2]}
                "{A[1+2]} <-> {A[1+2]}");

        random.assertIntBoundJustRequested(subListCount);
    }

    @Test
    void allowsUnassignedValues() {
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
        var subListCount = nthTriangle(listSize(a)) + nthTriangle(listSize(b));

        // The entity selector must be complete; it affects subList calculation and the move selector size.
        var entitySelector = mockEntitySelector(a, b);
        var moveSelector = new RandomSubListSwapMoveSelector<>(
                new RandomSubListSelector<>(
                        entitySelector,
                        mockNeverEndingEntityIndependentValueSelector(
                                getAllowsUnassignedvaluesListVariableDescriptor(scoreDirector),
                                v1, v1, v1, v1, v1, v1, v1, v1, v1, v1, v1, v1, v3, v3, v3, v3),
                        minimumSubListSize,
                        maximumSubListSize),
                new RandomSubListSelector<>(
                        entitySelector,
                        mockNeverEndingEntityIndependentValueSelector(
                                getAllowsUnassignedvaluesListVariableDescriptor(scoreDirector),
                                v1, v1, v1, v3),
                        minimumSubListSize,
                        maximumSubListSize),
                false);

        var random = new TestRandom(
                0, 0, 0, 1, 0, 2, 0, 0,
                1, 0, 1, 1, 1, 2, 1, 0,
                2, 0, 2, 1, 2, 2, 2, 0,
                0, 0, 0, 1, 0, 2, 0, 0,
                0, 0);

        solvingStarted(moveSelector, scoreDirector, random);

        assertCodesOfNeverEndingMoveSelector(moveSelector, (long) subListCount * subListCount,
                "{A[0+2]} <-> {A[0+2]}",
                "{A[0+2]} <-> {A[0+1]}",
                "{A[0+2]} <-> {A[1+1]}",
                "{A[0+2]} <-> {B[0+1]}",
                "{A[0+1]} <-> {A[0+2]}",
                "{A[0+1]} <-> {A[0+1]}",
                "{A[0+1]} <-> {A[1+1]}",
                "{A[0+1]} <-> {B[0+1]}",
                "{A[0+2]} <-> {A[1+1]}",
                "{A[0+1]} <-> {A[1+1]}",
                "{A[1+1]} <-> {A[1+1]}",
                "{A[1+1]} <-> {B[0+1]}",
                "{B[0+1]} <-> {A[0+2]}",
                "{B[0+1]} <-> {A[0+1]}",
                "{B[0+1]} <-> {A[1+1]}",
                "{B[0+1]} <-> {B[0+1]}");
    }

    @Test
    void sizeUnrestricted() {
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

        var minimumSubListSize = 1;
        var maximumSubListSize = Integer.MAX_VALUE;
        var subListCount = 6 + 1;

        // The entity selector must be complete; it affects subList calculation and the move selector size.
        var entitySelector = mockEntitySelector(a, b, c);
        var valueSelector = mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v1);
        var moveSelector = new RandomSubListSwapMoveSelector<>(
                new RandomSubListSelector<>(
                        entitySelector,
                        valueSelector,
                        minimumSubListSize,
                        maximumSubListSize),
                new RandomSubListSelector<>(
                        entitySelector,
                        valueSelector,
                        minimumSubListSize,
                        maximumSubListSize),
                false);

        var random = new TestRandom(0, 0);

        solvingStarted(moveSelector, scoreDirector, random);

        assertCodesOfNeverEndingMoveSelector(moveSelector, subListCount * subListCount);
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
        solution.setEntityList(List.of(a, b, c, d));
        solution.setValueList(List.of(v1, v2, v3, v4, v5, v6, v7, v11, v12, v13, v21, v22, v23, v24));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var minimumSubListSize = 3;
        var maximumSubListSize = 5;
        var subListCount = 12 + 1 + 3;

        // The entity selector must be complete; it affects subList calculation and the move selector size.
        var entitySelector = mockEntitySelector(a, b, c, d);
        var valueSelector = mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v1);
        var moveSelector = new RandomSubListSwapMoveSelector<>(
                new RandomSubListSelector<>(
                        entitySelector,
                        valueSelector,
                        minimumSubListSize,
                        maximumSubListSize),
                new RandomSubListSelector<>(
                        entitySelector,
                        valueSelector,
                        minimumSubListSize,
                        maximumSubListSize),
                false);

        var random = new TestRandom(0, 0);

        solvingStarted(moveSelector, scoreDirector, random);

        assertCodesOfNeverEndingMoveSelector(moveSelector, subListCount * subListCount);
    }

    @Test
    void phaseLifecycle() {
        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());

        var listVariableDescriptor = getListVariableDescriptor(scoreDirector);

        var entitySelector = mockEntitySelector(new TestdataListEntity[0]);
        var leftValueSelector = mockNeverEndingEntityIndependentValueSelector(listVariableDescriptor);
        var rightValueSelector = mockNeverEndingEntityIndependentValueSelector(listVariableDescriptor);
        var minimumSubListSize = 1;
        var maximumSubListSize = Integer.MAX_VALUE;

        var moveSelector = new RandomSubListSwapMoveSelector<>(
                new RandomSubListSelector<>(
                        entitySelector,
                        leftValueSelector,
                        minimumSubListSize,
                        maximumSubListSize),
                new RandomSubListSelector<>(
                        entitySelector,
                        rightValueSelector,
                        minimumSubListSize,
                        maximumSubListSize),
                false);

        var solverScope = solvingStarted(moveSelector, scoreDirector);
        var phaseScope = phaseStarted(moveSelector, solverScope);

        var stepScope1 = stepStarted(moveSelector, phaseScope);
        moveSelector.stepEnded(stepScope1);

        var stepScope2 = stepStarted(moveSelector, phaseScope);
        moveSelector.stepEnded(stepScope2);

        moveSelector.phaseEnded(phaseScope);
        moveSelector.solvingEnded(solverScope);

        // The invocation counts are multiplied for the entity selector because it is used by both left and right
        // subList selectors and each registers the entity selector to its phaseLifecycleSupport.
        verifyPhaseLifecycle(entitySelector, 2, 2, 4);
        verifyPhaseLifecycle(leftValueSelector, 1, 1, 2);
        verifyPhaseLifecycle(rightValueSelector, 1, 1, 2);
    }
}
