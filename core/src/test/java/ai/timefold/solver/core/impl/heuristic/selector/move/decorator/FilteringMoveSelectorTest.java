package ai.timefold.solver.core.impl.heuristic.selector.move.decorator;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfMoveSelector;
import static ai.timefold.solver.core.testutil.PlannerAssert.verifyPhaseLifecycle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Iterator;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.heuristic.move.DummyMove;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.BasicPlumbingTermination;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class FilteringMoveSelectorTest {

    @Test
    void filterCacheTypeSolver() {
        filter(SelectionCacheType.SOLVER, 1);
    }

    @Test
    void filterCacheTypePhase() {
        filter(SelectionCacheType.PHASE, 2);
    }

    @Test
    void filterCacheTypeStep() {
        filter(SelectionCacheType.STEP, 5);
    }

    @Test
    void filterCacheTypeJustInTime() {
        filter(SelectionCacheType.JUST_IN_TIME, 5);
    }

    @Test
    void bailOutByTermination() {
        var phaseScope = mock(AbstractPhaseScope.class);
        var moveSelector = mock(MoveSelector.class);
        var termination = mock(BasicPlumbingTermination.class);
        var iterator = mock(Iterator.class);
        when(moveSelector.getSize()).thenReturn(1000L);
        when(moveSelector.isNeverEnding()).thenReturn(true);
        when(moveSelector.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(true);
        when(phaseScope.getTermination()).thenReturn(termination);
        when(termination.isPhaseTerminated(any(AbstractPhaseScope.class))).thenReturn(false, true);
        var filteredMoveSelector = FilteringMoveSelector.of(moveSelector, (scoreDirector, selection) -> false);
        filteredMoveSelector.phaseStarted(phaseScope);
        assertThat(filteredMoveSelector.iterator().hasNext()).isFalse();
        // The termination returns true at the second call, and 2000 calls are executed in total
        verify(iterator, times(2000)).next();
    }

    public void filter(SelectionCacheType cacheType, int timesCalled) {
        MoveSelector childMoveSelector = SelectorTestUtils.mockMoveSelector(
                new DummyMove("a1"), new DummyMove("a2"), new DummyMove("a3"), new DummyMove("a4"));

        SelectionFilter<TestdataSolution, DummyMove> filter = (scoreDirector, move) -> !move.getCode().equals("a3");
        MoveSelector moveSelector = FilteringMoveSelector.of(childMoveSelector, (SelectionFilter) filter);
        if (cacheType.isCached()) {
            moveSelector = new CachingMoveSelector(moveSelector, cacheType, false);
        }

        SolverScope solverScope = mock(SolverScope.class);
        moveSelector.solvingStarted(solverScope);

        AbstractPhaseScope phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        moveSelector.phaseStarted(phaseScopeA);

        AbstractStepScope stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        moveSelector.stepStarted(stepScopeA1);
        assertAllCodesOfMoveSelector(moveSelector, (cacheType.isNotCached() ? 4L : 3L), "a1", "a2", "a4");
        moveSelector.stepEnded(stepScopeA1);

        AbstractStepScope stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        moveSelector.stepStarted(stepScopeA2);
        assertAllCodesOfMoveSelector(moveSelector, (cacheType.isNotCached() ? 4L : 3L), "a1", "a2", "a4");
        moveSelector.stepEnded(stepScopeA2);

        moveSelector.phaseEnded(phaseScopeA);

        AbstractPhaseScope phaseScopeB = mock(AbstractPhaseScope.class);
        when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
        moveSelector.phaseStarted(phaseScopeB);

        AbstractStepScope stepScopeB1 = mock(AbstractStepScope.class);
        when(stepScopeB1.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB1);
        assertAllCodesOfMoveSelector(moveSelector, (cacheType.isNotCached() ? 4L : 3L), "a1", "a2", "a4");
        moveSelector.stepEnded(stepScopeB1);

        AbstractStepScope stepScopeB2 = mock(AbstractStepScope.class);
        when(stepScopeB2.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB2);
        assertAllCodesOfMoveSelector(moveSelector, (cacheType.isNotCached() ? 4L : 3L), "a1", "a2", "a4");
        moveSelector.stepEnded(stepScopeB2);

        AbstractStepScope stepScopeB3 = mock(AbstractStepScope.class);
        when(stepScopeB3.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB3);
        assertAllCodesOfMoveSelector(moveSelector, (cacheType.isNotCached() ? 4L : 3L), "a1", "a2", "a4");
        moveSelector.stepEnded(stepScopeB3);

        moveSelector.phaseEnded(phaseScopeB);

        moveSelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(childMoveSelector, 1, 2, 5);
        verify(childMoveSelector, times(timesCalled)).iterator();
        verify(childMoveSelector, times(timesCalled)).getSize();
    }

}
