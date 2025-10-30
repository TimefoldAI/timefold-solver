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
     * Once an element of the list is removed either via {@link #remove(Random)} or {@link #remove(int)},
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
     * Removes the element at the given index in the underlying list.
     * Once this method returns, no subsequent {@link #pick(Random)} will return this element ever again.
     * 
     * @param index the index of the element to remove
     * @return The element which exists in the original list at the given index.
     * @throws NoSuchElementException if the index has already been removed
     */
    T remove(int index);

    record SequenceElement<T>(T value, int index) {
    }

}
