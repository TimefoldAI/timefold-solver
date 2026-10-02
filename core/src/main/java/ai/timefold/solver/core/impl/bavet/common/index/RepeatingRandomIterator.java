package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

import org.jspecify.annotations.NullMarked;

/**
 * Exists to support random selection with replacement.
 * It accepts a list of items on input, and does not copy or modify it.
 * Does not allow null values.
 * <p>
 * Unlike {@link UniqueRandomIterator},
 * this iterator never ends (unless the underlying list is empty)
 * and may return the same element more than once.
 * {@link #remove()} is not supported.
 * {@link #forEachRemaining(Consumer)} is not supported, as this iterator may never end.
 * Every unpicked and previously picked value has the same probability of being picked next,
 * so the overall selection stays fair even though it may repeat.
 *
 * @param <T>
 */
@NullMarked
public sealed interface RepeatingRandomIterator<T>
        extends Iterator<T>
        permits DefaultRepeatingRandomIterator, WeightedRepeatingIterator {

    static <T> RepeatingRandomIterator<T> of(ElementAwareArrayList<T> list, RandomGenerator random) {
        return new DefaultRepeatingRandomIterator<>(list, random);
    }

    static <T> RepeatingRandomIterator<T> empty() {
        return DefaultRepeatingRandomIterator.empty();
    }

    /**
     * Returns whether there are any elements to pick from at all.
     * Unlike {@link UniqueRandomIterator#hasNext()},
     * this never turns {@code false} just because elements were already picked;
     * only an empty source does that.
     */
    @Override
    boolean hasNext();

    /**
     * Picks a random element from the list.
     * The same element may be returned again by a later call.
     */
    @Override
    T next();

    /**
     * Not supported: this iterator never ends by itself,
     * so {@code forEachRemaining()} cannot terminate.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    default void forEachRemaining(Consumer<? super T> action) {
        throw new UnsupportedOperationException("""
                The random iterator (%s) never ends, so forEachRemaining() cannot terminate.
                Maybe use hasNext() and next() with your own stop condition, \
                or ask for a unique random iterator instead."""
                .formatted(this));
    }

}
