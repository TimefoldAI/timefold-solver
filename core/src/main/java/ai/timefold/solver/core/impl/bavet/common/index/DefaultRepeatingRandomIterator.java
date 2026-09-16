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
        // Draw over the physical slots and reject gaps,
        // instead of asking for a logical index;
        // the latter would make the list compact just to resolve that index.
        var slotCount = source.slotCount();
        ElementAwareArrayList<T>.Entry entry;
        do {
            entry = source.entryAt(workingRandom.nextInt(slotCount));
        } while (entry == null);
        return entry.element();
    }

    // No remove()/forEachRemaining() overrides: RepeatingRandomIterator's defaults already throw.

}
