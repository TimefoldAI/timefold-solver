package ai.timefold.solver.core.impl.solver.termination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UnimprovedBestSolutionTerminationTest {

    @Test
    void testTermination() {
        var termination = new UnimprovedBestSolutionTermination<TestdataSolution>(0.5);
        var solverScope = Mockito.mock(SolverScope.class);
        var phaseScope = Mockito.mock(LocalSearchPhaseScope.class);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(2));
        when(solverScope.getMoveEvaluationSpeed()).thenReturn(1L);
        termination.phaseStarted(phaseScope);
        termination.waitForFirstBestScore = false;

        // First curve
        termination.currentBest = SimpleScore.of(2);
        termination.initialImprovementMoveCount = 0L;
        termination.lastImprovementMoveCount = 5L;
        when(solverScope.getMoveEvaluationCount()).thenReturn(11L);
        assertThat(termination.isPhaseTerminated(phaseScope)).isTrue();

        // Second curve
        termination.terminate = null;
        termination.currentBest = SimpleScore.of(2);
        termination.initialImprovementMoveCount = 10L;
        termination.lastImprovementMoveCount = 15L;
        when(solverScope.getMoveEvaluationCount()).thenReturn(21L);
        assertThat(termination.isPhaseTerminated(phaseScope)).isTrue();
    }

    @Test
    void testGrowthCurves() {
        var termination = new UnimprovedBestSolutionTermination<TestdataSolution>(0.5);
        var solverScope = Mockito.mock(SolverScope.class);
        var phaseScope = Mockito.mock(LocalSearchPhaseScope.class);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(2));
        when(solverScope.getMoveEvaluationSpeed()).thenReturn(1L);
        termination.phaseStarted(phaseScope);
        termination.waitForFirstBestScore = false;

        // Adding a new curve
        termination.currentBest = SimpleScore.of(1);
        termination.initialImprovementMoveCount = 0L;
        termination.lastImprovementMoveCount = 11L;
        assertThat(termination.initialImprovementMoveCount).isZero();
        when(solverScope.getMoveEvaluationCount()).thenReturn(20L);
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.initialImprovementMoveCount).isEqualTo(11L);

        // Not adding a new curve - flat line smaller than the minimum
        termination.terminate = null;
        termination.currentBest = SimpleScore.of(1);
        termination.initialImprovementMoveCount = 0L;
        termination.lastImprovementMoveCount = 11L;
        assertThat(termination.initialImprovementMoveCount).isZero();
        when(solverScope.getMoveEvaluationCount()).thenReturn(15L);
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.initialImprovementMoveCount).isZero();

        // Not adding a new curve - flat line larger than the minimum
        termination.terminate = null;
        termination.currentBest = SimpleScore.of(1);
        termination.initialImprovementMoveCount = 0L;
        termination.lastImprovementMoveCount = 11L;
        assertThat(termination.initialImprovementMoveCount).isZero();
        when(solverScope.getMoveEvaluationCount()).thenReturn(30L);
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.initialImprovementMoveCount).isZero();
    }

    @Test
    void testMinimalInterval() {
        var termination = new UnimprovedBestSolutionTermination<TestdataSolution>(0.5);
        var solverScope = Mockito.mock(SolverScope.class);
        var phaseScope = Mockito.mock(LocalSearchPhaseScope.class);
        when(phaseScope.getSolverScope()).thenReturn(solverScope);
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(2));
        when(solverScope.getMoveEvaluationSpeed()).thenReturn(1L);
        termination.phaseStarted(phaseScope);

        termination.lastImprovementMoveCount = 7L;
        termination.currentBest = SimpleScore.of(2);
        when(solverScope.getMoveEvaluationCount()).thenReturn(UnimprovedBestSolutionTermination.MINIMAL_INTERVAL_TIME - 1);
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();

        termination.terminate = null;
        when(solverScope.getMoveEvaluationCount()).thenReturn(UnimprovedBestSolutionTermination.MINIMAL_INTERVAL_TIME + 1);
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
    }

    @Test
    void invalidTermination() {
        assertThatIllegalArgumentException().isThrownBy(() -> new UnimprovedBestSolutionTermination<TestdataSolution>(-1));
    }
}
