package ai.timefold.solver.core.impl.move;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.solver.random.UniformRandomChooser;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;

/**
 * Draws a fresh, independent move for every call to {@link #next()},
 * uniform over the unexhausted iterators themselves,
 * not weighted by how many moves each iterator can still produce.
 * <p>
 * Every chosen iterator has a next move:
 * {@link #of(RandomGenerator, List, BiFunction)} only admits iterators for which {@link Iterator#hasNext()} is true,
 * and {@link #next()} removes the iterator it drew from as soon as that iterator has no next move.
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

    private final UniformRandomChooser<Iterator<Move<Solution_>>> chooser;
    private final RandomGenerator workingRandom;

    private UniformRandomUnionMoveIterator(List<Iterator<Move<Solution_>>> moveIteratorList, RandomGenerator workingRandom) {
        this.chooser = new UniformRandomChooser<>(moveIteratorList, Iterator::hasNext);
        this.workingRandom = workingRandom;
    }

    @Override
    public boolean hasNext() {
        return !chooser.isEmpty();
    }

    @Override
    public Move<Solution_> next() {
        var moveIterator = chooser.choose(workingRandom);
        if (moveIterator == null) {
            throw new NoSuchElementException();
        }
        var move = moveIterator.next();
        if (!moveIterator.hasNext()) {
            chooser.removeChosen();
        }
        return move;
    }

}
