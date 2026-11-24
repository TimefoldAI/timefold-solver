package ai.timefold.solver.core.impl.score.bound;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A {@link MinimumScoreBound} for when the minimum required score for each
 * attempted move in a step is the same.
 * 
 * @param <Score_> the score type
 */
@NullMarked
public final class StepBasedMinimumScoreBound<Score_ extends Score<Score_>>
        implements MinimumScoreBound<Score_> {
    @Nullable
    Score_ minimumRequiredScoreForStep;

    @Override
    public @Nullable Score_ getMinimumRequiredScoreForMove() {
        return minimumRequiredScoreForStep;
    }

    @Override
    public @Nullable Score_ getMinimumRequiredScoreForStep() {
        return minimumRequiredScoreForStep;
    }

    public void clear() {
        minimumRequiredScoreForStep = null;
    }

    /**
     * This method can only be called within {@link PhaseLifecycleListener#phaseStarted(AbstractPhaseScope)},
     * {@link PhaseLifecycleListener#phaseEnded(AbstractPhaseScope)},
     * {@link PhaseLifecycleListener#stepStarted(AbstractStepScope)},
     * and {@link PhaseLifecycleListener#stepEnded(AbstractStepScope)}.
     * <p>
     * Sets the minimum required score for a move to be accepted for the next step.
     */
    public void setMinimumRequiredScoreForStep(@Nullable Score_ score) {
        this.minimumRequiredScoreForStep = score;
    }
}
