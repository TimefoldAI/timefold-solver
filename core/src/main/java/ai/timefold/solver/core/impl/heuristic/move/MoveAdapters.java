package ai.timefold.solver.core.impl.heuristic.move;

import java.util.Iterator;
import java.util.function.Predicate;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.move.MoveDirector;

import org.jspecify.annotations.NullMarked;

/**
 * While the Neighborhoods API and Move Selectors API need to coexist,
 * there are places where moves need to be converted between the old and new types.
 * This class provides static methods to perform these conversions.
 */
@NullMarked
public final class MoveAdapters {

    static <Solution_> ai.timefold.solver.core.impl.heuristic.move.Move<Solution_>
            toLegacyMove(ai.timefold.solver.core.preview.api.move.Move<Solution_> newMove) {
        if (newMove instanceof Move<Solution_> legacyMove) {
            return legacyMove;
        }
        return new NewMoveAdapter<>(newMove);
    }

    public static <Solution_> ai.timefold.solver.core.preview.api.move.Move<Solution_>
            unadapt(ai.timefold.solver.core.preview.api.move.Move<Solution_> possibleLegacyMove) {
        if (possibleLegacyMove instanceof NewMoveAdapter<Solution_> newMoveAdapter) {
            return newMoveAdapter.newMove();
        }
        return possibleLegacyMove;
    }

    public static <Solution_> Iterator<ai.timefold.solver.core.impl.heuristic.move.Move<Solution_>>
            toLegacyMoveIterator(Iterator<ai.timefold.solver.core.preview.api.move.Move<Solution_>> newIterator) {
        return new NewIteratorAdapter<>(newIterator);
    }

    /**
     * Used to determine if a move is doable.
     * A move is only doable if:
     *
     * <ul>
     * <li>It is a new {@link ai.timefold.solver.core.preview.api.move.Move}.</li>
     * <li>It is a legacy move and its {@link AbstractMove#isMoveDoable(ScoreDirector)} return {@code true}.</li>
     * </ul>
     *
     * @param moveDirector never null
     * @param move never null
     * @return true if the move is doable
     */
    public static <Solution_> boolean isDoable(MoveDirector<Solution_, ?> moveDirector,
            ai.timefold.solver.core.preview.api.move.Move<Solution_> move) {
        if (move instanceof Move<Solution_> legacyMove) {
            return legacyMove.isMoveDoable(moveDirector.getScoreDirector());
        } else {
            return true; // New moves are always doable.
        }
    }

    public static <Solution_> boolean testWhenLegacyMove(ai.timefold.solver.core.preview.api.move.Move<Solution_> move,
            Predicate<Move<Solution_>> predicate) {
        if (move instanceof Move<Solution_> legacyMove) {
            return predicate.test(legacyMove);
        } else {
            return false;
        }
    }

    private MoveAdapters() {
        // No instantiation.
    }

}
