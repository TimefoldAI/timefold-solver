package ai.timefold.solver.core.impl.solver.termination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.withPrecision;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.time.Clock;

import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class UnimprovedTimeMillisSpentTerminationTest {

    @Test
    void forNegativeUnimprovedTimeMillis_exceptionIsThrown() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new UnimprovedTimeMillisSpentTermination<>(-1L))
                .withMessageContaining("cannot be negative");
    }

    @Test
    void solverTermination() {
        SolverScope<TestdataSolution> solverScope = spy(new SolverScope<>());
        AbstractPhaseScope<TestdataSolution> phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        Clock clock = mock(Clock.class);

        Termination<TestdataSolution> termination = new UnimprovedTimeMillisSpentTermination<>(1000L, clock);
        termination.solvingStarted(solverScope);
        termination.phaseStarted(phaseScope);

        doReturn(1000L).when(clock).millis();
        doReturn(500L).when(solverScope).getBestSolutionTimeMillis();
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.5, withPrecision(0.0));

        doReturn(2000L).when(clock).millis();
        doReturn(1000L).when(solverScope).getBestSolutionTimeMillis();
        assertThat(termination.isSolverTerminated(solverScope)).isTrue();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(1.0, withPrecision(0.0));
    }

    @Test
    void phaseTermination() {
        SolverScope<TestdataSolution> solverScope = new SolverScope<>();
        AbstractPhaseScope<TestdataSolution> phaseScope = spy(new LocalSearchPhaseScope<>(solverScope, 0));
        Clock clock = mock(Clock.class);

        Termination<TestdataSolution> termination = new UnimprovedTimeMillisSpentTermination<>(1000L, clock);
        termination.solvingStarted(solverScope);
        termination.phaseStarted(phaseScope);

        doReturn(1000L).when(clock).millis();
        doReturn(500L).when(phaseScope).getPhaseBestSolutionTimeMillis();
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.5, withPrecision(0.0));

        doReturn(2000L).when(clock).millis();
        doReturn(1000L).when(phaseScope).getPhaseBestSolutionTimeMillis();
        assertThat(termination.isPhaseTerminated(phaseScope)).isTrue();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(1.0, withPrecision(0.0));
    }

    @Test
    void solverTerminationWithConstructionHeuristic() { // CH ignores unimproved time spent termination.
        SolverScope<TestdataSolution> solverScope = spy(new SolverScope<>());
        Clock clock = mock(Clock.class);

        Termination<TestdataSolution> termination = new UnimprovedTimeMillisSpentTermination<>(1000L, clock);
        termination.solvingStarted(solverScope);

        AbstractPhaseScope<TestdataSolution> chPhaseScope = new ConstructionHeuristicPhaseScope<>(solverScope, 0);
        termination.phaseStarted(chPhaseScope);

        // During the construction heuristic, the unimproved termination should not trigger.
        doReturn(1000L).when(clock).millis();
        doReturn(0L).when(solverScope).getBestSolutionTimeMillis();
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.0, withPrecision(0.0));

        doReturn(2000L).when(clock).millis();
        doReturn(0L).when(solverScope).getBestSolutionTimeMillis();
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.0, withPrecision(0.0));

        termination.phaseEnded(chPhaseScope);

        AbstractPhaseScope<TestdataSolution> lsPhaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        termination.phaseStarted(lsPhaseScope);

        // When local search starts, the unimproved termination should start triggering,
        // but the start time should act as if reset to zero.
        doReturn(3000L).when(clock).millis();
        doReturn(2500L).when(solverScope).getBestSolutionTimeMillis();
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.5, withPrecision(0.0));

        doReturn(4000L).when(clock).millis();
        doReturn(3000L).when(solverScope).getBestSolutionTimeMillis();
        assertThat(termination.isSolverTerminated(solverScope)).isTrue();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(1.0, withPrecision(0.0));

        termination.phaseEnded(lsPhaseScope);
        termination.solvingEnded(solverScope);
    }

    @Test
    void phaseTerminationWithConstructionHeuristic() { // CH ignores unimproved time spent termination.
        SolverScope<TestdataSolution> solverScope = new SolverScope<>();
        AbstractPhaseScope<TestdataSolution> phaseScope = spy(new ConstructionHeuristicPhaseScope<>(solverScope, 0));
        Clock clock = mock(Clock.class);

        Termination<TestdataSolution> termination = new UnimprovedTimeMillisSpentTermination<>(1000L, clock);
        termination.solvingStarted(solverScope);
        termination.phaseStarted(phaseScope);

        doReturn(1000L).when(clock).millis();
        doReturn(500L).when(phaseScope).getPhaseBestSolutionTimeMillis();
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.0, withPrecision(0.0));

        doReturn(2000L).when(clock).millis();
        doReturn(1000L).when(phaseScope).getPhaseBestSolutionTimeMillis();
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.0, withPrecision(0.0));
    }
}
