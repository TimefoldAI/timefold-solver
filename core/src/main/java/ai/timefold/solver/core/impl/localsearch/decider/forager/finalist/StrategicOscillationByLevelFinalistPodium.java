package ai.timefold.solver.core.impl.localsearch.decider.forager.finalist;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

/**
 * Strategic oscillation, works well with Tabu search.
 *
 * @see FinalistPodium
 */
public final class StrategicOscillationByLevelFinalistPodium<Solution_> extends AbstractFinalistPodium<Solution_> {

    private final boolean referenceBestScoreInsteadOfLastStepScore;

    // Guaranteed inside local search, therefore no need for InnerScore.
    private Score<?> referenceScore;
    private Number[] referenceLevelNumbers;
    private Score<?> finalistScore;
    private Number[] finalistLevelNumbers;
    private boolean finalistImprovesUponReference;

    public StrategicOscillationByLevelFinalistPodium(boolean referenceBestScoreInsteadOfLastStepScore) {
        this.referenceBestScoreInsteadOfLastStepScore = referenceBestScoreInsteadOfLastStepScore;
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        referenceScore = referenceBestScoreInsteadOfLastStepScore
                ? stepScope.getPhaseScope().getBestScore().raw()
                : stepScope.getPhaseScope().getLastCompletedStepScope().getScore().raw();
        referenceLevelNumbers = referenceScore.toLevelNumbers();
        finalistScore = null;
        finalistLevelNumbers = null;
        finalistImprovesUponReference = false;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void addMove(LocalSearchMoveScope<Solution_> moveScope) {
        boolean accepted = moveScope.getAccepted();
        if (finalistIsAccepted && !accepted) {
            return;
        }
        if (accepted && !finalistIsAccepted) {
            finalistIsAccepted = true;
            finalistScore = null;
            finalistLevelNumbers = null;
        }
        Score moveScore = moveScope.getScore().raw();
        var moveLevelNumbers = moveScore.toLevelNumbers();
        var comparison = doComparison(moveScore, moveLevelNumbers);
        if (comparison > 0) {
            finalistScore = moveScore;
            finalistLevelNumbers = moveLevelNumbers;
            finalistImprovesUponReference = (moveScore.compareTo(referenceScore) > 0);
            clearAndAddFinalist(moveScope);
        } else if (comparison == 0) {
            addFinalist(moveScope);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private int doComparison(Score moveScore, Number[] moveLevelNumbers) {
        if (finalistScore == null) {
            return 1;
        }
        // If there is an improving move, do not oscillate
        if (!finalistImprovesUponReference && moveScore.compareTo(referenceScore) < 0) {
            for (var i = 0; i < referenceLevelNumbers.length; i++) {
                var moveIsHigher = ((Comparable) moveLevelNumbers[i]).compareTo(referenceLevelNumbers[i]) > 0;
                var finalistIsHigher = ((Comparable) finalistLevelNumbers[i]).compareTo(referenceLevelNumbers[i]) > 0;
                if (moveIsHigher) {
                    if (finalistIsHigher) {
                        // Both are higher, take the best one but do not ignore higher levels
                        break;
                    } else {
                        // The move has the first level which is higher while the finalist is lower than the reference
                        return 1;
                    }
                } else {
                    if (finalistIsHigher) {
                        // The finalist has the first level which is higher while the move is lower than the reference
                        return -1;
                    } else {
                        // Both are lower, ignore this level
                    }
                }
            }
        }
        return moveScore.compareTo(finalistScore);
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        referenceScore = null;
        referenceLevelNumbers = null;
        finalistScore = null;
        finalistLevelNumbers = null;
    }

}
