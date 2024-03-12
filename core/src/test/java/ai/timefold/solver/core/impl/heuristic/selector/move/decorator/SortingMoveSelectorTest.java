package ai.timefold.solver.core.impl.heuristic.selector.move.decorator;

import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertAllCodesOfMoveSelector;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.verifyPhaseLifecycle;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Comparator;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.heuristic.move.DummyMove;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class SortingMoveSelectorTest {

    @Test
    void cacheTypeSolver() {
        runCacheType(SelectionCacheType.SOLVER, 1);
    }

    @Test
    void cacheTypePhase() {
        runCacheType(SelectionCacheType.PHASE, 2);
    }

    @Test
    void cacheTypeStep() {
        runCacheType(SelectionCacheType.STEP, 5);
    }

    @Test
    void cacheTypeJustInTime() {
        assertThatIllegalArgumentException().isThrownBy(() -> runCacheType(SelectionCacheType.JUST_IN_TIME, 5));
    }

    public void runCacheType(SelectionCacheType cacheType, int timesCalled) {
        MoveSelector childMoveSelector = SelectorTestUtils.mockMoveSelector(DummyMove.class,
                new DummyMove("jan"), new DummyMove("feb"), new DummyMove("mar"),
                new DummyMove("apr"), new DummyMove("may"), new DummyMove("jun"));

        SelectionSorter<TestdataSolution, DummyMove> sorter = (scoreDirector, selectionList) -> selectionList
                .sort(Comparator.comparing(DummyMove::getCode));
        MoveSelector moveSelector = new SortingMoveSelector(childMoveSelector, cacheType, sorter);

        SolverScope solverScope = mock(SolverScope.class);
        moveSelector.solvingStarted(solverScope);

        AbstractPhaseScope phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        moveSelector.phaseStarted(phaseScopeA);

        AbstractStepScope stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        moveSelector.stepStarted(stepScopeA1);
        assertAllCodesOfMoveSelector(moveSelector, "apr", "feb", "jan", "jun", "mar", "may");
        moveSelector.stepEnded(stepScopeA1);

        AbstractStepScope stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        moveSelector.stepStarted(stepScopeA2);
        assertAllCodesOfMoveSelector(moveSelector, "apr", "feb", "jan", "jun", "mar", "may");
        moveSelector.stepEnded(stepScopeA2);

        moveSelector.phaseEnded(phaseScopeA);

        AbstractPhaseScope phaseScopeB = mock(AbstractPhaseScope.class);
        when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
        moveSelector.phaseStarted(phaseScopeB);

        AbstractStepScope stepScopeB1 = mock(AbstractStepScope.class);
        when(stepScopeB1.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB1);
        assertAllCodesOfMoveSelector(moveSelector, "apr", "feb", "jan", "jun", "mar", "may");
        moveSelector.stepEnded(stepScopeB1);

        AbstractStepScope stepScopeB2 = mock(AbstractStepScope.class);
        when(stepScopeB2.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB2);
        assertAllCodesOfMoveSelector(moveSelector, "apr", "feb", "jan", "jun", "mar", "may");
        moveSelector.stepEnded(stepScopeB2);

        AbstractStepScope stepScopeB3 = mock(AbstractStepScope.class);
        when(stepScopeB3.getPhaseScope()).thenReturn(phaseScopeB);
        moveSelector.stepStarted(stepScopeB3);
        assertAllCodesOfMoveSelector(moveSelector, "apr", "feb", "jan", "jun", "mar", "may");
        moveSelector.stepEnded(stepScopeB3);

        moveSelector.phaseEnded(phaseScopeB);

        moveSelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(childMoveSelector, 1, 2, 5);
        verify(childMoveSelector, times(timesCalled)).iterator();
        verify(childMoveSelector, times(timesCalled)).getSize();
    }

}
