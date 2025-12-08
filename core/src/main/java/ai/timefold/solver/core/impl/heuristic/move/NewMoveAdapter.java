package ai.timefold.solver.core.impl.heuristic.move;

import java.util.Collection;
import java.util.Objects;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.move.director.MoveDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;

/**
 * Adapts {@link Move a new move}
 * to {@link ai.timefold.solver.core.impl.heuristic.move.Move a legacy move}.
 * Once the move selector framework is removed, this may be removed as well.
 * 
 * @param newMove the move to adapt
 * @param <Solution_>
 */
@NullMarked
record NewMoveAdapter<Solution_>(Move<Solution_> newMove)
        implements
            ai.timefold.solver.core.impl.heuristic.move.Move<Solution_> {

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return true; // New moves are always doable.
    }

    @Override
    public void doMoveOnly(ScoreDirector<Solution_> scoreDirector) {
        newMove.execute(getMoveDirector(scoreDirector));
    }

    private MoveDirector<Solution_, ?> getMoveDirector(ScoreDirector<Solution_> scoreDirector) {
        return ((InnerScoreDirector<Solution_, ?>) scoreDirector).getMoveDirector();
    }

    @Override
    public ai.timefold.solver.core.impl.heuristic.move.Move<Solution_>
            rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        return MoveAdapters.toLegacyMove(newMove.rebase(getMoveDirector(destinationScoreDirector)));
    }

    @Override
    public String getSimpleMoveTypeDescription() {
        return newMove.describe();
    }

    @Override
    public Collection<?> getPlanningEntities() {
        return newMove.extractPlanningEntities();
    }

    @Override
    public Collection<?> getPlanningValues() {
        return newMove.extractPlanningValues();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NewMoveAdapter<?> other
                && Objects.equals(newMove, other.newMove);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(newMove);
    }

    @Override
    public String toString() {
        return newMove.toString();
    }

}
