package ai.timefold.solver.core.impl.neighborhood;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

import ai.timefold.solver.core.impl.neighborhood.move.MoveIterable;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class OriginalOrderNeighborhoodIterator<Solution_> implements Iterator<Move<Solution_>> {

    private final Queue<MoveIterable<Solution_>> moveIterableQueue;
    private @Nullable Iterator<Move<Solution_>> currentMoveIterator = null;

    public OriginalOrderNeighborhoodIterator(MoveIterable<Solution_>... moveIterables) {
        this.moveIterableQueue = new ArrayDeque<>(Arrays.asList(moveIterables));
    }

    @Override
    public boolean hasNext() {
        if (currentMoveIterator != null && currentMoveIterator.hasNext()) {
            return true;
        }
        while (!moveIterableQueue.isEmpty()) {
            MoveIterable<Solution_> nextIterable = moveIterableQueue.poll();
            currentMoveIterator = nextIterable.iterator();
            if (currentMoveIterator.hasNext()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Move<Solution_> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return currentMoveIterator.next();
    }
}
