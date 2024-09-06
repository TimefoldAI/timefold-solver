package ai.timefold.solver.core.impl.solver.termination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MoveCountTerminationTest {

    @Test
    void phaseTermination() {
        var termination = new MoveCountTermination<TestdataSolution>(4);
        var phaseScope = Mockito.mock(AbstractPhaseScope.class);
        var scoreDirector = Mockito.mock(InnerScoreDirector.class);
        when(phaseScope.getScoreDirector()).thenReturn(scoreDirector);

        when(scoreDirector.getMoveCalculationCount()).thenReturn(0L);
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.0, offset(0.0));
        when(scoreDirector.getMoveCalculationCount()).thenReturn(1L);
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.25, offset(0.0));
        when(scoreDirector.getMoveCalculationCount()).thenReturn(2L);
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.5, offset(0.0));
        when(scoreDirector.getMoveCalculationCount()).thenReturn(3L);
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.75, offset(0.0));
        when(scoreDirector.getMoveCalculationCount()).thenReturn(4L);
        assertThat(termination.isPhaseTerminated(phaseScope)).isTrue();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(1.0, offset(0.0));
        when(scoreDirector.getMoveCalculationCount()).thenReturn(5L);
        assertThat(termination.isPhaseTerminated(phaseScope)).isTrue();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(1.0, offset(0.0));
    }

    @Test
    void solverTermination() {
        var termination = new MoveCountTermination<TestdataSolution>(4);
        var solverScope = Mockito.mock(SolverScope.class);
        var scoreDirector = Mockito.mock(InnerScoreDirector.class);
        when(solverScope.getScoreDirector()).thenReturn(scoreDirector);

        when(scoreDirector.getMoveCalculationCount()).thenReturn(0L);
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.0, offset(0.0));
        when(scoreDirector.getMoveCalculationCount()).thenReturn(1L);
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.25, offset(0.0));
        when(scoreDirector.getMoveCalculationCount()).thenReturn(2L);
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.5, offset(0.0));
        when(scoreDirector.getMoveCalculationCount()).thenReturn(3L);
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.75, offset(0.0));
        when(scoreDirector.getMoveCalculationCount()).thenReturn(4L);
        assertThat(termination.isSolverTerminated(solverScope)).isTrue();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(1.0, offset(0.0));
        when(scoreDirector.getMoveCalculationCount()).thenReturn(5L);
        assertThat(termination.isSolverTerminated(solverScope)).isTrue();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(1.0, offset(0.0));
    }

    @Test
    void invalidTermination() {
        assertThatIllegalArgumentException().isThrownBy(() -> new MoveCountTermination<TestdataSolution>(-1L));
    }
}
