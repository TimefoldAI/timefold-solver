package ai.timefold.solver.core.impl.localsearch.decider.acceptor.restart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UnimprovedTimeGeometricRestartStrategyTest {

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
        var strategy = new UnimprovedTimeGeometricRestartStrategy<>(clock);
        strategy.phaseStarted(phaseScope);
        assertThat(strategy.lastImprovementMillis).isEqualTo(1000L);
        assertThat(strategy.nextRestart).isEqualTo(1000L);
        assertThat(strategy.restartTriggered).isFalse();

        // First restart
        Mockito.reset(clock);
        when(clock.millis()).thenReturn(2000L);
        assertThat(strategy.isTriggered(moveScope)).isTrue();
        assertThat(strategy.lastImprovementMillis).isEqualTo(2000L);
        assertThat(strategy.nextRestart).isEqualTo(1000L);
        assertThat(strategy.restartTriggered).isTrue();
        strategy.reset();
        assertThat(strategy.isTriggered(moveScope)).isFalse();

        // Second restart
        Mockito.reset(clock);
        when(clock.millis()).thenReturn(3000L);
        assertThat(strategy.isTriggered(moveScope)).isTrue();
        assertThat(strategy.lastImprovementMillis).isEqualTo(3000L);
        assertThat(strategy.nextRestart).isEqualTo(2000L);
        assertThat(strategy.restartTriggered).isTrue();
        strategy.reset();

        // Third restart
        Mockito.reset(clock);
        when(clock.millis()).thenReturn(5000L);
        assertThat(strategy.isTriggered(moveScope)).isTrue();
        assertThat(strategy.lastImprovementMillis).isEqualTo(5000L);
        assertThat(strategy.nextRestart).isEqualTo(3000L);
        assertThat(strategy.restartTriggered).isTrue();
        strategy.reset();
    }

    @Test
    void updateBestSolution() {
        var clock = mock(Clock.class);
        when(clock.millis()).thenReturn(1000L, 2000L);
        var phaseScope = mock(LocalSearchPhaseScope.class);
        var stepScope = mock(LocalSearchStepScope.class);
        var moveScope = mock(LocalSearchMoveScope.class);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        when(moveScope.getStepScope()).thenReturn(stepScope);
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(1000));
        when(stepScope.getScore()).thenReturn(SimpleScore.of(2000));

        var strategy = new UnimprovedTimeGeometricRestartStrategy<>(clock);
        strategy.phaseStarted(phaseScope);
        strategy.stepStarted(stepScope);
        strategy.stepEnded(stepScope);
        assertThat(strategy.lastImprovementMillis).isEqualTo(2000L);
    }

    @Test
    void reset() {
        var clock = mock(Clock.class);
        when(clock.millis()).thenReturn(1L);
        var strategy = new UnimprovedTimeGeometricRestartStrategy<>(clock);
        strategy.reset();
        assertThat(strategy.lastImprovementMillis).isEqualTo(1L);
        assertThat(strategy.restartTriggered).isFalse();
    }
}
