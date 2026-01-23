package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.BitSet;
import java.util.NoSuchElementException;
import java.util.Random;

import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Keeps an exact track of which items were already removed from the sequence,
 * so that no item is ever returned twice.
 * It accepts a list of unique items on input, and does not copy or modify it.
 *
 * @param <T>
 */
@NullMarked
final class DefaultUniqueRandomSequence<T> implements UniqueRandomSequence<T> {

    private final ElementAwareArrayList<T> source;
    private final int length;
    private final Random workingRandom;
    private final BitSet removed;

    private int removedCount;
    private int leftmostIndex;
    private int rightmostIndex;

    private int nextIndex = -1;
    private @Nullable T next = null;
    private int indexToOptionallyRemove = -1;

    DefaultUniqueRandomSequence(ElementAwareArrayList<T> source, Random workingRandom) {
        this.source = source;
        this.length = source.size();
        this.removed = new BitSet(); // Do not size upfront, we may only remove a few elements.
        this.workingRandom = workingRandom;
        this.removedCount = 0;
        this.leftmostIndex = 0;
        this.rightmostIndex = length - 1;
    }

    @Override
    public boolean hasNext() {
        if (source.isEmpty() || removedCount >= length) {
            return false;
        }
        if (nextIndex != -1) {
            return true;
        }
        // Pick a random index from the underlying list.
        // If the index has already been removed, find the next closest active one.
        // If no such index is found, pick the previous closest active one.
        // This algorithm ensures that we do not pick the same index twice.
        var randomIndex = workingRandom.nextInt(leftmostIndex, rightmostIndex + 1);
        nextIndex = pickIndex(workingRandom, randomIndex);
        next = source.get(nextIndex).element();
        indexToOptionallyRemove = -1;
        return true;
    }

    private int pickIndex(Random workingRandom, int index) {
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
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        indexToOptionallyRemove = nextIndex;
        var returnValue = next;
        nextIndex = -1;
        next = null;
        return returnValue;
    }

    @Override
    public void remove() {
        if (indexToOptionallyRemove == -1) {
            throw new IllegalStateException(
                    "The next() method has not been called yet, or the remove() method was already called after the last next() call.");
        }
        removedCount++;
        removed.set(indexToOptionallyRemove);
        // Update the leftmost and rightmost zero index to keep probability distribution even.
        if (indexToOptionallyRemove == leftmostIndex) {
            leftmostIndex = removed.nextClearBit(leftmostIndex);
        }
        if (indexToOptionallyRemove == rightmostIndex) {
            rightmostIndex = removed.previousClearBit(rightmostIndex);
        }
        indexToOptionallyRemove = -1;
    }
}
