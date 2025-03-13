package ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion;

import static ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion.DiminishedReturnsStuckCriterion.START_TIME_WINDOW_MILLIS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.DiminishedReturnsTermination;

import org.junit.jupiter.api.Test;

class DiminishedReturnsStuckCriterionTest {

    @Test
    void isSolverStuck() {
        var solverScope = mock(SolverScope.class);
        var phaseScope = mock(LocalSearchPhaseScope.class);
        var stepScope = mock(LocalSearchStepScope.class);
        var moveScope = mock(LocalSearchMoveScope.class);
        var termination = mock(DiminishedReturnsTermination.class);

        when(moveScope.getStepScope()).thenReturn(stepScope);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        when(moveScope.getScore()).thenReturn(SimpleScore.of(1));
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(1));
        when(termination.isTerminated(anyLong(), any())).thenReturn(false, true);

        // No restart
        var strategy = new DiminishedReturnsStuckCriterion<>(termination);
        strategy.solvingStarted(null);
        strategy.phaseStarted(phaseScope);
        assertThat(strategy.isSolverStuck(stepScope)).isFalse();

        // First restart
        assertThat(strategy.isSolverStuck(stepScope)).isTrue();
        assertThat(strategy.nextRestart).isEqualTo(2L * START_TIME_WINDOW_MILLIS);

        // Second restart
        assertThat(strategy.isSolverStuck(stepScope)).isTrue();
        assertThat(strategy.nextRestart).isEqualTo(3L * START_TIME_WINDOW_MILLIS);
    }

    @Test
    void reset() {
        var solverScope = mock(SolverScope.class);
        var phaseScope = mock(LocalSearchPhaseScope.class);
        var stepScope = mock(LocalSearchStepScope.class);
        var moveScope = mock(LocalSearchMoveScope.class);
        var termination = mock(DiminishedReturnsTermination.class);

        when(moveScope.getStepScope()).thenReturn(stepScope);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        when(moveScope.getScore()).thenReturn(SimpleScore.of(1));
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(1));
        when(termination.isTerminated(anyLong(), any())).thenReturn(true);

        // Restart
        var strategy = new DiminishedReturnsStuckCriterion<>(termination);
        strategy.solvingStarted(null);
        strategy.phaseStarted(phaseScope);
        assertThat(strategy.isSolverStuck(stepScope)).isTrue();
        assertThat(strategy.nextRestart).isEqualTo(2L * START_TIME_WINDOW_MILLIS);

        // Reset
        strategy.stepStarted(stepScope);
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(2));
        strategy.stepEnded(stepScope);
        assertThat(strategy.nextRestart).isEqualTo(START_TIME_WINDOW_MILLIS);
    }
}
