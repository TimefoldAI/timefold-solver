package ai.timefold.solver.core.impl.score.bound;

import ai.timefold.solver.core.api.score.Score;

import org.jspecify.annotations.Nullable;

/**
 * A {@link MinimumScoreBound} for when there is no minimum required score for
 * a step to be accepted.
 * 
 * @param <Score_> the score type
 */
public final class EmptyMinimumScoreBound<Score_ extends Score<Score_>> implements MinimumScoreBound<Score_> {
    private static final EmptyMinimumScoreBound<?> INSTANCE = new EmptyMinimumScoreBound<>();

    private EmptyMinimumScoreBound() {
    }

    @SuppressWarnings("unchecked")
    public static <Score_ extends Score<Score_>> EmptyMinimumScoreBound<Score_> getInstance() {
        return (EmptyMinimumScoreBound<Score_>) INSTANCE;
    }

    @Override
    public @Nullable Score_ getMinimumRequiredScoreForMove() {
        return null;
    }

    @Override
    public @Nullable Score_ getMinimumRequiredScoreForStep() {
        return null;
    }
}
