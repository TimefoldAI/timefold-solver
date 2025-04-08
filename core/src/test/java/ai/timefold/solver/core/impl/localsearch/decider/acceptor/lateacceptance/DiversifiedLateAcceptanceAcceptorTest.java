package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptorTest;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.junit.jupiter.api.Test;

class DiversifiedLateAcceptanceAcceptorTest extends AbstractAcceptorTest {

    @Test
    void acceptanceCriterion() {
        var acceptor = new DiversifiedLateAcceptanceAcceptor<>();
        acceptor.setLateAcceptanceSize(3);

        var solverScope = new SolverScope<>();
        solverScope.setInitializedBestScore(SimpleScore.of(-1000));
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        acceptor.phaseStarted(phaseScope);

        // Equal to the current solution
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        stepScope0.setInitializedScore(SimpleScore.of(-1000));
        var moveScope0 = buildMoveScope(stepScope0, -1000);
        stepScope0.getPhaseScope().setLastCompletedStepScope(stepScope0);
        assertThat(acceptor.isAccepted(moveScope0)).isTrue();

        // Better than the current best late element
        moveScope0 = buildMoveScope(stepScope0, -999);
        assertThat(acceptor.isAccepted(moveScope0)).isTrue();

        // Recompute lateWorse and the number of occurrences
        acceptor.phaseStarted(phaseScope);
        moveScope0 = buildMoveScope(stepScope0, -2000);
        stepScope0.setScore(moveScope0.getScore());
        acceptor.lateWorseScore = InnerScore.fullyAssigned(SimpleScore.of(-2001));
        acceptor.lateWorseOccurrences = 1;
        acceptor.previousScores[0] = InnerScore.fullyAssigned(SimpleScore.of(-2001));
        acceptor.previousScores[1] = InnerScore.fullyAssigned(SimpleScore.of(-2001));
        acceptor.previousScores[2] = InnerScore.fullyAssigned(SimpleScore.of(-2000));
        stepScope0.getPhaseScope().getLastCompletedStepScope().setInitializedScore(SimpleScore.of(-2001));
        acceptor.isAccepted(moveScope0);
        assertThat(acceptor.previousScores[0]).isEqualTo(InnerScore.fullyAssigned(SimpleScore.of(-2000)));
        assertThat(acceptor.lateWorseScore).isEqualTo(InnerScore.fullyAssigned(SimpleScore.of(-2001)));
        assertThat(acceptor.lateWorseOccurrences).isEqualTo(1);
    }

    @Test
    void replacementCriterion() {
        var acceptor = new DiversifiedLateAcceptanceAcceptor<>();
        acceptor.setLateAcceptanceSize(3);

        var solverScope = new SolverScope<>();
        solverScope.setInitializedBestScore(SimpleScore.of(-1000));
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        acceptor.phaseStarted(phaseScope);
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        stepScope0.setInitializedScore(SimpleScore.of(-1000));

        // Current worse than late score and late score worse than previous
        acceptor.phaseStarted(phaseScope);
        acceptor.lateWorseScore = InnerScore.fullyAssigned(SimpleScore.of(-2005));
        var moveScope0 = buildMoveScope(stepScope0, -2000);
        stepScope0.setScore(moveScope0.getScore());
        acceptor.previousScores[0] = InnerScore.fullyAssigned(SimpleScore.of(-1999));
        stepScope0.getPhaseScope().getLastCompletedStepScope().setInitializedScore(SimpleScore.of(-1998));
        acceptor.isAccepted(moveScope0);
        assertThat(acceptor.previousScores[0]).isEqualTo(InnerScore.fullyAssigned(SimpleScore.of(-2000)));

        // Current worse than late score and late score better than late score
        acceptor.phaseStarted(phaseScope);
        acceptor.lateWorseScore = InnerScore.fullyAssigned(SimpleScore.of(-2005));
        moveScope0 = buildMoveScope(stepScope0, -2001);
        stepScope0.setScore(moveScope0.getScore());
        acceptor.previousScores[0] = InnerScore.fullyAssigned(SimpleScore.of(-1999));
        stepScope0.getPhaseScope().getLastCompletedStepScope().setInitializedScore(SimpleScore.of(-2000));
        acceptor.isAccepted(moveScope0);
        assertThat(acceptor.previousScores[0]).isEqualTo(InnerScore.fullyAssigned(SimpleScore.of(-2001)));

        // Current equal to previous and current worse than late score
        acceptor.phaseStarted(phaseScope);
        acceptor.lateWorseScore = InnerScore.fullyAssigned(SimpleScore.of(-2005));
        moveScope0 = buildMoveScope(stepScope0, -2001);
        stepScope0.setScore(moveScope0.getScore());
        acceptor.previousScores[0] = InnerScore.fullyAssigned(SimpleScore.of(-1999));
        stepScope0.getPhaseScope().getLastCompletedStepScope().setInitializedScore(SimpleScore.of(-2001));
        acceptor.isAccepted(moveScope0);
        assertThat(acceptor.previousScores[0]).isEqualTo(InnerScore.fullyAssigned(SimpleScore.of(-2001)));

        // Current better than previous and previous better than late score
        acceptor.phaseStarted(phaseScope);
        acceptor.lateWorseScore = InnerScore.fullyAssigned(SimpleScore.of(-2005));
        moveScope0 = buildMoveScope(stepScope0, -1998);
        stepScope0.setScore(moveScope0.getScore());
        acceptor.previousScores[0] = InnerScore.fullyAssigned(SimpleScore.of(-2000));
        stepScope0.getPhaseScope().getLastCompletedStepScope().setInitializedScore(SimpleScore.of(-1999));
        acceptor.isAccepted(moveScope0);
        assertThat(acceptor.previousScores[0]).isEqualTo(InnerScore.fullyAssigned(SimpleScore.of(-1998)));

        // Current better than previous and previous worse than late score
        acceptor.phaseStarted(phaseScope);
        acceptor.lateWorseScore = InnerScore.fullyAssigned(SimpleScore.of(-2005));
        moveScope0 = buildMoveScope(stepScope0, -1998);
        stepScope0.setScore(moveScope0.getScore());
        acceptor.previousScores[0] = InnerScore.fullyAssigned(SimpleScore.of(-1999));
        stepScope0.getPhaseScope().getLastCompletedStepScope().setInitializedScore(SimpleScore.of(-2000));
        acceptor.isAccepted(moveScope0);
        assertThat(acceptor.previousScores[0]).isEqualTo(InnerScore.fullyAssigned(SimpleScore.of(-1998)));

        // Current worse than late score and previous worse than the current
        acceptor.phaseStarted(phaseScope);
        acceptor.lateWorseScore = InnerScore.fullyAssigned(SimpleScore.of(-2005));
        moveScope0 = buildMoveScope(stepScope0, -2000);
        stepScope0.setScore(moveScope0.getScore());
        acceptor.previousScores[0] = InnerScore.fullyAssigned(SimpleScore.of(-1999));
        stepScope0.getPhaseScope().getLastCompletedStepScope().setInitializedScore(SimpleScore.of(-2001));
        acceptor.isAccepted(moveScope0);
        assertThat(acceptor.previousScores[0]).isEqualTo(InnerScore.fullyAssigned(SimpleScore.of(-2000)));

        // No replacement
        // Current better than late score and previous better than current
        acceptor.phaseStarted(phaseScope);
        acceptor.lateWorseScore = InnerScore.fullyAssigned(SimpleScore.of(-2005));
        moveScope0 = buildMoveScope(stepScope0, -2000);
        stepScope0.setScore(moveScope0.getScore());
        acceptor.previousScores[0] = InnerScore.fullyAssigned(SimpleScore.of(-2001));
        stepScope0.getPhaseScope().getLastCompletedStepScope().setInitializedScore(SimpleScore.of(-1999));
        acceptor.isAccepted(moveScope0);
        assertThat(acceptor.previousScores[0]).isEqualTo(InnerScore.fullyAssigned(SimpleScore.of(-2001)));

        // Current equal to previous and current better than late score
        acceptor.phaseStarted(phaseScope);
        acceptor.lateWorseScore = InnerScore.fullyAssigned(SimpleScore.of(-2005));
        moveScope0 = buildMoveScope(stepScope0, -2000);
        stepScope0.setScore(moveScope0.getScore());
        acceptor.previousScores[0] = InnerScore.fullyAssigned(SimpleScore.of(-2001));
        stepScope0.getPhaseScope().getLastCompletedStepScope().setInitializedScore(SimpleScore.of(-2000));
        acceptor.isAccepted(moveScope0);
        assertThat(acceptor.previousScores[0]).isEqualTo(InnerScore.fullyAssigned(SimpleScore.of(-2001)));
    }
}
