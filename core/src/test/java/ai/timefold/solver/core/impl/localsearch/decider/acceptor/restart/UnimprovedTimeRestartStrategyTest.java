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

class UnimprovedTimeRestartStrategyTest {

    @Test
    void isTriggered() {
        var clock = mock(Clock.class);
        when(clock.millis()).thenReturn(1000L);
        var phaseScope = mock(LocalSearchPhaseScope.class);
        var stepScope = mock(LocalSearchStepScope.class);
        var moveScope = mock(LocalSearchMoveScope.class);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        when(moveScope.getStepScope()).thenReturn(stepScope);
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(1000));

        // Initial values
        var strategy = new UnimprovedTimeRestartStrategy<>(clock);
        strategy.solvingStarted(null);
        strategy.phaseStarted(phaseScope);
        assertThat(strategy.lastImprovementMillis).isZero();
        assertThat(strategy.nextRestart).isEqualTo(1000L);

        // Finish grace period
        Mockito.reset(clock);
        when(clock.millis()).thenReturn(12000L);
        assertThat(strategy.isTriggered(moveScope)).isFalse();
        assertThat(strategy.lastImprovementMillis).isEqualTo(12000L);
        assertThat(strategy.nextRestart).isEqualTo(1000L);
        strategy.reset(moveScope);
        assertThat(strategy.isTriggered(moveScope)).isFalse();

        // First restart
        Mockito.reset(clock);
        when(clock.millis()).thenReturn(13000L);
        assertThat(strategy.isTriggered(moveScope)).isTrue();
        assertThat(strategy.lastImprovementMillis).isEqualTo(13000L);
        assertThat(strategy.nextRestart).isEqualTo(2000L);
        strategy.reset(moveScope);
        assertThat(strategy.isTriggered(moveScope)).isFalse();

        // Second restart
        Mockito.reset(clock);
        when(clock.millis()).thenReturn(15000L);
        assertThat(strategy.isTriggered(moveScope)).isTrue();
        assertThat(strategy.lastImprovementMillis).isEqualTo(15000L);
        assertThat(strategy.nextRestart).isEqualTo(3000L);
        strategy.reset(moveScope);

        // Third restart
        Mockito.reset(clock);
        when(clock.millis()).thenReturn(18000L);
        assertThat(strategy.isTriggered(moveScope)).isTrue();
        assertThat(strategy.lastImprovementMillis).isEqualTo(18000L);
        assertThat(strategy.nextRestart).isEqualTo(5000L);
        strategy.reset(moveScope);
    }

    @Test
    void updateBestSolution() {
        var clock = mock(Clock.class);
        when(clock.millis()).thenReturn(1000L, 20000L, 20000L, 20001L);
        var phaseScope = mock(LocalSearchPhaseScope.class);
        var stepScope = mock(LocalSearchStepScope.class);
        var moveScope = mock(LocalSearchMoveScope.class);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        when(moveScope.getStepScope()).thenReturn(stepScope);
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(1000));
        when(stepScope.getScore()).thenReturn(SimpleScore.of(2000));

        var strategy = new UnimprovedTimeRestartStrategy<>(clock);
        strategy.solvingStarted(mock(SolverScope.class));
        strategy.phaseStarted(phaseScope);
        strategy.stepStarted(stepScope);
        // Trigger
        strategy.isTriggered(moveScope);
        // Update the last improvement
        strategy.stepEnded(stepScope);
        assertThat(strategy.lastImprovementMillis).isEqualTo(20001L);
    }

    @Test
    void reset() {
        var clock = mock(Clock.class);
        var moveScope = mock(LocalSearchMoveScope.class);
        when(clock.millis()).thenReturn(1000L, 11000L);

        var strategy = new UnimprovedTimeRestartStrategy<>(clock);
        strategy.solvingStarted(mock(SolverScope.class));
        strategy.phaseStarted(mock(LocalSearchPhaseScope.class));
        // Trigger
        strategy.isTriggered(moveScope);
        // Reset
        strategy.reset(moveScope);
        assertThat(strategy.lastImprovementMillis).isEqualTo(11000L);
    }
}
