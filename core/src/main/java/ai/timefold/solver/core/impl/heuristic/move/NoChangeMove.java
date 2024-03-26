package ai.timefold.solver.core.impl.heuristic.move;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

/**
 * Makes no changes.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class NoChangeMove<Solution_> extends AbstractSimplifiedMove<Solution_> {

    public static final NoChangeMove<?> INSTANCE = new NoChangeMove<>();

    public static <Solution_> NoChangeMove<Solution_> getInstance() {
        return (NoChangeMove<Solution_>) INSTANCE;
    }

    private NoChangeMove() {
        // No external instances allowed.
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return false;
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        // Do nothing.
    }

    @Override
    public Move<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        return (Move<Solution_>) INSTANCE;
    }

    @Override
    public String getSimpleMoveTypeDescription() {
        return "No change";
    }

}
