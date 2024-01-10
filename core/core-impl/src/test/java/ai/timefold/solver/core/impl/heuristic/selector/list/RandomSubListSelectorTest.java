package ai.timefold.solver.core.impl.heuristic.selector.list;

import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.phaseStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.solvingStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.stepStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.list.TriangularNumbers.nthTriangle;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getListVariableDescriptor;
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

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class RandomSubListSelectorTest {

    @Test
    void randomUnrestricted() {
        TestdataListValue v1 = new TestdataListValue("1");
        TestdataListValue v2 = new TestdataListValue("2");
        TestdataListValue v3 = new TestdataListValue("3");
        TestdataListValue v4 = new TestdataListValue("4");
        TestdataListEntity a = TestdataListEntity.createWithValues("A", v1, v2, v3, v4);
        TestdataListEntity b = TestdataListEntity.createWithValues("B");

        InnerScoreDirector<TestdataListSolution, SimpleScore> scoreDirector =
                mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());

        int minimumSubListSize = 1;
        int maximumSubListSize = Integer.MAX_VALUE;
        int subListCount = 10;

        // The number of subLists of [1, 2, 3, 4] is the 4th triangular number (10).
        assertThat(subListCount).isEqualTo(nthTriangle(listSize(a)) + nthTriangle(listSize(b)));

        RandomSubListSelector<TestdataListSolution> selector = new RandomSubListSelector<>(
                mockEntitySelector(a, b),
                mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v1),
                minimumSubListSize,
                maximumSubListSize);

        TestRandom random = new TestRandom(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 99);

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
    void randomWithSubListSizeBounds() {
        TestdataListValue v1 = new TestdataListValue("1");
        TestdataListValue v2 = new TestdataListValue("2");
        TestdataListValue v3 = new TestdataListValue("3");
        TestdataListValue v4 = new TestdataListValue("4");
        TestdataListValue v5 = new TestdataListValue("5");
        TestdataListEntity a = TestdataListEntity.createWithValues("A", v1, v2, v3, v4, v5);
        TestdataListEntity b = TestdataListEntity.createWithValues("B");

        InnerScoreDirector<TestdataListSolution, SimpleScore> scoreDirector =
                mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());

        int minimumSubListSize = 2;
        int maximumSubListSize = 3;
        int subListCount = 15 - 5 - 3;

        RandomSubListSelector<TestdataListSolution> selector = new RandomSubListSelector<>(
                mockEntitySelector(a, b),
                mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v1),
                minimumSubListSize,
                maximumSubListSize);

        TestRandom random = new TestRandom(0, 1, 2, 3, 4, 5, 6, 99);

        solvingStarted(selector, scoreDirector, random);

        // Every possible subList is selected.
        assertCodesOfNeverEndingIterableSelector(selector, subListCount,
                "A[0+3]", "A[1+3]", "A[2+3]",
                "A[0+2]", "A[1+2]", "A[2+2]", "A[3+2]");
        random.assertIntBoundJustRequested(subListCount);
    }

    @Test
    void emptyWhenMinimumSubListSizeGreaterThanListSize() {
        TestdataListValue v1 = new TestdataListValue("1");
        TestdataListValue v2 = new TestdataListValue("2");
        TestdataListValue v3 = new TestdataListValue("3");
        TestdataListEntity a = TestdataListEntity.createWithValues("A", v1, v2, v3);

        InnerScoreDirector<TestdataListSolution, SimpleScore> scoreDirector =
                mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());

        int minimumSubListSize = 4;
        int maximumSubListSize = Integer.MAX_VALUE;

        RandomSubListSelector<TestdataListSolution> selector = new RandomSubListSelector<>(
                mockEntitySelector(a),
                mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector)),
                minimumSubListSize,
                maximumSubListSize);

        solvingStarted(selector, scoreDirector);

        assertEmptyNeverEndingIterableSelector(selector, 0);
    }

    @Test
    void phaseLifecycle() {
        InnerScoreDirector<TestdataListSolution, SimpleScore> scoreDirector =
                mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());

        EntitySelector<TestdataListSolution> entitySelector = mockEntitySelector(new TestdataListEntity[0]);
        EntityIndependentValueSelector<TestdataListSolution> valueSelector =
                mockNeverEndingEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector));

        int minimumSubListSize = 1;
        int maximumSubListSize = Integer.MAX_VALUE;

        RandomSubListSelector<TestdataListSolution> selector = new RandomSubListSelector<>(
                entitySelector,
                valueSelector,
                minimumSubListSize,
                maximumSubListSize);

        SolverScope<TestdataListSolution> solverScope = solvingStarted(selector, scoreDirector);
        AbstractPhaseScope<TestdataListSolution> phaseScope = phaseStarted(selector, solverScope);

        AbstractStepScope<TestdataListSolution> stepScope1 = stepStarted(selector, phaseScope);
        selector.stepEnded(stepScope1);

        AbstractStepScope<TestdataListSolution> stepScope2 = stepStarted(selector, phaseScope);
        selector.stepEnded(stepScope2);

        selector.phaseEnded(phaseScope);
        selector.solvingEnded(solverScope);

        verifyPhaseLifecycle(entitySelector, 1, 1, 2);
        verifyPhaseLifecycle(valueSelector, 1, 1, 2);
    }

    @Test
    void validateConstructorArguments() {
        EntitySelector<TestdataListSolution> entitySelector = mockEntitySelector(new TestdataListEntity[0]);
        EntityIndependentValueSelector<TestdataListSolution> valueSelector =
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
