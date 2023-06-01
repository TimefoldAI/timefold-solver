package ai.timefold.solver.enterprise.nearby.value;

import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.mockReplayingValueSelector;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.phaseStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.solvingStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.stepStarted;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.mockEntityIndependentValueSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertAllCodesOfIterableSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCodesOfNeverEndingIterableSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockScoreDirector;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.mimic.MimicReplayingValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.list.TestDistanceMeter;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testutil.TestNearbyRandom;
import ai.timefold.solver.core.impl.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class NearValueNearbyValueSelectorTest {

    @Test
    void originalSelection() {
        TestdataListValue v1 = new TestdataListValue("10");
        TestdataListValue v2 = new TestdataListValue("45");
        TestdataListValue v3 = new TestdataListValue("50");
        TestdataListValue v4 = new TestdataListValue("60");
        TestdataListValue v5 = new TestdataListValue("75");

        InnerScoreDirector<TestdataListSolution, SimpleScore> scoreDirector =
                mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());

        // Used to populate the distance matrix with destinations.
        EntityIndependentValueSelector<TestdataListSolution> valueSelector =
                mockEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v1, v2, v3, v4, v5);

        // The replaying selector determines the destination matrix origin.
        MimicReplayingValueSelector<TestdataListSolution> mockReplayingValueSelector =
                mockReplayingValueSelector(valueSelector.getVariableDescriptor(), v3, v3, v3, v3, v3, v3, v3);

        NearValueNearbyValueSelector<TestdataListSolution> nearbyValueSelector =
                new NearValueNearbyValueSelector<>(valueSelector, mockReplayingValueSelector, new TestDistanceMeter(), null,
                        false);

        // A[0]=v1(10)
        // A[1]=v2(45)
        // A[2]=v3(50) <= origin
        // A[3]=v4(60)
        // B[0]=v5(75)

        SolverScope<TestdataListSolution> solverScope = solvingStarted(nearbyValueSelector, scoreDirector);
        AbstractPhaseScope<TestdataListSolution> phaseScopeA = phaseStarted(nearbyValueSelector, solverScope);
        AbstractStepScope<TestdataListSolution> stepScopeA1 = stepStarted(nearbyValueSelector, phaseScopeA);
        assertAllCodesOfIterableSelector(nearbyValueSelector, valueSelector.getSize(), "50", "45", "60", "75", "10");
        nearbyValueSelector.stepEnded(stepScopeA1);
        nearbyValueSelector.phaseEnded(phaseScopeA);
        nearbyValueSelector.solvingEnded(solverScope);
    }

    @Test
    void randomSelection() {
        TestdataListValue v1 = new TestdataListValue("10");
        TestdataListValue v2 = new TestdataListValue("45");
        TestdataListValue v3 = new TestdataListValue("50");
        TestdataListValue v4 = new TestdataListValue("60");
        TestdataListValue v5 = new TestdataListValue("75");

        InnerScoreDirector<TestdataListSolution, SimpleScore> scoreDirector =
                mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());

        // Used to populate the distance matrix with destinations.
        EntityIndependentValueSelector<TestdataListSolution> valueSelector =
                mockEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v1, v2, v3, v4, v5);

        // The replaying selector determines the destination matrix origin.
        MimicReplayingValueSelector<TestdataListSolution> mockReplayingValueSelector =
                mockReplayingValueSelector(valueSelector.getVariableDescriptor(), v3, v3, v3, v3, v3, v3);

        NearValueNearbyValueSelector<TestdataListSolution> nearbyValueSelector =
                new NearValueNearbyValueSelector<>(valueSelector, mockReplayingValueSelector, new TestDistanceMeter(),
                        new TestNearbyRandom(), true);

        TestRandom testRandom = new TestRandom(3, 2, 1, 4, 0); // nearbyIndices (=> destinations)

        // A[0]=v1(10)
        // A[1]=v2(45)
        // A[2]=v3(50) <= origin
        // A[3]=v4(60)
        // B[0]=v5(75)

        SolverScope<TestdataListSolution> solverScope = solvingStarted(nearbyValueSelector, scoreDirector, testRandom);
        AbstractPhaseScope<TestdataListSolution> phaseScopeA = phaseStarted(nearbyValueSelector, solverScope);
        AbstractStepScope<TestdataListSolution> stepScopeA1 = stepStarted(nearbyValueSelector, phaseScopeA);
        //                                                        3     2     1     4     0
        assertCodesOfNeverEndingIterableSelector(nearbyValueSelector, valueSelector.getSize(), "75", "60", "45", "10", "50");
        nearbyValueSelector.stepEnded(stepScopeA1);
        nearbyValueSelector.phaseEnded(phaseScopeA);
        nearbyValueSelector.solvingEnded(solverScope);
    }
}
