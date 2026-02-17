package ai.timefold.solver.core.impl.heuristic.move;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NullMarked;

/**
 * A CompositeMove is composed out of multiple other moves.
 * <p>
 * Warning: each of moves in the moveList must not rely on the effect of a previous move in the moveList
 * to create its undoMove correctly.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see Move
 */
@NullMarked
public final class SelectorBasedCompositeMove<Solution_> extends AbstractSelectorBasedMove<Solution_> {

    /**
     * @param moves never null, sometimes empty. Do not modify this argument afterwards or the CompositeMove corrupts.
     * @return never null
     */
    @SafeVarargs
    public static <Solution_, Move_ extends Move<Solution_>> Move<Solution_> buildMove(Move_... moves) {
        return switch (moves.length) {
            case 0 -> SelectorBasedNoChangeMove.getInstance();
            case 1 -> moves[0];
            default -> new SelectorBasedCompositeMove<>(moves);
        };
    }

    /**
     * @param moveList never null, sometimes empty
     * @return never null
     */
    @SuppressWarnings("unchecked")
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
    SelectorBasedCompositeMove(Move<Solution_>... moves) {
        this.moves = moves;
    }

    public Move<Solution_>[] getMoves() {
        return moves;
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        for (var move : moves) {
            if (move instanceof AbstractSelectorBasedMove<Solution_> legacyMove
                    && legacyMove.isMoveDoable(scoreDirector)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void execute(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        for (var move : moves) {
            if (move instanceof AbstractSelectorBasedMove<Solution_> legacyMove) {
                if (legacyMove.isMoveDoable(scoreDirector)) {
                    legacyMove.execute(scoreDirector);
                }
            } else {
                move.execute(((InnerScoreDirector<Solution_, ?>) scoreDirector).getMoveDirector());
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public SelectorBasedCompositeMove<Solution_> rebase(Rebaser rebaser) {
        var rebasedMoves = new Move[moves.length];
        for (var i = 0; i < moves.length; i++) {
            rebasedMoves[i] = moves[i].rebase(rebaser);
        }
        return new SelectorBasedCompositeMove<Solution_>(rebasedMoves);
    }

    @Override
    public String describe() {
        return "CompositeMove" + Arrays.stream(moves)
                .map(Move::describe)
                .sorted()
                .map(childMoveTypeDescription -> "* " + childMoveTypeDescription)
                .collect(Collectors.joining(",", "(", ")"));
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        var entities = LinkedHashSet.newLinkedHashSet(moves.length * 2);
        for (var move : moves) {
            entities.addAll(move.getPlanningEntities());
        }
        return entities;
    }

    @Override
    public SequencedCollection<Object> getPlanningValues() {
        var values = LinkedHashSet.newLinkedHashSet(moves.length * 2);
        for (var move : moves) {
            values.addAll(move.getPlanningValues());
        }
        return values;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SelectorBasedCompositeMove<?> otherCompositeMove
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
