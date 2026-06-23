package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.IBendableScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScore;

public class LateAcceptanceAcceptor<Solution_> extends AbstractAcceptor<Solution_> {

    protected int lateAcceptanceSize = -1;
    protected boolean hillClimbingEnabled = true;

    protected InnerScore<?>[] previousScores;
    protected int lateScoreIndex = -1;

    private boolean canResetLastBestScores = false;
    private long previousBestScoreIndex;
    private double[] previousBestScoreDoubles;

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
        previousScores = new InnerScore[lateAcceptanceSize];
        var initialScore = phaseScope.getBestScore();
        Arrays.fill(previousScores, initialScore);
        lateScoreIndex = 0;
        var scoreDefinition = phaseScope.getSolverScope().getScoreDefinition();
        canResetLastBestScores =
                !IBendableScore.class.isAssignableFrom(scoreDefinition.getScoreClass()) && scoreDefinition.getLevelsSize() > 1;
        if (canResetLastBestScores) {
            previousBestScoreIndex = 0;
            previousBestScoreDoubles = initialScore.raw().toLevelDoubles();
        }
    }

    private void validate() {
        if (lateAcceptanceSize <= 0) {
            throw new IllegalArgumentException(
                    "The lateAcceptanceSize (%d) cannot be negative or zero.".formatted(lateAcceptanceSize));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        var moveScore = (InnerScore) moveScope.getScore();
        var lateScore = getPreviousScore(lateScoreIndex);
        if (moveScore.compareTo(lateScore) >= 0) {
            return true;
        }
        if (hillClimbingEnabled) {
            var lastStepScore = moveScope.getStepScope().getPhaseScope()
                    .getLastCompletedStepScope().getScore();
            return moveScore.compareTo(lastStepScore) >= 0;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private <Score_ extends Score<Score_>> InnerScore<Score_> getPreviousScore(int lateScoreIndex) {
        return (InnerScore<Score_>) previousScores[lateScoreIndex];
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        if (canResetLastBestScores && previousBestScoreIndex != stepScope.getPhaseScope().getBestSolutionStepIndex()) {
            this.previousBestScoreIndex = stepScope.getPhaseScope().getBestSolutionStepIndex();
            this.previousBestScoreDoubles = stepScope.getPhaseScope().getBestScore().raw().toLevelDoubles();
        }
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        previousScores[lateScoreIndex] = stepScope.getScore();
        lateScoreIndex = (lateScoreIndex + 1) % lateAcceptanceSize;
        if (canResetLastBestScores && previousBestScoreIndex != stepScope.getPhaseScope().getBestSolutionStepIndex()) {
            var newBestScore = stepScope.getPhaseScope().getBestScore();
            var newBestScoreDoubles = newBestScore.raw().toLevelDoubles();
            var hardOrMediumChanged = false;
            for (var i = 0; i < newBestScoreDoubles.length - 1; i++) {
                if (newBestScoreDoubles[i] != previousBestScoreDoubles[i]) {
                    hardOrMediumChanged = true;
                    break;
                }
            }
            if (hardOrMediumChanged) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Late elements reset to {}", newBestScore);
                }
                Arrays.fill(previousScores, newBestScore);
            }
        }
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        previousScores = null;
        lateScoreIndex = -1;
    }

}
