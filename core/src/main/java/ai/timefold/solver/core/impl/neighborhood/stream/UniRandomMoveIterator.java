package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.heuristic.move.AbstractSelectorBasedMove;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * An iterator for the uni-move stream which returns elements in random order.
 * Implements sampling without replacement via {@link ai.timefold.solver.core.impl.bavet.common.index.UniqueRandomIterator}.
 */
@NullMarked
final class UniRandomMoveIterator<Solution_, A> implements Iterator<Move<Solution_>> {

    private final UniMoveStreamContext<Solution_, A> context;
    private final RandomGenerator workingRandom;
    private @Nullable Iterator<UniTuple<A>> tupleIterator;

    private @Nullable Move<Solution_> nextMove;

    public UniRandomMoveIterator(UniMoveStreamContext<Solution_, A> context, RandomGenerator workingRandom) {
        this.context = Objects.requireNonNull(context);
        this.workingRandom = Objects.requireNonNull(workingRandom);
    }

    @Override
    public boolean hasNext() {
        if (nextMove != null) {
            return true;
        }
        if (tupleIterator == null) { // Only create a possibly expensive instance when we actually need it.
            tupleIterator = context.getDatasetInstance().randomIterator(Objects.requireNonNull(workingRandom));
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
