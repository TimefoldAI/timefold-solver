package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertAllCodesOfValueSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.verifyPhaseLifecycle;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Comparator;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

import org.junit.jupiter.api.Test;

class SortingValueSelectorTest {

    @Test
    void originalSelectionCacheTypeSolver() {
        runOriginalSelection(SelectionCacheType.SOLVER, 1);
    }

    @Test
    void originalSelectionCacheTypePhase() {
        runOriginalSelection(SelectionCacheType.PHASE, 2);
    }

    @Test
    void originalSelectionCacheTypeStep() {
        runOriginalSelection(SelectionCacheType.STEP, 5);
    }

    public void runOriginalSelection(SelectionCacheType cacheType, int timesCalled) {
        EntityIndependentValueSelector childValueSelector = SelectorTestUtils.mockEntityIndependentValueSelector(
                TestdataEntity.class, "value",
                new TestdataValue("jan"), new TestdataValue("feb"), new TestdataValue("mar"),
                new TestdataValue("apr"), new TestdataValue("may"), new TestdataValue("jun"));

        SelectionSorter<TestdataSolution, TestdataValue> sorter = (scoreDirector, selectionList) -> selectionList
                .sort(Comparator.comparing(TestdataObject::getCode));
        EntityIndependentValueSelector valueSelector = new SortingValueSelector(childValueSelector, cacheType, sorter);

        SolverScope solverScope = mock(SolverScope.class);
        valueSelector.solvingStarted(solverScope);

        AbstractPhaseScope phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        valueSelector.phaseStarted(phaseScopeA);

        AbstractStepScope stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        valueSelector.stepStarted(stepScopeA1);
        assertAllCodesOfValueSelector(valueSelector, "apr", "feb", "jan", "jun", "mar", "may");
        valueSelector.stepEnded(stepScopeA1);

        AbstractStepScope stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        valueSelector.stepStarted(stepScopeA2);
        assertAllCodesOfValueSelector(valueSelector, "apr", "feb", "jan", "jun", "mar", "may");
        valueSelector.stepEnded(stepScopeA2);

        valueSelector.phaseEnded(phaseScopeA);

        AbstractPhaseScope phaseScopeB = mock(AbstractPhaseScope.class);
        when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
        valueSelector.phaseStarted(phaseScopeB);

        AbstractStepScope stepScopeB1 = mock(AbstractStepScope.class);
        when(stepScopeB1.getPhaseScope()).thenReturn(phaseScopeB);
        valueSelector.stepStarted(stepScopeB1);
        assertAllCodesOfValueSelector(valueSelector, "apr", "feb", "jan", "jun", "mar", "may");
        valueSelector.stepEnded(stepScopeB1);

        AbstractStepScope stepScopeB2 = mock(AbstractStepScope.class);
        when(stepScopeB2.getPhaseScope()).thenReturn(phaseScopeB);
        valueSelector.stepStarted(stepScopeB2);
        assertAllCodesOfValueSelector(valueSelector, "apr", "feb", "jan", "jun", "mar", "may");
        valueSelector.stepEnded(stepScopeB2);

        AbstractStepScope stepScopeB3 = mock(AbstractStepScope.class);
        when(stepScopeB3.getPhaseScope()).thenReturn(phaseScopeB);
        valueSelector.stepStarted(stepScopeB3);
        assertAllCodesOfValueSelector(valueSelector, "apr", "feb", "jan", "jun", "mar", "may");
        valueSelector.stepEnded(stepScopeB3);

        valueSelector.phaseEnded(phaseScopeB);

        valueSelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(childValueSelector, 1, 2, 5);
        verify(childValueSelector, times(timesCalled)).iterator();
        verify(childValueSelector, times(timesCalled)).getSize();
    }

}
