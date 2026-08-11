package ai.timefold.solver.core.impl.neighborhood;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

import ai.timefold.solver.core.impl.bavet.common.index.UniqueRandomIterator;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveIterable;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Removes a move iterator from {@link #unexhaustedMoveIteratorList} only once it reports no next move,
 * which happens for an empty dataset,
 * or for a join where no left tuple has a live right side,
 * or for an exhausted {@link UniqueRandomIterator}.
 * Otherwise, this iterator is effectively endless.
 */
@NullMarked
final class RandomOrderNeighborhoodIterator<Solution_> implements Iterator<Move<Solution_>> {

    private final List<Iterator<Move<Solution_>>> unexhaustedMoveIteratorList;
    private final RandomGenerator workingRandom;

    private @Nullable Iterator<Move<Solution_>> currentMoveIterator;

    public RandomOrderNeighborhoodIterator(List<MoveIterable<Solution_>> moveIterableList, RandomGenerator workingRandom) {
        this.unexhaustedMoveIteratorList = moveIterableList.stream()
                .map(m -> m.iterator(workingRandom))
                .collect(Collectors.toList());
        this.workingRandom = workingRandom;
    }

    @Override
    public boolean hasNext() {
        if (currentMoveIterator != null && currentMoveIterator.hasNext()) {
            return true;
        }
        while (!unexhaustedMoveIteratorList.isEmpty()) {
            var randomIndex = workingRandom.nextInt(unexhaustedMoveIteratorList.size());
            currentMoveIterator = unexhaustedMoveIteratorList.get(randomIndex);
            if (currentMoveIterator.hasNext()) {
                return true;
            } else {
                unexhaustedMoveIteratorList.remove(randomIndex);
            }
        }
        currentMoveIterator = null;
        return false;
    }

    @Override
    public Move<Solution_> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return currentMoveIterator.next(); // Guaranteed to iterate in random order.
    }

    @Override
    public void forEachRemaining(Consumer<? super Move<Solution_>> action) {
        // Effectively never ends as long as one of the wrapped move iterators does not.
        throw new UnsupportedOperationException("""
                This iterator does not end, so forEachRemaining() cannot terminate.
                Maybe use hasNext() and next() with your own stop condition instead.""");
    }
}
