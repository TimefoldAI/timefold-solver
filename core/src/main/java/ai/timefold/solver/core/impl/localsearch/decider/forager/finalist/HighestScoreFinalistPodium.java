package ai.timefold.solver.core.impl.localsearch.decider.forager.finalist;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

/**
 * Default implementation of {@link FinalistPodium}.
 *
 * @see FinalistPodium
 */
public final class HighestScoreFinalistPodium<Solution_> extends AbstractFinalistPodium<Solution_> {

    private Score<?> finalistScore;

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        finalistScore = null;
    }

    @Override
    public void addMove(LocalSearchMoveScope<Solution_> moveScope) {
        var accepted = moveScope.getAccepted() != null && moveScope.getAccepted();
        if (finalistIsAccepted && !accepted) {
            return;
        }
        if (accepted && !finalistIsAccepted) {
            finalistIsAccepted = true;
            finalistScore = null;
        }
        var moveScore = moveScope.getScore().raw(); // Guaranteed local search; no need for InnerScore.
        var scoreComparison = doComparison(moveScore);
        if (scoreComparison > 0) {
            finalistScore = moveScore;
            clearAndAddFinalist(moveScope);
        } else if (scoreComparison == 0) {
            addFinalist(moveScope);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private int doComparison(Score moveScore) {
        if (finalistScore == null) {
            return 1;
        }
        return moveScore.compareTo(finalistScore);
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        finalistScore = null;
    }

}
