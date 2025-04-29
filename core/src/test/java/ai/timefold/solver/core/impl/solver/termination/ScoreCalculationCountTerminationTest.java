package ai.timefold.solver.core.impl.solver.termination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class ScoreCalculationCountTerminationTest {

    @Test
    void solveTermination() {
        SolverTermination<TestdataSolution> termination = new ScoreCalculationCountTermination<>(1000L);
        SolverScope<TestdataSolution> solverScope = mock(SolverScope.class);
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector = mock(InnerScoreDirector.class);
        doReturn(scoreDirector).when(solverScope).getScoreDirector();

        when(scoreDirector.getCalculationCount()).thenReturn(0L);
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.0, offset(0.0));
        when(scoreDirector.getCalculationCount()).thenReturn(100L);
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.1, offset(0.0));
        when(scoreDirector.getCalculationCount()).thenReturn(500L);
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.5, offset(0.0));
        when(scoreDirector.getCalculationCount()).thenReturn(700L);
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.7, offset(0.0));
        when(scoreDirector.getCalculationCount()).thenReturn(1000L);
        assertThat(termination.isSolverTerminated(solverScope)).isTrue();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(1.0, offset(0.0));
        when(scoreDirector.getCalculationCount()).thenReturn(1200L);
        assertThat(termination.isSolverTerminated(solverScope)).isTrue();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(1.0, offset(0.0));
    }

    @Test
    void phaseTermination() {
        PhaseTermination<TestdataSolution> termination = new ScoreCalculationCountTermination<>(1000L);
        AbstractPhaseScope<TestdataSolution> phaseScope = mock(AbstractPhaseScope.class);
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector = mock(InnerScoreDirector.class);
        doReturn(scoreDirector).when(phaseScope).getScoreDirector();

        when(scoreDirector.getCalculationCount()).thenReturn(0L);
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.0, offset(0.0));
        when(scoreDirector.getCalculationCount()).thenReturn(100L);
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.1, offset(0.0));
        when(scoreDirector.getCalculationCount()).thenReturn(500L);
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.5, offset(0.0));
        when(scoreDirector.getCalculationCount()).thenReturn(700L);
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.7, offset(0.0));
        when(scoreDirector.getCalculationCount()).thenReturn(1000L);
        assertThat(termination.isPhaseTerminated(phaseScope)).isTrue();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(1.0, offset(0.0));
        when(scoreDirector.getCalculationCount()).thenReturn(1200L);
        assertThat(termination.isPhaseTerminated(phaseScope)).isTrue();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(1.0, offset(0.0));
    }

}
