package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Iterator;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

import org.jspecify.annotations.NullMarked;

/**
 * Exists to support random unique selection.
 * It accepts a list of unique items on input, and does not copy or modify it.
 * Does not allow null values.
 * <p>
 * Every element for the underlying list is eventually returned exactly once,
 * and then the iterator ends, without any cooperation from the caller:
 * {@link #remove()} is not supported and never needs to be called.
 * See {@link RepeatingRandomIterator} for the never-ending alternative
 * used everywhere else by default,
 * and {@link RetiringRandomIterator} for the never-ending alternative
 * that lets the caller permanently drop an element from the pool
 * by calling {@link RetiringRandomIterator#retire()}.
 * <p>
 * It is imperative for the overall fairness of the solver that the picking is random and fair,
 * meaning each unpicked value has the same probability of being picked next.
 *
 * @param <T>
 */
@NullMarked
public sealed interface UniqueRandomIterator<T>
        extends Iterator<T>
        permits DefaultUniqueRandomIterator, ComparisonIndexer.RandomIterator, ContainedInIndexer.RandomIterator,
        ContainingAnyOfIndexer.RandomIterator {

    static <T> UniqueRandomIterator<T> of(ElementAwareArrayList<T> list, RandomGenerator random) {
        return new DefaultUniqueRandomIterator<>(RetiringRandomIterator.of(list, random));
    }

    static <T> UniqueRandomIterator<T> empty() {
        return new DefaultUniqueRandomIterator<>(RetiringRandomIterator.of(new ElementAwareArrayList<>(), () -> 0));
    }

    /**
     * Returns whether there are no more elements to pick from.
     *
     * @return true if there are no more elements to pick from, false otherwise
     */
    @Override
    boolean hasNext();

    /**
     * Picks a random element which has not already been returned by this iterator.
     * Once an element has been returned, it will never be returned again by this method.
     *
     * @return a random element which has not already been returned by this iterator
     */
    @Override
    T next();

}
