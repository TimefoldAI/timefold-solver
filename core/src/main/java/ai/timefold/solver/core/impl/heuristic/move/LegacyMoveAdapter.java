package ai.timefold.solver.core.impl.heuristic.move;

import java.util.Collection;

import ai.timefold.solver.core.api.move.Move;
import ai.timefold.solver.core.api.move.MutableSolutionState;
import ai.timefold.solver.core.api.move.SolutionState;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.move.InnerMutableSolutionState;
import ai.timefold.solver.core.impl.move.director.MoveDirector;
import ai.timefold.solver.core.impl.move.generic.NoChangeMove;

/**
 * Adapts {@link ai.timefold.solver.core.impl.heuristic.move.Move} a legacy move)
 * to {@link Move a new move}.
 * Once the move selector framework is removed, this may be removed as well.
 * 
 * @param legacyMove never null
 * @param <Solution_>
 */
public record LegacyMoveAdapter<Solution_>(
        ai.timefold.solver.core.impl.heuristic.move.Move<Solution_> legacyMove) implements Move<Solution_> {

    /**
     * Used to determine if a move is doable.
     * A move is only doable if:
     * 
     * <ul>
     * <li>It is a new {@link Move} and not a {@link NoChangeMove}</li>
     * <li>It is a legacy move and its {@link AbstractMove#isMoveDoable(ScoreDirector)} return false.</li>
     * </ul>
     * 
     * New moves are doable by default.
     * 
     * @param moveDirector never null
     * @param move never null
     * @return true if the move is doable
     * @param <Solution_>
     */
    public static <Solution_> boolean isDoable(MoveDirector<Solution_> moveDirector, Move<Solution_> move) {
        if (move instanceof LegacyMoveAdapter<Solution_> legacyMoveAdapter) {
            return legacyMoveAdapter.isMoveDoable(moveDirector);
        } else {
            return !(move instanceof NoChangeMove<Solution_>);
        }
    }

    @Override
    public void execute(MutableSolutionState<Solution_> mutableSolutionState) {
        var scoreDirector = getScoreDirector(mutableSolutionState);
        legacyMove.doMoveOnly(scoreDirector);
    }

    private ScoreDirector<Solution_> getScoreDirector(SolutionState<Solution_> mutableSolutionState) {
        return ((InnerMutableSolutionState<Solution_>) mutableSolutionState).getScoreDirector();
    }

    public boolean isMoveDoable(SolutionState<Solution_> solutionState) {
        return legacyMove.isMoveDoable(getScoreDirector(solutionState));
    }

    @Override
    public String describe() {
        return legacyMove.getSimpleMoveTypeDescription();
    }

    @Override
    public Move<Solution_> rebase(SolutionState<Solution_> solutionState) {
        return new LegacyMoveAdapter<>(legacyMove.rebase(getScoreDirector(solutionState)));
    }

    @Override
    public Collection<?> getPlanningEntities() {
        return legacyMove.getPlanningEntities();
    }

    @Override
    public Collection<?> getPlanningValues() {
        return legacyMove.getPlanningValues();
    }

    @Override
    public String toString() {
        return legacyMove.toString();
    }
}
