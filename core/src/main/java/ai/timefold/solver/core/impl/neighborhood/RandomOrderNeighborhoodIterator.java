package ai.timefold.solver.core.impl.neighborhood;

import java.util.Arrays;
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

    private final List<Iterator<Move<Solution_>>> moveIterators;
    private final Random workingRandom;

    private @Nullable Iterator<Move<Solution_>> currentMoveIterator;

    public RandomOrderNeighborhoodIterator(Random workingRandom, MoveIterable<Solution_>... moveIterables) {
        this.moveIterators = Arrays.stream(moveIterables)
                .map(m -> m.iterator(workingRandom))
                .collect(Collectors.toList());
        this.workingRandom = workingRandom;
    }

    @Override
    public boolean hasNext() {
        while (!moveIterators.isEmpty()) {
            var randomIndex = workingRandom.nextInt(moveIterators.size());
            currentMoveIterator = moveIterators.get(randomIndex);
            if (currentMoveIterator.hasNext()) {
                return true;
            } else {
                moveIterators.remove(randomIndex);
            }
        }
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
