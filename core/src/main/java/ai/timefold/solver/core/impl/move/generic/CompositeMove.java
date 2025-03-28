package ai.timefold.solver.core.impl.move.generic;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.util.CollectionUtils;
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
    @SuppressWarnings("unchecked")
    public static <Solution_, Move_ extends Move<Solution_>> Move<Solution_> buildMove(List<Move_> moveList) {
        return buildMove(moveList.toArray(new Move[0]));
    }

    private final Move<Solution_>[] moves;

    private CompositeMove(Move<Solution_>[] moves) {
        this.moves = moves;
    }

    @Override
    public void execute(MutableSolutionView<Solution_> solutionView) {
        for (Move<Solution_> move : moves) {
            move.execute(solutionView);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Move<Solution_> rebase(Rebaser rebaser) {
        Move<Solution_>[] rebasedMoves = new Move[moves.length];
        for (int i = 0; i < moves.length; i++) {
            rebasedMoves[i] = moves[i].rebase(rebaser);
        }
        return new CompositeMove<>(rebasedMoves);
    }

    @Override
    public Collection<?> extractPlanningEntities() {
        Set<Object> entities = CollectionUtils.newLinkedHashSet(moves.length * 2);
        for (Move<Solution_> move : moves) {
            entities.addAll(move.extractPlanningEntities());
        }
        return entities;
    }

    @Override
    public Collection<?> extractPlanningValues() {
        Set<Object> values = CollectionUtils.newLinkedHashSet(moves.length * 2);
        for (Move<Solution_> move : moves) {
            values.addAll(move.extractPlanningValues());
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
        return o instanceof CompositeMove<?> that && Objects.deepEquals(moves, that.moves);
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
