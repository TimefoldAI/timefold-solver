package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;

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
     * @param moveList Do not modify this argument afterwards or the CompositeMove corrupts.
     * @return never null
     */
    static <Solution_> Move<Solution_> buildMove(List<? extends Move<Solution_>> moveList) {
        return switch (moveList.size()) {
            case 0 -> throw new UnsupportedOperationException("The %s cannot be built from an empty move list."
                    .formatted(CompositeMove.class.getSimpleName()));
            case 1 -> moveList.getFirst();
            default -> new CompositeMove<>(moveList);
        };
    }

    private final List<? extends Move<Solution_>> moveList;

    private CompositeMove(List<? extends Move<Solution_>> moveList) {
        this.moveList = moveList;
    }

    @Override
    public void execute(MutableSolutionView<Solution_> solutionView) {
        for (var move : moveList) {
            move.execute(solutionView);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Move<Solution_> rebase(Lookup lookup) {
        Move<Solution_>[] rebasedMoves = new Move[moveList.size()];
        for (var i = 0; i < moveList.size(); i++) {
            rebasedMoves[i] = moveList.get(i).rebase(lookup);
        }
        return new CompositeMove<>(Arrays.asList(rebasedMoves));
    }

    @Override
    public SequencedCollection<Object> getPlanningEntities() {
        var entities = LinkedHashSet.newLinkedHashSet(moveList.size() * 2);
        for (var move : moveList) {
            entities.addAll(move.getPlanningEntities());
        }
        return entities;
    }

    @Override
    public SequencedCollection<Object> getPlanningValues() {
        var values = LinkedHashSet.newLinkedHashSet(moveList.size() * 2);
        for (var move : moveList) {
            values.addAll(move.getPlanningValues());
        }
        return values;
    }

    @Override
    public String describe() {
        return getClass().getSimpleName() + moveList.stream()
                .map(Move::describe)
                .sorted()
                .map(childMoveTypeDescription -> "*" + childMoveTypeDescription)
                .collect(Collectors.joining(",", "(", ")"));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CompositeMove<?> that
                && Objects.equals(moveList, that.moveList);
    }

    @Override
    public int hashCode() {
        return moveList.hashCode();
    }

    @Override
    public String toString() {
        return moveList.toString();
    }
}
