package ai.timefold.solver.core.impl.heuristic.move;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.util.CollectionUtils;

/**
 * A CompositeMove is composed out of multiple other moves.
 * <p>
 * Warning: each of moves in the moveList must not rely on the effect of a previous move in the moveList
 * to create its undoMove correctly.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see Move
 */
public final class CompositeMove<Solution_> implements Move<Solution_> {

    /**
     * @param moves never null, sometimes empty. Do not modify this argument afterwards or the CompositeMove corrupts.
     * @return never null
     */
    @SafeVarargs
    public static <Solution_, Move_ extends Move<Solution_>> Move<Solution_> buildMove(Move_... moves) {
        return switch (moves.length) {
            case 0 -> NoChangeMove.getInstance();
            case 1 -> moves[0];
            default -> new CompositeMove<>(moves);
        };
    }

    /**
     * @param moveList never null, sometimes empty
     * @return never null
     */
    public static <Solution_, Move_ extends Move<Solution_>> Move<Solution_> buildMove(List<Move_> moveList) {
        return buildMove(moveList.toArray(new Move[0]));
    }

    // ************************************************************************
    // Non-static members
    // ************************************************************************

    private final Move<Solution_>[] moves;

    /**
     * @param moves never null, never empty. Do not modify this argument afterwards or this CompositeMove corrupts.
     */
    @SafeVarargs
    CompositeMove(Move<Solution_>... moves) {
        this.moves = moves;
    }

    public Move<Solution_>[] getMoves() {
        return moves;
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        for (Move<Solution_> move : moves) {
            if (move.isMoveDoable(scoreDirector)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Move<Solution_> doMove(ScoreDirector<Solution_> scoreDirector) {
        Move<Solution_>[] undoMoves = new Move[moves.length];
        int doableCount = 0;
        for (Move<Solution_> move : moves) {
            if (!move.isMoveDoable(scoreDirector)) {
                continue;
            }
            // Calls scoreDirector.triggerVariableListeners() between moves
            // because a later move can depend on the shadow variables changed by an earlier move
            Move<Solution_> undoMove = move.doMove(scoreDirector);
            // Undo in reverse order and each undoMove is created after previous moves have been done
            undoMoves[moves.length - 1 - doableCount] = undoMove;
            doableCount++;
        }
        if (doableCount < undoMoves.length) {
            undoMoves = Arrays.copyOfRange(undoMoves, undoMoves.length - doableCount, undoMoves.length);
        }
        // No need to call scoreDirector.triggerVariableListeners() because Move.doMove() already does it for every move.
        return CompositeMove.buildMove(undoMoves);
    }

    @Override
    public void doMoveOnly(ScoreDirector<Solution_> scoreDirector) {
        for (Move<Solution_> move : moves) {
            if (!move.isMoveDoable(scoreDirector)) {
                continue;
            }
            // Calls scoreDirector.triggerVariableListeners() between moves
            // because a later move can depend on the shadow variables changed by an earlier move
            move.doMoveOnly(scoreDirector);
        }
    }

    @Override
    public CompositeMove<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        Move<Solution_>[] rebasedMoves = new Move[moves.length];
        for (int i = 0; i < moves.length; i++) {
            rebasedMoves[i] = moves[i].rebase(destinationScoreDirector);
        }
        return new CompositeMove<>(rebasedMoves);
    }

    // ************************************************************************
    // Introspection methods
    // ************************************************************************

    @Override
    public String getSimpleMoveTypeDescription() {
        return getClass().getSimpleName() + Arrays.stream(moves)
                .map(Move::getSimpleMoveTypeDescription)
                .sorted()
                .map(childMoveTypeDescription -> "* " + childMoveTypeDescription)
                .collect(Collectors.joining(",", "(", ")"));
    }

    @Override
    public Collection<?> getPlanningEntities() {
        Set<Object> entities = CollectionUtils.newLinkedHashSet(moves.length * 2);
        for (Move<Solution_> move : moves) {
            entities.addAll(move.getPlanningEntities());
        }
        return entities;
    }

    @Override
    public Collection<?> getPlanningValues() {
        Set<Object> values = CollectionUtils.newLinkedHashSet(moves.length * 2);
        for (Move<Solution_> move : moves) {
            values.addAll(move.getPlanningValues());
        }
        return values;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CompositeMove<?> otherCompositeMove
                && Arrays.equals(moves, otherCompositeMove.moves);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(moves);
    }

    @Override
    public String toString() {
        return Arrays.toString(moves);
    }

}
