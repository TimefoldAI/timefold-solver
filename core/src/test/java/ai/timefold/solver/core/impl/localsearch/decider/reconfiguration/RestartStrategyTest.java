package ai.timefold.solver.core.impl.localsearch.decider.reconfiguration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.Acceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.junit.jupiter.api.Test;

class RestartStrategyTest {

    @Test
    void restoreBestSolution() {
        // Requires the decider
        var badStrategy = new RestoreBestSolutionRestartStrategy<>(null, null);
        assertThatThrownBy(() -> badStrategy.phaseStarted(null)).isInstanceOf(NullPointerException.class);

        // Restore the best solution
        var moveSelector = mock(MoveSelector.class);
        var acceptor = mock(Acceptor.class);
        var strategy = new RestoreBestSolutionRestartStrategy<>(moveSelector, acceptor);

        var solverScope = mock(SolverScope.class);
        var phaseScope = mock(LocalSearchPhaseScope.class);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        var stepScope = mock(LocalSearchStepScope.class);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        strategy.applyRestart(stepScope);
        // Restore the best solution
        verify(solverScope, times(1)).setWorkingSolutionFromBestSolution();
        // Restart the phase
        verify(phaseScope, times(1)).resetSolverStuck();
        verify(moveSelector, times(1)).phaseStarted(any());
        verify(acceptor, times(1)).phaseStarted(any());
    }
}
