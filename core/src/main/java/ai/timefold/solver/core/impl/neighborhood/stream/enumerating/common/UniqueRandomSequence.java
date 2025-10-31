package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Exists to support random unique selection.
 * It accepts a list of unique items on input, and does not copy or modify it.
 * Instead, it keeps metadata on which indexes of the list were removed already, never to return them again.
 * Does not allow null values.
 * 
 * @param <T>
 */
@NullMarked
public final class UniqueRandomSequence<T> {

    private static final UniqueRandomSequence<?> EMPTY = new UniqueRandomSequence<>(Collections.emptyList());

    @SuppressWarnings("unchecked")
    public static <T> UniqueRandomSequence<T> empty() {
        return (UniqueRandomSequence<T>) EMPTY;
    }

    private final List<T> originalList;
    private final int length;
    private final BitSet removed;

    private int removedCount;
    private int leftmostIndex;
    private int rightmostIndex;

    public UniqueRandomSequence(List<T> listOfUniqueItems) {
        this(listOfUniqueItems, null);
    }

    public UniqueRandomSequence(List<T> listOfUniqueItems, @Nullable BitSet ignoredIndexesSet) {
        this.originalList = Collections.unmodifiableList(listOfUniqueItems);
        this.length = listOfUniqueItems.size();
        this.removed = new BitSet(length);
        if (ignoredIndexesSet != null) {
            this.removed.or(ignoredIndexesSet); // Defensive copy.
            this.removedCount = removed.cardinality();
            this.leftmostIndex = removed.nextClearBit(0);
            this.rightmostIndex = removed.previousClearBit(length - 1);
        } else {
            this.removedCount = 0;
            this.leftmostIndex = 0;
            this.rightmostIndex = length - 1;
        }
    }

    /**
     * Picks a random element from the list which has not already been removed.
     * Once an element of the list is removed either via {@link #remove(Random)} or {@link #remove(int)},
     * it will never be returned again by this method.
     * 
     * @param workingRandom the random number generator to use
     * @return a random element from the list which has not already been removed
     * @throws NoSuchElementException if there are no more elements to pick from
     */
    public SequenceElement<T> pick(Random workingRandom) {
        var randomIndex = pickIndex(workingRandom);
        return new SequenceElement<>(originalList.get(randomIndex), randomIndex);
    }

    private int pickIndex(Random workingRandom) {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }

        // Pick a random index from the underlying list.
        // If the index has already been removed, find the next closest active one.
        // If no such index is found, pick the previous closest active one.
        // This algorithm ensures that we do not pick the same index twice.
        var randomIndex = workingRandom.nextInt(leftmostIndex, rightmostIndex + 1);
        if (removed.get(randomIndex)) {
            // use the closest index to avoid skewing the probability
            randomIndex = determineActiveIndex(workingRandom, randomIndex);
            if (randomIndex < 0) {
                throw new IllegalStateException(
                        "Impossible state: The index cannot be negative, was (%s).".formatted(randomIndex));
            }
        }
        return randomIndex;
    }

    private int determineActiveIndex(Random workingRandom, int randomIndex) {
        var nextClearIndex = removed.nextClearBit(randomIndex);
        var previousClearIndex = removed.previousClearBit(randomIndex);

        // check if both next and previous clear indexes are valid
        if (nextClearIndex >= length) {
            throw new IllegalStateException(
                    "Impossible state: There is no clear bit after the index (%s).".formatted(randomIndex));
        }
        if (previousClearIndex == -1) {
            throw new IllegalStateException(
                    "Impossible state: There is no clear bit before the index (%s).".formatted(randomIndex));
        }

        var nextIndexDistance = nextClearIndex - randomIndex;
        var previousIndexDistance = randomIndex - previousClearIndex;

        // if the distance is equal, randomly choose between them,
        // otherwise return the one that is closer to the random index
        if (nextIndexDistance == previousIndexDistance) {
            return workingRandom.nextBoolean() ? nextClearIndex : previousClearIndex;
        }
        return nextIndexDistance < previousIndexDistance ? nextClearIndex : previousClearIndex;
    }

    /**
     * Removes a random element in the underlying list which has not already been removed.
     * Once this method returns, no subsequent {@link #pick(Random)} will return this element ever again.
     * 
     * @param workingRandom the random number generator to use
     * @return The element which exists in the original list at the removed index.
     * @throws NoSuchElementException if there are no more elements to pick from
     */
    public T remove(Random workingRandom) {
        return remove(pickIndex(workingRandom));
    }

    /**
     * Removes the element at the given index in the underlying list.
     * Once this method returns, no subsequent {@link #pick(Random)} will return this element ever again.
     * 
     * @param index the index of the element to remove
     * @return The element which exists in the original list at the given index.
     * @throws IllegalArgumentException if the index has already been removed
     */
    public T remove(int index) {
        if (removed.get(index)) {
            throw new IllegalArgumentException("The index (%s) has already been removed.".formatted(index));
        }
        removed.set(index);
        removedCount++;

        // update the leftmost and rightmost zero index to keep probability distribution even
        if (index == leftmostIndex) {
            leftmostIndex = removed.nextClearBit(leftmostIndex);
        }
        if (index == rightmostIndex) {
            rightmostIndex = removed.previousClearBit(rightmostIndex);
        }
        return originalList.get(index);
    }

    /**
     * @return true if all elements have been removed already, or if there were no elements in the first place.
     */
    public boolean isEmpty() {
        return removedCount >= length;
    }

    public record SequenceElement<T>(T value, int index) {
    }

}
