package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import ai.timefold.solver.core.api.score.IBendableScore;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.director.InnerScore;

/**
 * Default implementation of {@link LevelScoreState} for scores with more than one level.
 * <p>
 * Caches the best-solution step index and the corresponding score level values.
 * On each step, {@link #update} refreshes the cache when the best solution has changed,
 * and {@link #isNonDominatedLevelChanged} compares the non-dominated levels
 * (hard for {@link IBendableScore IBendableScore}, hard and medium for all others)
 * against the cached values to determine whether the {@link LateAcceptanceScoreBuffer} should be reset.
 */
final class DefaultLevelScoreState<Solution_> implements LevelScoreState<Solution_> {

    private final int nonDominatedLevelCount;
    private long previousBestScoreIndex;
    private Number[] previousBestScoreLevels;

    @SuppressWarnings("rawtypes")
    DefaultLevelScoreState(InnerScore initialScore, ScoreDefinition scoreDefinition) {
        previousBestScoreLevels = initialScore.raw().toLevelNumbers();
        if (IBendableScore.class.isAssignableFrom(scoreDefinition.getScoreClass())) {
            // We only evaluate the hard score levels
            nonDominatedLevelCount = scoreDefinition.getFeasibleLevelsSize();
        } else {
            // We only evaluate the hard or medium levels
            nonDominatedLevelCount = scoreDefinition.getLevelsSize() - 1;
        }
    }

    @Override
    public void update(LocalSearchStepScope<Solution_> stepScope) {
        if (previousBestScoreIndex != stepScope.getPhaseScope().getBestSolutionStepIndex()) {
            // Update the current best score information
            this.previousBestScoreIndex = stepScope.getPhaseScope().getBestSolutionStepIndex();
            this.previousBestScoreLevels = stepScope.getPhaseScope().getBestScore().raw().toLevelNumbers();
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
    @Override
    public boolean isNonDominatedLevelChanged(LocalSearchStepScope<Solution_> stepScope) {
        if (previousBestScoreIndex != stepScope.getPhaseScope().getBestSolutionStepIndex()) {
            var newBestScore = stepScope.getPhaseScope().getBestScore();
            var newBestScoreDoubles = newBestScore.raw().toLevelNumbers();
            for (var i = 0; i < nonDominatedLevelCount; i++) {
                if (!newBestScoreDoubles[i].equals(previousBestScoreLevels[i])) {
                    return true;
                }
            }
        }
        return false;
    }
}
