package ai.timefold.solver.core.impl.heuristic.move;

import java.util.Collection;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

/**
 * Abstract superclass for {@link Move}, suggested starting point to implement undo moves
 * when not using {@link AbstractSimplifiedMove}.
 * Unless raw performance is a concern, consider using {@link AbstractSimplifiedMove} instead.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see Move
 */
public abstract class AbstractUndoMove<Solution_> implements Move<Solution_> {

    protected final Move<Solution_> parentMove;

    protected AbstractUndoMove(Move<Solution_> parentMove) {
        this.parentMove = Objects.requireNonNull(parentMove);
    }

    @Override
    public final boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return true; // Undo moves are always doable; the parent move was already done.
    }

    @Override
    public final Move<Solution_> doMove(ScoreDirector<Solution_> scoreDirector) {
        throw new UnsupportedOperationException("""
                Impossible state: undo move (%s) can't be undone.
                doMoveOnly(...) should have been called instead."""
                .formatted(this));
    }

    @Override
    public final void doMoveOnly(ScoreDirector<Solution_> scoreDirector) {
        doMoveOnGenuineVariables(scoreDirector);
        scoreDirector.triggerVariableListeners();
    }

    /**
     * Like {@link #doMoveOnly(ScoreDirector)} but without the {@link ScoreDirector#triggerVariableListeners()} call
     * (because {@link #doMoveOnly(ScoreDirector)} already does that).
     *
     * @param scoreDirector never null
     */
    protected abstract void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector);

    @Override
    public final Collection<?> getPlanningEntities() {
        return parentMove.getPlanningEntities();
    }

    @Override
    public final Collection<?> getPlanningValues() {
        return parentMove.getPlanningValues();
    }

    @Override
    public String getSimpleMoveTypeDescription() {
        return "Undo(" + parentMove.getSimpleMoveTypeDescription() + ")";
    }

    @Override
    public String toString() {
        return getSimpleMoveTypeDescription();
    }
}
