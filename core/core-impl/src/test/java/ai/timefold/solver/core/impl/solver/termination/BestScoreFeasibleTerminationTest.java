package ai.timefold.solver.core.impl.solver.termination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.buildin.HardSoftScoreDefinition;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class BestScoreFeasibleTerminationTest {

    @Test
    void solveTermination() {
        ScoreDefinition scoreDefinition = mock(ScoreDefinition.class);
        when(scoreDefinition.getFeasibleLevelsSize()).thenReturn(1);
        Termination<TestdataSolution> termination = new BestScoreFeasibleTermination<>(scoreDefinition, new double[] {});
        SolverScope<TestdataSolution> solverScope = mock(SolverScope.class);
        when(solverScope.getScoreDefinition()).thenReturn(new HardSoftScoreDefinition());
        when(solverScope.getStartingInitializedScore()).thenReturn(HardSoftScore.of(-100, -100));
        when(solverScope.isBestSolutionInitialized()).thenReturn(true);

        when(solverScope.getBestScore()).thenReturn(HardSoftScore.of(-100, -100));
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.0, offset(0.0));
        when(solverScope.getBestScore()).thenReturn(HardSoftScore.of(-80, -100));
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.2, offset(0.0));
        when(solverScope.getBestScore()).thenReturn(HardSoftScore.of(-60, -100));
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.4, offset(0.0));
        when(solverScope.getBestScore()).thenReturn(HardSoftScore.of(-40, -100));
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.6, offset(0.0));
        when(solverScope.getBestScore()).thenReturn(HardSoftScore.of(-20, -100));
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.8, offset(0.0));
        when(solverScope.getBestScore()).thenReturn(HardSoftScore.of(0, -100));
        assertThat(termination.isSolverTerminated(solverScope)).isTrue();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(1.0, offset(0.0));
    }

    @Test
    void phaseTermination() {
        ScoreDefinition scoreDefinition = mock(ScoreDefinition.class);
        when(scoreDefinition.getFeasibleLevelsSize()).thenReturn(1);
        Termination<TestdataSolution> termination = new BestScoreFeasibleTermination<>(scoreDefinition, new double[] {});
        AbstractPhaseScope<TestdataSolution> phaseScope = mock(AbstractPhaseScope.class);
        when(phaseScope.getStartingScore()).thenReturn(HardSoftScore.of(-100, -100));
        when(phaseScope.isBestSolutionInitialized()).thenReturn(true);

        when(phaseScope.getBestScore()).thenReturn(HardSoftScore.of(-100, -100));
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.0, offset(0.0));
        when(phaseScope.getBestScore()).thenReturn(HardSoftScore.of(-80, -100));
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.2, offset(0.0));
        when(phaseScope.getBestScore()).thenReturn(HardSoftScore.of(-60, -100));
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.4, offset(0.0));
        when(phaseScope.getBestScore()).thenReturn(HardSoftScore.of(-40, -100));
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.6, offset(0.0));
        when(phaseScope.getBestScore()).thenReturn(HardSoftScore.of(-20, -100));
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.8, offset(0.0));
        when(phaseScope.getBestScore()).thenReturn(HardSoftScore.of(0, -100));
        assertThat(termination.isPhaseTerminated(phaseScope)).isTrue();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(1.0, offset(0.0));
    }

    @Test
    void calculateTimeGradientBendableScoreHHSSS() {
        ScoreDefinition scoreDefinition = mock(ScoreDefinition.class);
        when(scoreDefinition.getFeasibleLevelsSize()).thenReturn(2);
        BestScoreFeasibleTermination<TestdataSolution> termination = new BestScoreFeasibleTermination<>(scoreDefinition,
                new double[] { 0.75 });

        // Normal cases
        // Smack in the middle
        assertThat(termination.calculateFeasibilityTimeGradient(
                BendableScore.of(new int[] { -10, -100 }, new int[] { -50, -60, -70 }),
                BendableScore.of(new int[] { -4, -40 }, new int[] { -50, -60, -70 }))).isEqualTo(0.6, offset(0.0));
    }

}
