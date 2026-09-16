package ai.timefold.solver.core.impl.move;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;

/**
 * Draws a fresh, independent move for every call to {@link #next()},
 * uniform over the unexhausted iterators in {@link #moveIteratorList} itself,
 * not weighted by how many moves each iterator can still produce.
 * <p>
 * Every iterator in {@link #moveIteratorList} is guaranteed to have a next move:
 * {@link #of(RandomGenerator, List, BiFunction) the constructor} only admits iterators
 * for which {@link Iterator#hasNext()} is true,
 * and {@link #next()} re-establishes that guarantee for the iterator it just drew from before returning.
 * This keeps {@link #hasNext()} an O(1) check instead of a sweep over every iterator.
 */
@NullMarked
public final class UniformRandomUnionMoveIterator<Solution_>
        implements Iterator<Move<Solution_>> {

    public static <Solution_, Source_> Iterator<Move<Solution_>> of(RandomGenerator workingRandom,
            List<Source_> sourceList, BiFunction<Source_, RandomGenerator, Iterator<Move<Solution_>>> extractorFunction) {
        var result = new ArrayList<Iterator<Move<Solution_>>>(sourceList.size());
        for (var i = 0; i < sourceList.size(); i++) { // No iterator created on hot path.
            var iterator = extractorFunction.apply(sourceList.get(i), workingRandom);
            if (iterator.hasNext()) {
                result.add(iterator);
            }
        }
        return switch (result.size()) {
            case 0 -> Collections.emptyIterator();
            case 1 -> result.getFirst();
            default -> new UniformRandomUnionMoveIterator<>(result, workingRandom);
        };
    }

    private final List<Iterator<Move<Solution_>>> moveIteratorList;
    private final RandomGenerator workingRandom;

    private UniformRandomUnionMoveIterator(List<Iterator<Move<Solution_>>> moveIteratorList, RandomGenerator workingRandom) {
        this.moveIteratorList = moveIteratorList;
        this.workingRandom = workingRandom;
    }

    @Override
    public boolean hasNext() {
        return !moveIteratorList.isEmpty();
    }

    @Override
    public Move<Solution_> next() {
        var size = moveIteratorList.size();
        return switch (size) {
            case 0 -> throw new NoSuchElementException();
            case 1 -> nextSingleIterator();
            default -> nextManyIterators(size);
        };
    }

    private Move<Solution_> nextSingleIterator() {
        var moveIterator = moveIteratorList.getFirst();
        var move = moveIterator.next();
        if (!moveIterator.hasNext()) {
            moveIteratorList.removeFirst();
        }
        return move;
    }

    private Move<Solution_> nextManyIterators(int size) {
        var randomIndex = workingRandom.nextInt(size);
        var moveIterator = moveIteratorList.get(randomIndex);
        var move = moveIterator.next();
        if (!moveIterator.hasNext()) {
            var lastIterator = moveIteratorList.removeLast();
            if (randomIndex != size - 1) {
                // Swap the last element into the gap left by the exhausted iterator,
                // instead of shifting the tail;
                // the list order carries no meaning.
                moveIteratorList.set(randomIndex, lastIterator);
            }
        }
        return move;
    }

}
