package ai.timefold.solver.core.impl.solver.termination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.when;

import java.time.Clock;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UnimprovedBestSolutionTerminationTest {

    @Test
    void testTermination() {
        Clock clock = Mockito.mock(Clock.class);
        var currentTime = Clock.systemUTC().millis();
        var termination = new UnimprovedBestSolutionTermination<TestdataSolution>(0.5, 0.4, 10L, clock);
        var solverScope = Mockito.mock(SolverScope.class);
        var phaseScope = Mockito.mock(LocalSearchPhaseScope.class);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(2));
        when(solverScope.getMoveEvaluationSpeed()).thenReturn(1L);
        termination.phaseStarted(phaseScope);
        termination.waitForFirstBestScore = false;

        // Terminate
        termination.currentBest = SimpleScore.of(2);
        termination.initialCurvePointMillis = currentTime;
        termination.lastImprovementMillis = currentTime + 10_000;
        when(clock.millis()).thenReturn(currentTime + 21_000);
        assertThat(termination.isPhaseTerminated(phaseScope)).isTrue();

        // Don't terminate
        termination.terminate = null;
        termination.currentBest = SimpleScore.of(2);
        termination.initialCurvePointMillis = currentTime;
        termination.lastImprovementMillis = currentTime + 10_000;
        when(clock.millis()).thenReturn(currentTime + 14_000);
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();

        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(-1.0);
    }

    @Test
    void testStartNewCurve() {
        Clock clock = Mockito.mock(Clock.class);
        var currentTime = Clock.systemUTC().millis();
        var termination = new UnimprovedBestSolutionTermination<TestdataSolution>(0.5, 0.3, 10L, clock);
        var solverScope = Mockito.mock(SolverScope.class);
        var phaseScope = Mockito.mock(LocalSearchPhaseScope.class);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(2));
        when(solverScope.getMoveEvaluationSpeed()).thenReturn(1L);
        termination.phaseStarted(phaseScope);
        termination.waitForFirstBestScore = false;

        // Adding a new curve
        termination.currentBest = SimpleScore.of(1);
        termination.initialCurvePointMillis = currentTime;
        termination.lastImprovementMillis = currentTime + 10_000;
        when(clock.millis()).thenReturn(currentTime + 14_000);
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.initialCurvePointMillis).isEqualTo(currentTime + 10_000);

        // Not adding a new curve - flat line smaller than the minimum
        termination.terminate = null;
        termination.currentBest = SimpleScore.of(1);
        termination.initialCurvePointMillis = currentTime;
        termination.lastImprovementMillis = currentTime + 10_000;
        when(clock.millis()).thenReturn(currentTime + 11_000);
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.initialCurvePointMillis).isEqualTo(currentTime);

        // Not adding a new curve - flat line larger than the minimum
        termination.terminate = null;
        termination.currentBest = SimpleScore.of(1);
        termination.initialCurvePointMillis = currentTime;
        termination.lastImprovementMillis = currentTime + 10_000;
        when(clock.millis()).thenReturn(currentTime + 16_000);
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.initialCurvePointMillis).isEqualTo(currentTime);
    }

    @Test
    void testMinimalInterval() {
        Clock clock = Mockito.mock(Clock.class);
        var currentTime = Clock.systemUTC().millis();
        var termination = new UnimprovedBestSolutionTermination<TestdataSolution>(0.5, 0.4, 10L, clock);
        var solverScope = Mockito.mock(SolverScope.class);
        var phaseScope = Mockito.mock(LocalSearchPhaseScope.class);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(2));
        when(solverScope.getMoveEvaluationSpeed()).thenReturn(1L);
        termination.phaseStarted(phaseScope);
        termination.waitForFirstBestScore = false;

        // Don't terminate
        termination.currentBest = SimpleScore.of(2);
        termination.initialCurvePointMillis = currentTime;
        termination.lastImprovementMillis = currentTime + 1_000;
        when(clock.millis()).thenReturn(currentTime + 9_000);
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();

        // Terminate
        termination.terminate = null;
        termination.currentBest = SimpleScore.of(2);
        termination.initialCurvePointMillis = currentTime;
        termination.lastImprovementMillis = currentTime + 1_000;
        when(clock.millis()).thenReturn(currentTime + 10_000);
        assertThat(termination.isPhaseTerminated(phaseScope)).isTrue();
    }

    @Test
    void invalidTermination() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new UnimprovedBestSolutionTermination<TestdataSolution>(-1.0, 0.0, 1L));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new UnimprovedBestSolutionTermination<TestdataSolution>(0.0, -1.0, 1L));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new UnimprovedBestSolutionTermination<TestdataSolution>(0.0, 1.0, 1L));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new UnimprovedBestSolutionTermination<TestdataSolution>(1.0, 1.0, 0L));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new UnimprovedBestSolutionTermination<TestdataSolution>(0.1, 1.0, 1L)
                        .calculateSolverTimeGradient(null));
    }
}
