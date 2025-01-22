package ai.timefold.solver.core.impl.localsearch.decider.reconfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import ai.timefold.solver.core.impl.localsearch.decider.LocalSearchDecider;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.restart.NoOpRestartStrategy;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.junit.jupiter.api.Test;

class ReconfigurationStrategyTest {

    @Test
    void noOp() {
        var strategy = new NoOpRestartStrategy<>();
        assertThat(strategy.isTriggered(mock(LocalSearchMoveScope.class))).isFalse();
    }

    @Test
    void restoreBestSolution() {
        var decider = mock(LocalSearchDecider.class);
        var strategy = new RestoreBestSolutionReconfigurationStrategy<>();

        // Requires the decider
        assertThatThrownBy(() -> strategy.phaseStarted(null)).isInstanceOf(NullPointerException.class);

        // Restore the best solution
        var solverScope = mock(SolverScope.class);
        var phaseScope = mock(LocalSearchPhaseScope.class);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        var stepScope = mock(LocalSearchStepScope.class);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        strategy.setDecider(decider);
        strategy.apply(stepScope);
        // Restore the best solution
        verify(solverScope, times(1)).setWorkingSolutionFromBestSolution();
        // Restart the phase
        verify(decider, times(1)).phaseStarted(any());
    }
}
