package ai.timefold.solver.core.impl.solver.scope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;

import ai.timefold.solver.core.api.solver.phase.PhaseType;
import ai.timefold.solver.core.api.solver.termination.TerminationType;
import ai.timefold.solver.core.impl.phase.DefaultPhaseId;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.termination.DefaultTerminationId;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testutil.MockClock;

import org.junit.jupiter.api.Test;

class SolverScopeTest {

    @Test
    void runStatisticsSumAllSegments() {
        var clock = new MockClock(Clock.systemDefaultZone());
        InnerScoreDirector<TestdataSolution, ?> scoreDirector = mock(InnerScoreDirector.class);
        var solverScope = new SolverScope<TestdataSolution>(clock);
        solverScope.setScoreDirector(scoreDirector);
        var solverStatistics = solverScope.getSolverStatistics();
        solverStatistics.solvingStarted();

        solverScope.startingNow();
        when(scoreDirector.getCalculationCount()).thenReturn(10L);
        solverScope.addChildThreadsScoreCalculationCount(5L);
        solverScope.addMoveEvaluationCount(3L);
        clock.tick(Duration.ofMillis(100));
        solverScope.endingNow();

        solverScope.startingNow();
        when(scoreDirector.getCalculationCount()).thenReturn(7L);
        assertThat(solverScope.getScoreCalculationCount()).isEqualTo(7L); // The child count resets.

        solverScope.addMoveEvaluationCount(2L);
        clock.tick(Duration.ofMillis(50));
        solverScope.endingNow();

        assertThat(solverStatistics.getTimeMillisSpent()).isEqualTo(150L);
        assertThat(solverStatistics.getScoreCalculationCount()).isEqualTo(22L);
        assertThat(solverStatistics.getMoveEvaluationCount()).isEqualTo(5L);

        solverStatistics.solvingStarted();
        assertThat(solverStatistics.getTimeMillisSpent()).isZero();
        assertThat(solverStatistics.getScoreCalculationCount()).isZero();
        assertThat(solverStatistics.getMoveEvaluationCount()).isZero();
    }

    @Test
    void timeToBestSolutionSumsSegments() {
        var clock = new MockClock(Clock.systemDefaultZone());
        InnerScoreDirector<TestdataSolution, ?> scoreDirector = mock(InnerScoreDirector.class);
        var solverScope = new SolverScope<TestdataSolution>(clock);
        solverScope.setScoreDirector(scoreDirector);
        var solverStatistics = solverScope.getSolverStatistics();
        solverStatistics.solvingStarted();

        solverScope.startingNow();
        clock.tick(Duration.ofMillis(30));
        solverScope.setBestSolutionTimeMillis(clock.millis());
        clock.tick(Duration.ofMillis(70));
        solverScope.endingNow();
        assertThat(solverStatistics.getTimeToBestSolutionMillis()).isEqualTo(30L);

        solverScope.startingNow();
        clock.tick(Duration.ofMillis(20));
        solverScope.setBestSolutionTimeMillis(clock.millis());
        clock.tick(Duration.ofMillis(50));
        solverScope.endingNow();
        assertThat(solverStatistics.getTimeToBestSolutionMillis()).isEqualTo(120L);

        solverStatistics.solvingStarted();
        assertThat(solverStatistics.getTimeToBestSolutionMillis()).isZero();
    }

    @Test
    void startingNowClearsTerminationAndPhase() {
        var solverScope = new SolverScope<TestdataSolution>();
        var terminationId = new DefaultTerminationId(TerminationType.STEP_COUNT_LIMIT, null, 0, null);
        solverScope.setLastTerminationId(terminationId);
        solverScope.setLastPhase(new DefaultPhaseId(PhaseType.LOCAL_SEARCH, 1));
        assertThat(solverScope.getLastTerminationId()).isEqualTo(terminationId);
        assertThat(solverScope.getLastPhase()).isEqualTo(new DefaultPhaseId(PhaseType.LOCAL_SEARCH, 1));

        solverScope.startingNow();
        assertThat(solverScope.getLastTerminationId()).isNull();
        assertThat(solverScope.getLastPhase()).isNull();
    }

}
