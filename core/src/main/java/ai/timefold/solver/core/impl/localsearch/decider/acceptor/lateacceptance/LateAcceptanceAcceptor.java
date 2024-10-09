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
    // max number of solutions that can be accepted
    protected long lateAcceptanceReconfigurationSize;
    // current number of accepted solutions
    protected long currentReconfigurationRationCount = 1;
    // max number of moves evaluated before triggering the reconfiguration
    protected long maxReconfigurationMoveCount;
    protected Score<?> lastAcceptedScore = null;
    // move termination that triggers the reconfiguration when it is terminated 
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

    public void setLateAcceptanceReconfigurationSize(long lateAcceptanceReconfigurationSize) {
        this.lateAcceptanceReconfigurationSize = lateAcceptanceReconfigurationSize;
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
        currentReconfigurationRationCount = 1;
        maxReconfigurationMoveCount = (long) (phaseScope.getMoveSelectorSize() * moveCountLimitPercentage / 100);
        moveCountTermination = new MoveCountTermination<>(maxReconfigurationMoveCount, true);
        moveCountTermination.phaseStarted(phaseScope);
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
            logger.info("Reconfiguration accepted move index ({}), moves evaluated ({}), score ({}), move ({}).",
                    moveScope.getMoveIndex(),
                    moveScope.getStepScope().getPhaseScope().getSolverScope().getMoveEvaluationCount(), moveScope.getScore(),
                    moveScope.getMove());
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
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        Score stepScore = stepScope.getScore();
        var endingReconfiguration = previousScores[lateScoreIndex] == null;
        previousScores[lateScoreIndex] = stepScope.getScore();
        lateScoreIndex = (lateScoreIndex + 1) % lateAcceptanceSize;
        if (maxReconfigurationMoveCount > 0 && lateAcceptanceReconfigurationSize > 0) {
            // The termination is only updated when a superior solution is found
            // or when ending a reconfiguration process.
            // Otherwise, we continue incrementing the moves until the reconfiguration is triggered.
            var improveBestSolution = lastAcceptedScore != null && stepScore.compareTo(lastAcceptedScore) > 0;
            if (endingReconfiguration || improveBestSolution) {
                moveCountTermination.stepEnded(stepScope);
                if (improveBestSolution) {
                    // Reset the current number of accepted solutions
                    logger.info("Best solution improvement: current score ({}), new score ({}).",
                            lastAcceptedScore, stepScope.getScore());
                    currentReconfigurationRationCount = 1;
                }
            }
        }
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        moveCountTermination.phaseEnded(phaseScope);
        previousScores = null;
        lateScoreIndex = -1;
        currentReconfigurationRationCount = 1;
        lastAcceptedScore = null;
    }

    @Override
    public boolean needReconfiguration(LocalSearchStepScope<Solution_> stepScope) {
        return maxReconfigurationMoveCount > 0 && lateAcceptanceReconfigurationSize > 0
                && moveCountTermination.isSolverTerminated(stepScope.getPhaseScope().getSolverScope());
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void applyReconfiguration(LocalSearchStepScope<Solution_> stepScope) {
        var idx = lateScoreIndex;
        if (previousScores[idx] == null) {
            // Reconfiguration already in progress
            return;
        }
        if (currentReconfigurationRationCount > lateAcceptanceReconfigurationSize) {
            // max count reached and starting over again
            currentReconfigurationRationCount = 1;
            logger.info("Reconfiguration resetting: accepted elements count from ({}) to ({}).",
                    lateAcceptanceReconfigurationSize, currentReconfigurationRationCount);
        }
        for (var i = 0; i < currentReconfigurationRationCount; i++) {
            // We first increment idx to ensure stepEnded logic won't rewrite it,
            // so it can be used in the next iteration
            idx = (idx + 1) % lateAcceptanceSize;
            previousScores[idx] = null;
        }
        Score currentBestScore = stepScope.getPhaseScope().getSolverScope().getBestScore();
        if (lastAcceptedScore == null || currentBestScore.compareTo(lastAcceptedScore) > 0) {
            lastAcceptedScore = currentBestScore;
        }
        logger.info(
                "Reconfiguration applied: accepted elements count ({}), max move count ({}), moves evaluated ({}), best current score ({}).",
                currentReconfigurationRationCount, maxReconfigurationMoveCount,
                stepScope.getPhaseScope().getSolverScope().getMoveEvaluationCount(),
                lastAcceptedScore);
        if (lateAcceptanceReconfigurationSize > 1) {
            currentReconfigurationRationCount++;
        }
    }
}
