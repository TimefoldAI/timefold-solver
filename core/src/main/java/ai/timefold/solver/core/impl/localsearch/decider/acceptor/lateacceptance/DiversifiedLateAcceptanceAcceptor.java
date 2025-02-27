package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;

public class DiversifiedLateAcceptanceAcceptor<Solution_> extends AbstractAcceptor<Solution_> {

    // The worst score in the late elements list
    protected Score<?> lateWorse;
    // Number of occurrences of lateWorse in the late elements
    protected int lateWorseOccurrences = -1;

    protected int lateAcceptanceSize = -1;

    protected Score<?>[] previousScores;
    protected int lateScoreIndex = -1;

    public void setLateAcceptanceSize(int lateAcceptanceSize) {
        this.lateAcceptanceSize = lateAcceptanceSize;
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
        lateWorseOccurrences = lateAcceptanceSize;
        lateWorse = initialScore;
    }

    private void validate() {
        if (lateAcceptanceSize <= 0) {
            throw new IllegalArgumentException(
                    "The lateAcceptanceSize (%d) cannot be negative or zero.".formatted(lateAcceptanceSize));
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        // The acceptance and replacement strategies are based on the work:
        // Diversified Late Acceptance Search by M. Namazi, C. Sanderson, M. A. H. Newton, M. M. A. Polash, and A. Sattar
        var moveScore = moveScope.getScore();
        var current = (Score) moveScope.getStepScope().getPhaseScope().getLastCompletedStepScope().getScore();
        var previous = current;
        var accept = moveScore.compareTo(current) == 0 || moveScore.compareTo(lateWorse) > 0;
        if (accept) {
            current = moveScore;
        }
        var lateScore = previousScores[lateScoreIndex];
        // Improves the diversification to allow the next iterations to find a better solution
        var currentScoreCmp = current.compareTo(lateScore);
        var currentScoreWorse = currentScoreCmp < 0;
        // Improves the intensification but avoids replacing values when the search falls into a plateau or local minima
        var currentScoreBetter = currentScoreCmp > 0 && current.compareTo(previous) > 0;
        if (currentScoreWorse || currentScoreBetter) {
            updateLateScore(current);
        }
        lateScoreIndex = (lateScoreIndex + 1) % lateAcceptanceSize;
        return accept;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void updateLateScore(Score newScore) {
        var newScoreWorseCmp = newScore.compareTo(lateWorse);
        var lateScore = (Score) previousScores[lateScoreIndex];
        var lateScoreEqual = lateScore.compareTo(lateWorse) == 0;
        if (newScoreWorseCmp < 0) {
            this.lateWorse = newScore;
            this.lateWorseOccurrences = 1;
        } else if (lateScoreEqual && newScoreWorseCmp != 0) {
            this.lateWorseOccurrences--;
        } else if (!lateScoreEqual && newScoreWorseCmp == 0) {
            this.lateWorseOccurrences++;
        }
        previousScores[lateScoreIndex] = newScore;
        // Recompute the new lateWorse and the number of occurrences
        if (lateWorseOccurrences == 0) {
            lateWorse = previousScores[0];
            lateWorseOccurrences = 1;
            for (var i = 1; i < lateAcceptanceSize; i++) {
                Score previousScore = previousScores[i];
                var scoreCmp = previousScore.compareTo(lateWorse);
                if (scoreCmp < 0) {
                    lateWorse = previousScores[i];
                    lateWorseOccurrences = 1;
                } else if (scoreCmp == 0) {
                    lateWorseOccurrences++;
                }
            }
        }
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        previousScores = null;
        lateScoreIndex = -1;
        lateWorse = null;
        lateWorseOccurrences = -1;
    }
}