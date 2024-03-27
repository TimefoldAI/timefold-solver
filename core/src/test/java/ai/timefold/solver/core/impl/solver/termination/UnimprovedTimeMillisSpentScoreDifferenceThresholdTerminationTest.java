package ai.timefold.solver.core.impl.solver.termination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.withPrecision;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.Clock;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class UnimprovedTimeMillisSpentScoreDifferenceThresholdTerminationTest {

    private static final long START_TIME_MILLIS = 0L;

    @Test
    void forNegativeUnimprovedTimeMillis_exceptionIsThrown() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new UnimprovedTimeMillisSpentScoreDifferenceThresholdTermination<>(
                        -1L,
                        SimpleScore.of(0)))
                .withMessageContaining("cannot be negative");
    }

    @Test
    void scoreImproves_terminationIsPostponed() {
        var solverScope = spy(new SolverScope<TestdataSolution>());
        var phaseScope = spy(new LocalSearchPhaseScope<>(solverScope));
        var stepScope = spy(new LocalSearchStepScope<>(phaseScope));
        var clock = mock(Clock.class);

        var termination = new UnimprovedTimeMillisSpentScoreDifferenceThresholdTermination<TestdataSolution>(1000L,
                SimpleScore.of(7), clock);

        // first step
        doReturn(START_TIME_MILLIS).when(clock).millis();
        doReturn(START_TIME_MILLIS).when(phaseScope).getStartingSystemTimeMillis();
        doReturn(START_TIME_MILLIS).when(solverScope).getBestSolutionTimeMillis();
        doReturn(true).when(stepScope).getBestScoreImproved();
        doReturn(SimpleScore.ZERO).when(solverScope).getBestScore();

        termination.solvingStarted(solverScope);
        termination.phaseStarted(phaseScope);
        termination.stepEnded(stepScope);

        // time has not yet run out
        doReturn(START_TIME_MILLIS + 500).when(clock).millis();
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.5, withPrecision(0.0));
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.5, withPrecision(0.0));

        // second step - score has improved beyond the threshold => termination is postponed by another second
        doReturn(START_TIME_MILLIS + 500).when(solverScope).getBestSolutionTimeMillis();
        doReturn(SimpleScore.of(10)).when(solverScope).getBestScore();

        termination.stepEnded(stepScope);

        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.0, withPrecision(0.0));
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.0, withPrecision(0.0));

        doReturn(START_TIME_MILLIS + 1500).when(clock).millis();
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(1.0, withPrecision(0.0));
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(1.0, withPrecision(0.0));

        doReturn(START_TIME_MILLIS + 1501).when(clock).millis();
        assertThat(termination.isPhaseTerminated(phaseScope)).isTrue();
        assertThat(termination.isSolverTerminated(solverScope)).isTrue();
    }

    @Test
    void scoreImprovesTooLate_terminates() {
        var solverScope = spy(new SolverScope<TestdataSolution>());
        var phaseScope = spy(new LocalSearchPhaseScope<>(solverScope));
        var stepScope = spy(new LocalSearchStepScope<>(phaseScope));
        var clock = mock(Clock.class);

        var termination = new UnimprovedTimeMillisSpentScoreDifferenceThresholdTermination<TestdataSolution>(1000L,
                SimpleScore.of(7), clock);
        doReturn(solverScope).when(phaseScope).getSolverScope();
        doReturn(phaseScope).when(stepScope).getPhaseScope();

        // first step
        doReturn(START_TIME_MILLIS).when(clock).millis();
        doReturn(START_TIME_MILLIS).when(phaseScope).getStartingSystemTimeMillis();
        doReturn(START_TIME_MILLIS).when(solverScope).getBestSolutionTimeMillis();
        doReturn(true).when(stepScope).getBestScoreImproved();
        doReturn(SimpleScore.ZERO).when(solverScope).getBestScore();

        termination.solvingStarted(solverScope);
        termination.phaseStarted(phaseScope);
        termination.stepEnded(stepScope);

        // time has not yet run out
        doReturn(START_TIME_MILLIS + 500).when(clock).millis();
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.5, withPrecision(0.0));
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.5, withPrecision(0.0));

        // second step - score has improved, but not beyond the threshold
        doReturn(START_TIME_MILLIS + 1000).when(clock).millis();
        doReturn(START_TIME_MILLIS + 1000).when(solverScope).getBestSolutionTimeMillis();
        doReturn(SimpleScore.of(5)).when(solverScope).getBestScore();
        termination.stepEnded(stepScope);

        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(1.0, withPrecision(0.0));
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(1.0, withPrecision(0.0));

        // third step - score has improved beyond the threshold, but too late
        doReturn(START_TIME_MILLIS + 1001).when(clock).millis();
        doReturn(START_TIME_MILLIS + 1001).when(solverScope).getBestSolutionTimeMillis();
        doReturn(SimpleScore.of(10)).when(solverScope).getBestScore();
        termination.stepEnded(stepScope);

        assertThat(termination.isPhaseTerminated(phaseScope)).isTrue();
        assertThat(termination.isSolverTerminated(solverScope)).isTrue();

        termination.phaseEnded(phaseScope);
        termination.solvingEnded(solverScope);
    }
}
