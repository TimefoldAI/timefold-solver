package ai.timefold.solver.core.impl.localsearch.decider.reconfiguration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import ai.timefold.solver.core.impl.localsearch.decider.LocalSearchDecider;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.junit.jupiter.api.Test;

class RestartStrategyTest {

    @Test
    void restoreBestSolution() {
        // Requires the decider
        var badStrategy = new RestoreBestSolutionRestartStrategy<>();
        assertThatThrownBy(() -> badStrategy.phaseStarted(null)).isInstanceOf(NullPointerException.class);

        // Restore the best solution
        var strategy = new RestoreBestSolutionRestartStrategy<>();

        var decider = mock(LocalSearchDecider.class);
        var solverScope = mock(SolverScope.class);
        var phaseScope = mock(LocalSearchPhaseScope.class);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        when(phaseScope.getDecider()).thenReturn(decider);
        var stepScope = mock(LocalSearchStepScope.class);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        strategy.solvingStarted(solverScope);
        strategy.phaseStarted(phaseScope);
        strategy.applyRestart(stepScope);
        // Restore the best solution
        verify(decider, times(1)).setWorkingSolutionFromBestSolution(any());
    }
}
