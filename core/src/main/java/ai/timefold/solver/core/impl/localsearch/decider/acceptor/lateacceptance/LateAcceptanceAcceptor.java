package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import ai.timefold.solver.core.api.score.IBendableScore;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.director.InnerScore;

public class LateAcceptanceAcceptor<Solution_> extends AbstractAcceptor<Solution_> {

    protected int lateAcceptanceSize = -1;
    protected boolean hillClimbingEnabled = true;

    LateAcceptanceScoreBuffer scoreBuffer;
    private BestScoreState<Solution_> bestBestScoreState;

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
        var initialScore = phaseScope.getBestScore();
        scoreBuffer = new LateAcceptanceScoreBuffer(lateAcceptanceSize, initialScore);
        var scoreDefinition = phaseScope.getSolverScope().getScoreDefinition();
        if (scoreDefinition.getLevelsSize() > 1) {
            bestBestScoreState = new BestScoreState<>(initialScore, scoreDefinition);
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
        var lateScore = scoreBuffer.getCurrent();
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

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        if (bestBestScoreState != null) {
            bestBestScoreState.update(stepScope);
        }
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        scoreBuffer.update(stepScope.getScore());
        if (bestBestScoreState != null && bestBestScoreState.isNonDominatedScoreChanged(stepScope)) {
            if (logger.isTraceEnabled()) {
                logger.trace("Late elements reset to {}", stepScope.getPhaseScope().getBestScore().raw());
            }
            scoreBuffer.tryReset(stepScope.getPhaseScope().getBestScore());
        }
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        scoreBuffer = null;
        bestBestScoreState = null;
    }

    private static class BestScoreState<Solution_> {

        private final int nonDominatedLevelCount;
        private long previousBestScoreIndex;
        private double[] previousBestScoreDoubles;
        private boolean firstEvaluation = true;

        @SuppressWarnings("rawtypes")
        BestScoreState(InnerScore initialScore, ScoreDefinition scoreDefinition) {
            previousBestScoreDoubles = initialScore.raw().toLevelDoubles();
            if (IBendableScore.class.isAssignableFrom(scoreDefinition.getScoreClass())) {
                // We only evaluate the hard score levels
                nonDominatedLevelCount = scoreDefinition.getFeasibleLevelsSize();
            } else {
                // We only evaluate the hard or medium levels
                nonDominatedLevelCount = scoreDefinition.getLevelsSize() - 1;
            }
        }

        void update(LocalSearchStepScope<Solution_> stepScope) {
            if (previousBestScoreIndex != stepScope.getPhaseScope().getBestSolutionStepIndex()) {
                // Update the current best score information
                this.previousBestScoreIndex = stepScope.getPhaseScope().getBestSolutionStepIndex();
                this.previousBestScoreDoubles = stepScope.getPhaseScope().getBestScore().raw().toLevelDoubles();
            }
        }

        /**
         * If non-dominated levels are updated (hard or medium), it is necessary to reset the late scores.
         * Failing to do so may cause the solver
         * to accept poor moves that do not affect the non-dominated scores but degrade the soft scores.
         * As a result,
         * any move that does not decrease the hard or medium score
         * but significantly worsens the soft score may be mistakenly accepted.
         * This could cause the working solution
         * to enter a bad region and require many additional steps to escape it.
         * 
         * @return true if any non-dominated score has changed; otherwise, returns false
         */
        boolean isNonDominatedScoreChanged(LocalSearchStepScope<Solution_> stepScope) {
            if (firstEvaluation || previousBestScoreIndex != stepScope.getPhaseScope().getBestSolutionStepIndex()) {
                firstEvaluation = false;
                var newBestScore = stepScope.getPhaseScope().getBestScore();
                var newBestScoreDoubles = newBestScore.raw().toLevelDoubles();
                for (var i = 0; i < nonDominatedLevelCount; i++) {
                    if (newBestScoreDoubles[i] != previousBestScoreDoubles[i]) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

}
