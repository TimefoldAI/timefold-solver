package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.heuristic.move.AbstractSelectorBasedMove;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Original iterator for the uni-move stream.
 * Builds moves based on all elements in the dataset, in the order in which they appear.
 */
@NullMarked
final class UniOriginalMoveIterator<Solution_, A> implements Iterator<Move<Solution_>> {

    private final UniMoveStreamContext<Solution_, A> context;

    private @Nullable Move<Solution_> nextMove;
    private @Nullable Iterator<UniTuple<A>> tupleIterator;

    public UniOriginalMoveIterator(UniMoveStreamContext<Solution_, A> context) {
        this.context = Objects.requireNonNull(context);
    }

    @Override
    public boolean hasNext() {
        if (nextMove != null) {
            return true;
        }
        if (tupleIterator == null) { // Only create a possibly expensive instance when we actually need it.
            tupleIterator = context.getDatasetInstance().iterator();
        }
        if (!tupleIterator.hasNext()) {
            return false;
        }
        nextMove = context.buildMove(tupleIterator.next().getA());
        if (nextMove instanceof AbstractSelectorBasedMove<Solution_> legacyMove) {
            throw new UnsupportedOperationException("""
                    Neighborhoods do not support legacy moves.
                    Please refactor your code (%s) to use the new Move API."""
                    .formatted(legacyMove.getClass().getCanonicalName()));
        }
        return true;
    }

    @Override
    public Move<Solution_> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        var result = Objects.requireNonNull(nextMove);
        nextMove = null;
        return result;
    }

}
