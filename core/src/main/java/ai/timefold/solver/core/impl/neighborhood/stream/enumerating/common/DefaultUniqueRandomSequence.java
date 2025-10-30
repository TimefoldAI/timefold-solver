package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.jspecify.annotations.NullMarked;

/**
 * Keeps an exact track of which items were already removed from the sequence,
 * so that no item is ever returned twice.
 * It accepts a list of unique items on input, and does not copy or modify it.
 *
 * @param <T>
 */
@NullMarked
public final class DefaultUniqueRandomSequence<T> implements UniqueRandomSequence<T> {

    private static final DefaultUniqueRandomSequence<?> EMPTY = new DefaultUniqueRandomSequence<>(Collections.emptyList());

    @SuppressWarnings("unchecked")
    public static <T> DefaultUniqueRandomSequence<T> empty() {
        return (DefaultUniqueRandomSequence<T>) EMPTY;
    }

    private final List<T> originalList;
    private final int length;
    private final BitSet removed;

    private int removedCount;
    private int leftmostIndex;
    private int rightmostIndex;

    public DefaultUniqueRandomSequence(List<T> listOfUniqueItems) {
        this.originalList = Collections.unmodifiableList(listOfUniqueItems);
        this.length = listOfUniqueItems.size();
        this.removed = new BitSet(length);
        this.removedCount = 0;
        this.leftmostIndex = 0;
        this.rightmostIndex = length - 1;
    }

    @Override
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
        return pickIndex(workingRandom, randomIndex);
    }

    int pickIndex(Random workingRandom, int index) {
        if (removed.get(index)) {
            // use the closest index to avoid skewing the probability
            index = determineActiveIndex(workingRandom, index);
            if (index < 0 || index >= length) {
                throw new NoSuchElementException();
            }
        }
        return index;
    }

    private int determineActiveIndex(Random workingRandom, int randomIndex) {
        var nextClearIndex = removed.nextClearBit(randomIndex);
        var previousClearIndex = removed.previousClearBit(randomIndex);

        var nextIndexDistance = nextClearIndex >= length ? Integer.MAX_VALUE : nextClearIndex - randomIndex;
        var previousIndexDistance = previousClearIndex == -1 ? Integer.MAX_VALUE : randomIndex - previousClearIndex;

        // if the distance is equal, randomly choose between them,
        // otherwise return the one that is closer to the random index
        if (nextIndexDistance == previousIndexDistance) {
            return workingRandom.nextBoolean() ? nextClearIndex : previousClearIndex;
        }
        return nextIndexDistance < previousIndexDistance ? nextClearIndex : previousClearIndex;
    }

    @Override
    public T remove(Random workingRandom) {
        return remove(pickIndex(workingRandom));
    }

    @Override
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

    public boolean isEmpty() {
        return removedCount >= length;
    }

}
