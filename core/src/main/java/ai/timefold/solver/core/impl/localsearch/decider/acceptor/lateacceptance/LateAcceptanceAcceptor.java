package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.termination.MoveCountTermination;

public class LateAcceptanceAcceptor<Solution_> extends AbstractAcceptor<Solution_> {

    protected int lateAcceptanceSize = -1;
    protected boolean hillClimbingEnabled = true;

    protected Score<?>[] previousScores;
    protected int lateScoreIndex = -1;

    protected Double moveCountLimitPercentage;
    protected Double moveReconfigurationRatio;
    protected long lateAcceptanceReconfigurationRatioCount;
    protected MoveCountTermination<Solution_> moveCountTermination;

    public void setLateAcceptanceSize(int lateAcceptanceSize) {
        this.lateAcceptanceSize = lateAcceptanceSize;
    }

    public void setHillClimbingEnabled(boolean hillClimbingEnabled) {
        this.hillClimbingEnabled = hillClimbingEnabled;
    }

    public void setMoveCountLimitPercentage(Double moveCountLimitPercentage) {
        this.moveCountLimitPercentage = moveCountLimitPercentage;
    }

    public void setMoveReconfigurationRatio(Double moveReconfigurationRatio) {
        this.moveReconfigurationRatio = moveReconfigurationRatio;
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
        lateAcceptanceReconfigurationRatioCount = (long) (phaseScope.getMoveSelectorSize() * moveReconfigurationRatio / 100);
        var lateAcceptanceReconfigurationMoveCount = (long) (phaseScope.getMoveSelectorSize() * moveCountLimitPercentage / 100);
        moveCountTermination = new MoveCountTermination<>(lateAcceptanceReconfigurationMoveCount, true);
        moveCountTermination.phaseStarted(phaseScope);
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        moveCountTermination.stepStarted(stepScope);
    }

    private void validate() {
        if (lateAcceptanceSize <= 0) {
            throw new IllegalArgumentException("The lateAcceptanceSize (" + lateAcceptanceSize
                    + ") cannot be negative or zero.");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        var moveScore = moveScope.getScore();
        var lateScore = previousScores[lateScoreIndex];
        if (lateScore == null) {
            logger.trace("Move index ({}), score ({}), accepted after reconfiguration ({}).", moveScope.getMoveIndex(),
                    moveScope.getScore(), moveScope.getMove());
            return true;
        }
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
        moveCountTermination = null;
        lateScoreIndex = -1;
    }

    @Override
    public boolean needReconfiguration(LocalSearchStepScope<Solution_> stepScope) {
        return moveCountTermination.isSolverTerminated(stepScope.getPhaseScope().getSolverScope());
    }

    @Override
    public void applyReconfiguration() {
        var idx = lateScoreIndex;
        if (previousScores[idx] == null) {
            // The method still has null values from the last reconfiguration,
            // and we don't need to apply any reconfiguration
            return;
        }
        for (var i = 0; i < lateAcceptanceReconfigurationRatioCount; i++) {
            previousScores[idx] = null;
            idx = (idx + 1) % lateAcceptanceSize;
        }
    }
}
