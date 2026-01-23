package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Iterator;

import org.jspecify.annotations.NullMarked;

/**
 * Exists to support random unique selection.
 * It accepts a list of unique items on input, and does not copy or modify it.
 * Instead, it keeps metadata on which indexes of the list were removed already, never to return them again.
 * Does not allow null values.
 * <p>
 * It is imperative for the overall fairness of the solver that the picking is random and fair,
 * meaning each unpicked value has the same probability of being picked next.
 * 
 * @param <T>
 */
@NullMarked
public sealed interface UniqueRandomSequence<T>
        extends Iterator<T>
        permits DefaultUniqueRandomSequence, FilteredUniqueRandomSequence {

    /**
     * Returns whether there are no more elements to pick from.
     * In case of {@link FilteredUniqueRandomSequence}, this method may return false positives,
     * as it cannot predict how many elements will be filtered out.
     *
     * @return true if there are no more elements to pick from, false otherwise
     */
    @Override
    boolean hasNext();

    /**
     * Picks a random element from the list which has not already been removed.
     * Once an element of the list is removed either via {@link #remove()},
     * it will never be returned again by this method.
     *
     * @return a random element from the list which has not already been removed
     */
    @Override
    T next();

    /**
     * Removes a random element in the underlying list which has not already been removed.
     * Once this method returns, no subsequent {@link #next()} will return this element ever again.
     */
    @Override
    void remove();

}
