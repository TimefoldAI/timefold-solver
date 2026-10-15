package ai.timefold.solver.core.impl.solver.scope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;

import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
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

}
