package ai.timefold.solver.core.impl.localsearch.decider.acceptor.restart;

import static ai.timefold.solver.core.impl.localsearch.decider.acceptor.restart.UnimprovedMoveCountStuckCriterion.UNIMPROVED_MOVE_COUNT_MULTIPLIER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UnimprovedMoveCountStuckCriterionTest {

    @Test
    void isSolverStuck() {
        var clock = mock(Clock.class);
        var instant = mock(Instant.class);
        var solverScope = mock(SolverScope.class);
        var phaseScope = mock(LocalSearchPhaseScope.class);
        var stepScope = mock(LocalSearchStepScope.class);
        var moveScope = mock(LocalSearchMoveScope.class);
        when(clock.instant()).thenReturn(instant);
        when(moveScope.getStepScope()).thenReturn(stepScope);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        when(instant.toEpochMilli()).thenReturn(1000L, UNIMPROVED_MOVE_COUNT_MULTIPLIER + 1000L);
        when(solverScope.getMoveEvaluationCount()).thenReturn(1000L);
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(1000));

        // Finish grace period
        var strategy = new UnimprovedMoveCountStuckCriterion<>(clock);
        strategy.solvingStarted(null);
        strategy.phaseStarted(phaseScope);
        assertThat(strategy.isSolverStuck(moveScope)).isFalse();
        assertThat(strategy.lastCheckpoint).isEqualTo(1000L);
        assertThat(strategy.nextRestart).isEqualTo(UNIMPROVED_MOVE_COUNT_MULTIPLIER);

        // First restart
        Mockito.reset(instant);
        var firstCount = UNIMPROVED_MOVE_COUNT_MULTIPLIER + 1000L;
        when(solverScope.getMoveEvaluationCount()).thenReturn(firstCount);
        assertThat(strategy.isSolverStuck(moveScope)).isTrue();
        assertThat(strategy.lastCheckpoint).isEqualTo(firstCount);
        assertThat(strategy.nextRestart).isEqualTo(2L * UNIMPROVED_MOVE_COUNT_MULTIPLIER);
        assertThat(strategy.isSolverStuck(moveScope)).isFalse();

        // Second restart
        Mockito.reset(instant);
        var secondCount = 2L * UNIMPROVED_MOVE_COUNT_MULTIPLIER + firstCount + 1;
        when(solverScope.getMoveEvaluationCount()).thenReturn(secondCount);
        assertThat(strategy.isSolverStuck(moveScope)).isTrue();
        assertThat(strategy.lastCheckpoint).isEqualTo(secondCount);
        assertThat(strategy.nextRestart).isEqualTo(3L * UNIMPROVED_MOVE_COUNT_MULTIPLIER);
        assertThat(strategy.isSolverStuck(moveScope)).isFalse();

        // Third restart
        var thirdCount = 3L * UNIMPROVED_MOVE_COUNT_MULTIPLIER + secondCount + 1;
        Mockito.reset(instant);
        when(solverScope.getMoveEvaluationCount()).thenReturn(thirdCount);
        assertThat(strategy.isSolverStuck(moveScope)).isTrue();
        assertThat(strategy.lastCheckpoint).isEqualTo(thirdCount);
        assertThat(strategy.nextRestart).isEqualTo(5L * UNIMPROVED_MOVE_COUNT_MULTIPLIER);
    }

    @Test
    void updateBestSolution() {
        var clock = mock(Clock.class);
        var instant = mock(Instant.class);
        var solverScope = mock(SolverScope.class);
        var phaseScope = mock(LocalSearchPhaseScope.class);
        var stepScope = mock(LocalSearchStepScope.class);
        var moveScope = mock(LocalSearchMoveScope.class);
        when(clock.instant()).thenReturn(instant);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        when(moveScope.getStepScope()).thenReturn(stepScope);
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(1000));
        when(stepScope.getScore()).thenReturn(SimpleScore.of(2000));
        when(instant.toEpochMilli()).thenReturn(1000L, UNIMPROVED_MOVE_COUNT_MULTIPLIER, UNIMPROVED_MOVE_COUNT_MULTIPLIER,
                UNIMPROVED_MOVE_COUNT_MULTIPLIER + 1);
        when(solverScope.getMoveEvaluationCount()).thenReturn(1000L, 1001L);

        var strategy = new UnimprovedMoveCountStuckCriterion<>(clock);
        strategy.solvingStarted(mock(SolverScope.class));
        strategy.phaseStarted(phaseScope);
        strategy.stepStarted(stepScope);
        // Trigger
        strategy.isSolverStuck(moveScope);
        // Update the last improvement
        strategy.stepEnded(stepScope);
        assertThat(strategy.lastCheckpoint).isEqualTo(1001L);
    }

    @Test
    void reset() {
        var clock = mock(Clock.class);
        var instant = mock(Instant.class);
        var solverScope = mock(SolverScope.class);
        var phaseScope = mock(LocalSearchPhaseScope.class);
        var stepScope = mock(LocalSearchStepScope.class);
        var moveScope = mock(LocalSearchMoveScope.class);
        when(clock.instant()).thenReturn(instant);
        when(moveScope.getStepScope()).thenReturn(stepScope);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        when(instant.toEpochMilli()).thenReturn(1000L, UNIMPROVED_MOVE_COUNT_MULTIPLIER + 1000L);
        when(solverScope.getMoveEvaluationCount()).thenReturn(1000L);

        var strategy = new UnimprovedMoveCountStuckCriterion<>(clock);
        strategy.solvingStarted(mock(SolverScope.class));
        strategy.phaseStarted(mock(LocalSearchPhaseScope.class));
        // Trigger
        strategy.isSolverStuck(moveScope);
        assertThat(strategy.lastCheckpoint).isEqualTo(1000L);
    }
}
