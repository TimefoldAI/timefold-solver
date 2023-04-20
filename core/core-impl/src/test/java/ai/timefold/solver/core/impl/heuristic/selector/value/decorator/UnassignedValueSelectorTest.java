package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.mockEntityIndependentValueSelector;
import static ai.timefold.solver.core.impl.testdata.domain.list.TestdataListUtils.getListVariableDescriptor;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertAllCodesOfIterator;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertAllCodesOfValueSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertAllCodesOfValueSelectorForEntity;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockScoreDirector;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;

import org.junit.jupiter.api.Test;

class UnassignedValueSelectorTest {

    @Test
    void filterOutAssignedValues() {
        TestdataListValue v1 = new TestdataListValue("1");
        TestdataListValue v2 = new TestdataListValue("2");
        TestdataListValue v3 = new TestdataListValue("3");
        TestdataListValue v4 = new TestdataListValue("4");
        TestdataListValue v5 = new TestdataListValue("5");
        // 1 and 3 are assigned, the rest (2, 4, 5) are unassigned.
        TestdataListEntity.createWithValues("A", v1, v3);

        InnerScoreDirector<TestdataListSolution, SimpleScore> scoreDirector =
                mockScoreDirector(TestdataListSolution.buildSolutionDescriptor());

        EntityIndependentValueSelector<TestdataListSolution> childValueSelector =
                mockEntityIndependentValueSelector(getListVariableDescriptor(scoreDirector), v1, v2, v3, v4, v5);

        UnassignedValueSelector<TestdataListSolution> valueSelector = new UnassignedValueSelector<>(childValueSelector);

        SolverScope<TestdataListSolution> solverScope = mock(SolverScope.class);
        valueSelector.solvingStarted(solverScope);

        AbstractPhaseScope<TestdataListSolution> phaseScope = mock(AbstractPhaseScope.class);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        when(phaseScope.<SimpleScore> getScoreDirector()).thenReturn(scoreDirector);
        valueSelector.phaseStarted(phaseScope);

        AbstractStepScope<TestdataListSolution> stepScope = mock(AbstractStepScope.class);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        valueSelector.stepStarted(stepScope);

        assertAllCodesOfValueSelector(valueSelector, "2", "4", "5");

        // Although the entity dependent iterators are not used, they should behave correctly.
        assertAllCodesOfValueSelectorForEntity(valueSelector, null, "2", "4", "5");
        assertAllCodesOfIterator(valueSelector.endingIterator(null), "2", "4", "5");
    }

    @Test
    void requireEndingChildValueSelector() {
        EntityIndependentValueSelector<TestdataListSolution> childValueSelector = mock(EntityIndependentValueSelector.class);

        when(childValueSelector.isNeverEnding()).thenReturn(true);

        assertThatIllegalArgumentException().isThrownBy(() -> new UnassignedValueSelector<>(childValueSelector));
    }
}
