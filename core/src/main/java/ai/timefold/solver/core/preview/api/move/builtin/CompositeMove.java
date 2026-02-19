package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
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
public final class CompositeMove<Solution_> implements Move<Solution_> {

    /**
     * @param moves never null, sometimes empty. Do not modify this argument afterwards or the CompositeMove corrupts.
     * @return never null
     */
    @SafeVarargs
    static <Solution_, Move_ extends Move<Solution_>> Move<Solution_> buildMove(Move_... moves) {
        return switch (moves.length) {
            case 0 -> throw new UnsupportedOperationException("The %s cannot be built from an empty move list."
                    .formatted(CompositeMove.class.getSimpleName()));
            case 1 -> moves[0];
            default -> new CompositeMove<>(moves);
        };
    }

    private final Move<Solution_>[] moves;

    private CompositeMove(Move<Solution_>[] moves) {
        this.moves = moves;
    }

    @Override
    public void execute(MutableSolutionView<Solution_> solutionView) {
        for (var move : moves) {
            move.execute(solutionView);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Move<Solution_> rebase(Rebaser rebaser) {
        Move<Solution_>[] rebasedMoves = new Move[moves.length];
        for (var i = 0; i < moves.length; i++) {
            rebasedMoves[i] = moves[i].rebase(rebaser);
        }
        return new CompositeMove<>(rebasedMoves);
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
    public String describe() {
        return getClass().getSimpleName() + Arrays.stream(moves)
                .map(Move::describe)
                .sorted()
                .map(childMoveTypeDescription -> "* " + childMoveTypeDescription)
                .collect(Collectors.joining(",", "(", ")"));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CompositeMove<?> that
                && Objects.deepEquals(moves, that.moves);
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
