package ai.timefold.solver.core.impl.neighborhood;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.stream.Collectors;

import ai.timefold.solver.core.impl.neighborhood.move.MoveIterable;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class RandomOrderNeighborhoodIterator<Solution_> implements Iterator<Move<Solution_>> {

    private final List<Iterator<Move<Solution_>>> unexhaustedMoveIteratorList;
    private final Random workingRandom;

    private @Nullable Iterator<Move<Solution_>> currentMoveIterator;
    private boolean hasNext = true;

    public RandomOrderNeighborhoodIterator(List<MoveIterable<Solution_>> moveIterableList, Random workingRandom) {
        this.unexhaustedMoveIteratorList = moveIterableList.stream()
                .map(m -> m.iterator(workingRandom))
                .collect(Collectors.toList());
        this.workingRandom = workingRandom;
    }

    @Override
    public boolean hasNext() {
        if (!hasNext) {
            return false;
        }
        while (!unexhaustedMoveIteratorList.isEmpty()) {
            var randomIndex = workingRandom.nextInt(unexhaustedMoveIteratorList.size());
            currentMoveIterator = unexhaustedMoveIteratorList.get(randomIndex);
            if (currentMoveIterator.hasNext()) {
                hasNext = true;
                return true;
            } else {
                unexhaustedMoveIteratorList.remove(randomIndex);
            }
        }
        hasNext = false;
        return false;
    }

    @Override
    public Move<Solution_> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return currentMoveIterator.next(); // Guaranteed to iterate in random order.
    }
}
