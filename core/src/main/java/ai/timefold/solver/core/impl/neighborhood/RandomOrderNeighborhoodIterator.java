package ai.timefold.solver.core.impl.neighborhood;

import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveIterable;
import org.jspecify.annotations.NullMarked;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

/**
 * Draws a fresh, independent neighborhood for every move,
 * uniform over {@link #unexhaustedMoveIteratorList} itself,
 * not weighted by how many moves each neighborhood can still produce.
 * <p>
 * {@link #hasNext()} prunes every exhausted neighborhood from {@link #unexhaustedMoveIteratorList} before answering,
 * rather than lazily discovering exhaustion only when {@link #next()} draws one.
 */
@NullMarked
final class RandomOrderNeighborhoodIterator<Solution_> implements Iterator<Move<Solution_>> {

    private final List<Iterator<Move<Solution_>>> unexhaustedMoveIteratorList;
    private final RandomGenerator workingRandom;

    public RandomOrderNeighborhoodIterator(List<MoveIterable<Solution_>> moveIterableList, RandomGenerator workingRandom) {
        this.unexhaustedMoveIteratorList = moveIterableList.stream()
                .map(m -> m.iterator(workingRandom))
                .collect(Collectors.toList());
        this.workingRandom = workingRandom;
    }

    @Override
    public boolean hasNext() {
        unexhaustedMoveIteratorList.removeIf(moveIterator -> !moveIterator.hasNext());
        return !unexhaustedMoveIteratorList.isEmpty();
    }

    @Override
    public Move<Solution_> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        var randomIndex = workingRandom.nextInt(unexhaustedMoveIteratorList.size());
        return unexhaustedMoveIteratorList.get(randomIndex).next();
    }

    @Override
    public void forEachRemaining(Consumer<? super Move<Solution_>> action) {
        // Effectively never ends as long as one of the wrapped move iterators does not.
        throw new UnsupportedOperationException("""
                This iterator does not end, so forEachRemaining() cannot terminate.
                Maybe use hasNext() and next() with your own stop condition instead.""");
    }
}
