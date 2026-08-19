package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.NoSuchElementException;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

import org.jspecify.annotations.NullMarked;

/**
 * Picks a random element out of the given list, with replacement, forever.
 * It accepts a list of items on input, and does not copy or modify it.
 *
 * @param <T>
 */
@NullMarked
final class DefaultRepeatingRandomIterator<T> implements RepeatingRandomIterator<T> {

    private static final DefaultRepeatingRandomIterator<Object> EMPTY =
            new DefaultRepeatingRandomIterator<>(new ElementAwareArrayList<>(), () -> 0);

    public static <T> RepeatingRandomIterator<T> empty() {
        return (RepeatingRandomIterator<T>) EMPTY;
    }

    private final ElementAwareArrayList<T> source;
    private final RandomGenerator workingRandom;

    DefaultRepeatingRandomIterator(ElementAwareArrayList<T> source, RandomGenerator workingRandom) {
        this.source = source;
        this.workingRandom = workingRandom;
    }

    @Override
    public boolean hasNext() {
        return !source.isEmpty();
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return source.get(workingRandom.nextInt(source.size()));
    }

    // No remove()/forEachRemaining() overrides: RepeatingRandomIterator's defaults already throw.

}
