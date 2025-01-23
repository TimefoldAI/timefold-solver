package ai.timefold.solver.core.impl.localsearch.decider.acceptor.restart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UnimprovedMoveCountRestartStrategyTest {

    @Test
    void isTriggered() {
        var clock = mock(Clock.class);
        var solverScope = mock(SolverScope.class);
        var phaseScope = mock(LocalSearchPhaseScope.class);
        var stepScope = mock(LocalSearchStepScope.class);
        var moveScope = mock(LocalSearchMoveScope.class);
        when(moveScope.getStepScope()).thenReturn(stepScope);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        when(clock.millis()).thenReturn(1000L, 11000L);
        when(solverScope.getMoveEvaluationCount()).thenReturn(1000L);
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(1000));

        // Finish grace period
        var strategy = new UnimprovedMoveCountRestartStrategy<>(clock);
        strategy.solvingStarted(null);
        strategy.phaseStarted(phaseScope);
        assertThat(strategy.isTriggered(moveScope)).isFalse();
        assertThat(strategy.lastImprovementMoveCount).isEqualTo(1000L);
        assertThat(strategy.nextRestart).isEqualTo(50000L);

        // First restart
        Mockito.reset(clock);
        var firstCount = 60000L;
        when(solverScope.getMoveEvaluationCount()).thenReturn(firstCount);
        assertThat(strategy.isTriggered(moveScope)).isTrue();
        assertThat(strategy.lastImprovementMoveCount).isEqualTo(firstCount);
        assertThat(strategy.nextRestart).isEqualTo(2L * 50000L);
        strategy.reset(moveScope);
        assertThat(strategy.isTriggered(moveScope)).isFalse();

        // Second restart
        Mockito.reset(clock);
        var secondCount = 2L * 50000L + firstCount;
        when(solverScope.getMoveEvaluationCount()).thenReturn(secondCount);
        assertThat(strategy.isTriggered(moveScope)).isTrue();
        assertThat(strategy.lastImprovementMoveCount).isEqualTo(secondCount);
        assertThat(strategy.nextRestart).isEqualTo(3L * 50000L);
        strategy.reset(moveScope);
        assertThat(strategy.isTriggered(moveScope)).isFalse();

        // Third restart
        var thirdCount = 3L * 50000L + secondCount;
        Mockito.reset(clock);
        when(solverScope.getMoveEvaluationCount()).thenReturn(thirdCount);
        assertThat(strategy.isTriggered(moveScope)).isTrue();
        assertThat(strategy.lastImprovementMoveCount).isEqualTo(thirdCount);
        assertThat(strategy.nextRestart).isEqualTo(5L * 50000L);
        strategy.reset(moveScope);
    }

    @Test
    void updateBestSolution() {
        var clock = mock(Clock.class);
        var solverScope = mock(SolverScope.class);
        var phaseScope = mock(LocalSearchPhaseScope.class);
        var stepScope = mock(LocalSearchStepScope.class);
        var moveScope = mock(LocalSearchMoveScope.class);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        when(moveScope.getStepScope()).thenReturn(stepScope);
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(1000));
        when(stepScope.getScore()).thenReturn(SimpleScore.of(2000));
        when(clock.millis()).thenReturn(1000L, 20000L, 20000L, 20001L);
        when(solverScope.getMoveEvaluationCount()).thenReturn(1000L, 1001L);

        var strategy = new UnimprovedMoveCountRestartStrategy<>(clock);
        strategy.solvingStarted(mock(SolverScope.class));
        strategy.phaseStarted(phaseScope);
        strategy.stepStarted(stepScope);
        // Trigger
        strategy.isTriggered(moveScope);
        // Update the last improvement
        strategy.stepEnded(stepScope);
        assertThat(strategy.lastImprovementMoveCount).isEqualTo(1001L);
    }

    @Test
    void reset() {
        var clock = mock(Clock.class);
        var solverScope = mock(SolverScope.class);
        var phaseScope = mock(LocalSearchPhaseScope.class);
        var stepScope = mock(LocalSearchStepScope.class);
        var moveScope = mock(LocalSearchMoveScope.class);
        when(moveScope.getStepScope()).thenReturn(stepScope);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        when(clock.millis()).thenReturn(1000L, 11000L);
        when(solverScope.getMoveEvaluationCount()).thenReturn(1000L);

        var strategy = new UnimprovedMoveCountRestartStrategy<>(clock);
        strategy.solvingStarted(mock(SolverScope.class));
        strategy.phaseStarted(mock(LocalSearchPhaseScope.class));
        // Trigger
        strategy.isTriggered(moveScope);
        // Reset
        strategy.reset(moveScope);
        assertThat(strategy.lastImprovementMoveCount).isEqualTo(1000L);
    }
}
