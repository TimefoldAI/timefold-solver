package ai.timefold.solver.core.impl.score.bound;

import ai.timefold.solver.core.api.score.Score;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A {@link MinimumScoreBound} for when the minimum required score for each
 * attempted move in a step can differ.
 * 
 * @param <Score_> the score type
 */
@NullMarked
public final class MoveBasedMinimumScoreBound<Score_ extends Score<Score_>>
        implements MinimumScoreBound<Score_> {
    @Nullable
    Score_ minimumRequiredScoreForMove;

    @Override
    public @Nullable Score_ getMinimumRequiredScoreForMove() {
        return minimumRequiredScoreForMove;
    }

    @Override
    public @Nullable Score_ getMinimumRequiredScoreForStep() {
        // As each move can have a different minimum score bound, there is no minimum
        // bound for the step
        return null;
    }

    public void clear() {
        minimumRequiredScoreForMove = null;
    }

    /**
     * Sets the minimum required score for a move to be accepted.
     */
    public void setMinimumRequiredScore(@Nullable Score_ score) {
        this.minimumRequiredScoreForMove = score;
    }
}
