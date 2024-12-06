package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

public class LateAcceptanceAcceptor<Solution_> extends AbstractAcceptor<Solution_> {

    protected int lateAcceptanceSize = -1;
    protected boolean hillClimbingEnabled = true;

    protected Score<?>[] previousScores;
    protected int lateScoreIndex = -1;

    public void setLateAcceptanceSize(int lateAcceptanceSize) {
        this.lateAcceptanceSize = lateAcceptanceSize;
    }

    public void setHillClimbingEnabled(boolean hillClimbingEnabled) {
        this.hillClimbingEnabled = hillClimbingEnabled;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        validate();
        previousScores = new Score[lateAcceptanceSize];
        var initialScore = phaseScope.getBestScore();
        Arrays.fill(previousScores, initialScore);
        lateScoreIndex = 0;
    }

    private void validate() {
        if (lateAcceptanceSize <= 0) {
            throw new IllegalArgumentException(
                    "The lateAcceptanceSize (%d) cannot be negative or zero.".formatted(lateAcceptanceSize));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        var moveScore = moveScope.getScore();
        var lateScore = previousScores[lateScoreIndex];
        if (moveScore.compareTo(lateScore) >= 0) {
            return true;
        }
        if (hillClimbingEnabled) {
            var lastStepScore = moveScope.getStepScope().getPhaseScope().getLastCompletedStepScope().getScore();
            return moveScore.compareTo(lastStepScore) >= 0;
        }
        return false;
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        previousScores[lateScoreIndex] = stepScope.getScore();
        lateScoreIndex = (lateScoreIndex + 1) % lateAcceptanceSize;
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        previousScores = null;
        lateScoreIndex = -1;
    }

}
