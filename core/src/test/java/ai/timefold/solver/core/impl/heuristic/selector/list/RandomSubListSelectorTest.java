package ai.timefold.solver.core.impl.heuristic.selector.list;

import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.phaseStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.solvingStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.stepStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.list.TriangularNumbers.nthTriangle;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getAllowsUnassignedvaluesListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getPinnedListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.listSize;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockEntitySelector;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockNeverEndingEntityIndependentValueSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCodesOfNeverEndingIterableSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertEmptyNeverEndingIterableSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.verifyPhaseLifecycle;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

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
import ai.timefold.solver.core.impl.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class RandomSubListSelectorTest {

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

        // The number of subLists of [1, 2, 3, 4] is the 4th triangular number (10).
        assertThat(subListCount).isEqualTo(nthTriangle(listSize(a)) + nthTriangle(listSize(b)));

        var selector = new RandomSubListSelector<>(
                mockEntitySelector(a, b),
                mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v1),
                minimumSubListSize,
                maximumSubListSize);

        var random = new TestRandom(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0);

        solvingStarted(selector, scoreDirector, random);

        // Every possible subList is selected.
        assertCodesOfNeverEndingIterableSelector(selector, subListCount,
                "A[0+4]",
                "A[0+3]", "A[1+3]",
                "A[0+2]", "A[1+2]", "A[2+2]",
                "A[0+1]", "A[1+1]", "A[2+1]", "A[3+1]");
        random.assertIntBoundJustRequested(subListCount);
    }

    @Test
    void randomWithPinning() {
        var v1 = new TestdataPinnedWithIndexListValue("1");
        var v2 = new TestdataPinnedWithIndexListValue("2");
        var v3 = new TestdataPinnedWithIndexListValue("3");
        var v4 = new TestdataPinnedWithIndexListValue("4");
        var a = TestdataPinnedWithIndexListEntity.createWithValues("A", v1, v2, v3, v4);
        a.setPlanningPinToIndex(1); // Ignore v1.
        var b = TestdataPinnedWithIndexListEntity.createWithValues("B");
        var solution = new TestdataPinnedWithIndexListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(List.of(v1, v2, v3, v4));

        var scoreDirector = mockScoreDirector(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var minimumSubListSize = 1;
        var maximumSubListSize = Integer.MAX_VALUE;
        var subListCount = 6;

        // The number of subLists of [2, 3, 4] is the 3rd triangular number (6).
        assertThat(subListCount).isEqualTo(nthTriangle(listSize(a) - 1) + nthTriangle(listSize(b)));

        var selector = new RandomSubListSelector<>(
                mockEntitySelector(a, b),
                mockNeverEndingEntityIndependentValueSelector(getPinnedListVariableDescriptor(scoreDirector), v1, v2),
                minimumSubListSize,
                maximumSubListSize);

        var random = new TestRandom(0, 1, 2, 3, 4, 5, 0);

        var solverScope = solvingStarted(selector, scoreDirector, random);
        phaseStarted(selector, solverScope);

        // Every possible subList is selected.
        assertCodesOfNeverEndingIterableSelector(selector, subListCount,
                "A[1+3]",
                "A[1+2]", "A[2+2]",
                "A[1+1]", "A[2+1]", "A[3+1]");
        random.assertIntBoundJustRequested(subListCount);
    }

    @Test
    void randomAllowsUnassignedValues() {
        var v1 = new TestdataAllowsUnassignedValuesListValue("1");
        var v2 = new TestdataAllowsUnassignedValuesListValue("2");
        var v3 = new TestdataAllowsUnassignedValuesListValue("3");
        var v4 = new TestdataAllowsUnassignedValuesListValue("4");
        var a = TestdataAllowsUnassignedValuesListEntity.createWithValues("A", v1, v2, v3);
        var b = TestdataAllowsUnassignedValuesListEntity.createWithValues("B");
        var solution = new TestdataAllowsUnassignedValuesListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(List.of(v1, v2, v3, v4));

        var scoreDirector = mockScoreDirector(TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var minimumSubListSize = 1;
        var maximumSubListSize = Integer.MAX_VALUE;
        var subListCount = 6;

        // The number of subLists of [1, 2, 3] is the 3rd triangular number (6).
        assertThat(subListCount).isEqualTo(nthTriangle(listSize(a)) + nthTriangle(listSize(b)));

        var selector = new RandomSubListSelector<>(
                mockEntitySelector(a, b),
                mockNeverEndingEntityIndependentValueSelector(getAllowsUnassignedvaluesListVariableDescriptor(scoreDirector),
                        v1, v2, v3, v4),
                minimumSubListSize,
                maximumSubListSize);

        var random = new TestRandom(0, 1, 2, 3, 4, 5, 0);

        solvingStarted(selector, scoreDirector, random);

        // Every possible subList is selected.
        assertCodesOfNeverEndingIterableSelector(selector, subListCount,
                "A[0+3]",
                "A[0+2]", "A[1+2]",
                "A[0+1]", "A[1+1]", "A[2+1]");
        random.assertIntBoundJustRequested(subListCount);
    }

    @Test
    void randomWithSubListSizeBounds() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var v4 = new TestdataListValue("4");
        var v5 = new TestdataListValue("5");
        var a = TestdataListEntity.createWithValues("A", v1, v2, v3, v4, v5);
        var b = TestdataListEntity.createWithValues("B");
        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(List.of(v1, v2, v3, v4, v5));

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);

        var minimumSubListSize = 2;
        var maximumSubListSize = 3;
        var subListCount = 15 - 5 - 3;

        var selector = new RandomSubListSelector<>(
                mockEntitySelector(a, b),
                mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v1),
                minimumSubListSize,
                maximumSubListSize);

        var random = new TestRandom(0, 1, 2, 3, 4, 5, 6, 0);

        solvingStarted(selector, scoreDirector, random);

        // Every possible subList is selected.
        assertCodesOfNeverEndingIterableSelector(selector, subListCount,
                "A[0+3]", "A[1+3]", "A[2+3]",
                "A[0+2]", "A[1+2]", "A[2+2]", "A[3+2]");
        random.assertIntBoundJustRequested(subListCount);
    }

    @Test
    void emptyWhenMinimumSubListSizeGreaterThanListSize() {
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var a = TestdataListEntity.createWithValues("A", v1, v2, v3);

        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());

        var minimumSubListSize = 4;
        var maximumSubListSize = Integer.MAX_VALUE;

        var selector = new RandomSubListSelector<>(
                mockEntitySelector(a),
                mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector)),
                minimumSubListSize,
                maximumSubListSize);

        solvingStarted(selector, scoreDirector);

        assertEmptyNeverEndingIterableSelector(selector, 0);
    }

    @Test
    void phaseLifecycle() {
        var scoreDirector = mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());

        var entitySelector = mockEntitySelector(new TestdataListEntity[0]);
        var valueSelector = mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector));

        var minimumSubListSize = 1;
        var maximumSubListSize = Integer.MAX_VALUE;

        var selector = new RandomSubListSelector<>(
                entitySelector,
                valueSelector,
                minimumSubListSize,
                maximumSubListSize);

        var solverScope = solvingStarted(selector, scoreDirector);
        var phaseScope = phaseStarted(selector, solverScope);

        var stepScope1 = stepStarted(selector, phaseScope);
        selector.stepEnded(stepScope1);

        var stepScope2 = stepStarted(selector, phaseScope);
        selector.stepEnded(stepScope2);

        selector.phaseEnded(phaseScope);
        selector.solvingEnded(solverScope);

        verifyPhaseLifecycle(entitySelector, 1, 1, 2);
        verifyPhaseLifecycle(valueSelector, 1, 1, 2);
    }

    @Test
    void validateConstructorArguments() {
        var entitySelector = mockEntitySelector(new TestdataListEntity[0]);
        var valueSelector =
                mockNeverEndingEntityIndependentValueSelector(TestdataListEntity.buildVariableDescriptorForValueList());

        assertThatIllegalArgumentException().isThrownBy(() -> new RandomSubListSelector<>(
                entitySelector, valueSelector, 0, 5))
                .withMessageContaining("greater than 0");
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomSubListSelector<>(
                entitySelector, valueSelector, 2, 1))
                .withMessageContaining("less than or equal to the maximum");
        assertThatNoException().isThrownBy(() -> new RandomSubListSelector<>(
                entitySelector, valueSelector, 1, 1));
    }
}
