package ai.timefold.solver.core.impl.score.bound;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.constructionheuristic.decider.forager.ConstructionHeuristicForager;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.Acceptor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents the minimum required score a move must evaluate to in order
 * to be accepted for a step.
 * 
 * @param <Score_> the score type
 */
@NullMarked
public interface MinimumScoreBound<Score_ extends Score<Score_>> {
    /**
     * Returns the minimum required score for the next move to be accepted.
     * Returns null if there is no such score.
     * This score may increase or decrease within a step.
     * This cannot be used for multithreaded contexts, unless the
     * {@link Acceptor} or {@link ConstructionHeuristicForager} guarantees that the bound only increases.
     * If the bound decreases, then a move evaluated using the previous score could be partially evaluated
     * under that score but be fully evaluated under the new score.
     * 
     * @return the minimum required score the next move must evaluate to be accepted, or null if there no such score
     */
    @Nullable
    Score_ getMinimumRequiredScoreForMove();

    /**
     * Returns the minimum required score for a move to be accepted within the current step.
     * Returns null if there is no such score.
     * This score cannot change within a step, but may change after a step ends and before one
     * begins.
     * This is useful for multithreaded contexts, where multiple moves are evaluated
     * in parallel (and thus cannot use a fluctuating bound).
     * 
     * @return the minimum required score a move must evaluate to be accepted within a step, or null if there no such score
     */
    @Nullable
    Score_ getMinimumRequiredScoreForStep();
}
