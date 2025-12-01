package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.NoSuchElementException;
import java.util.Random;

import org.jspecify.annotations.NullMarked;

/**
 * Exists to support random unique selection.
 * It accepts a list of unique items on input, and does not copy or modify it.
 * Instead, it keeps metadata on which indexes of the list were removed already, never to return them again.
 * Does not allow null values.
 * 
 * @param <T>
 */
@NullMarked
public sealed interface UniqueRandomSequence<T>
        permits DefaultUniqueRandomSequence, FilteredUniqueRandomSequence {

    /**
     * Picks a random element from the list which has not already been removed.
     * Once an element of the list is removed either via {@link #remove(Random)},
     * it will never be returned again by this method.
     *
     * @param workingRandom the random number generator to use
     * @return a random element from the list which has not already been removed
     * @throws NoSuchElementException if there are no more elements to pick from
     */
    SequenceElement<T> pick(Random workingRandom);

    /**
     * Removes a random element in the underlying list which has not already been removed.
     * Once this method returns, no subsequent {@link #pick(Random)} will return this element ever again.
     * 
     * @param workingRandom the random number generator to use
     * @return The element which exists in the original list at the removed index.
     * @throws NoSuchElementException if there are no more elements to pick from
     */
    T remove(Random workingRandom);

    /**
     * Returns whether there are no more elements to pick from.
     * In case of {@link FilteredUniqueRandomSequence}, this method may return false positives,
     * as it cannot predict how many elements will be filtered out.
     * 
     * @return true if there are no more elements to pick from, false otherwise
     */
    boolean isEmpty();

    record SequenceElement<T>(T value, int index) {
    }

}
