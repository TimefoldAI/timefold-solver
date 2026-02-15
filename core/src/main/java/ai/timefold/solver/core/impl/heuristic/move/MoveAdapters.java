package ai.timefold.solver.core.impl.heuristic.move;

import java.util.function.Predicate;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.move.MoveDirector;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;

/**
 * While the Neighborhoods API and Move Selectors API need to coexist,
 * there are places where moves need to be converted between the old and new types.
 * This class provides static methods to perform these conversions.
 */
@NullMarked
public final class MoveAdapters {

    /**
     * Used to determine if a move is doable.
     * A move is only doable if:
     *
     * <ul>
     * <li>It is a new {@link Move}.</li>
     * <li>It is a legacy move and its {@link AbstractSelectorBasedMove#isMoveDoable(ScoreDirector)} return {@code true}.</li>
     * </ul>
     *
     * @param moveDirector never null
     * @param move never null
     * @return true if the move is doable
     */
    public static <Solution_> boolean isDoable(MoveDirector<Solution_, ?> moveDirector, Move<Solution_> move) {
        if (move instanceof AbstractSelectorBasedMove<Solution_> legacyMove) {
            return legacyMove.isMoveDoable(moveDirector.getScoreDirector());
        } else {
            return true; // New moves are always doable.
        }
    }

    public static <Solution_> boolean testWhenLegacyMove(Move<Solution_> move, Predicate<Move<Solution_>> predicate) {
        if (move instanceof AbstractSelectorBasedMove<Solution_> legacyMove) {
            return predicate.test(legacyMove);
        } else {
            return false;
        }
    }

    private MoveAdapters() {
        // No instantiation.
    }

}
