package ai.timefold.solver.core.impl.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.NullMarked;

/**
 * {@link ArrayList}-backed list which allows for a cheap {@link #remove(Entry) removal of an element},
 * while still providing fast iteration and random access.
 * The order of iteration is guaranteed to be the insertion order.
 * <p>
 * It uses internal state of the entry to track insertion position of the element.
 * When an element is removed, the underlying collection isn't actually touched;
 * therefore, the insertion position of later elements isn't changed.
 * This position is called a gap.
 * Gaps are removed (the list is compacted) when {@link #forEach(Consumer)} or {@link #asList()} is called.
 * This keeps the overhead low while giving us all benefits of {@link ArrayList},
 * such as memory efficiency, random access, and fast iteration.
 * <p>
 * This class is very thread-unsafe.
 *
 * @param <T>
 */
@NullMarked
public final class ElementAwareArrayList<T> {

    private final List<Entry<T>> elementList = new ArrayList<>();
    private int lastElementPosition = -1;
    private int gapCount = 0;

    /**
     * Appends the specified element to the end of this collection.
     *
     * @param element element to be appended to this collection
     */
    public Entry<T> add(T element) {
        var newEntry = new Entry<>(element, ++lastElementPosition);
        elementList.add(newEntry);
        return newEntry;
    }

    /**
     * Removes the first occurrence of the specified element from this collection, if present.
     *
     * @param entry entry to be removed from this collection
     * @throws IllegalStateException if the element wasn't found in this collection
     */
    public void remove(Entry<T> entry) {
        if (entry.isRemoved()) {
            throw new IllegalStateException("The entry (%s) was already removed."
                    .formatted(entry));
        }
        // For performance, we do not touch the list.
        // Instead, we mark it as a gap by setting its position to -1.
        // Technically, this is a memory leak of the value;
        // in practice, compaction will clean it up relatively quickly.
        entry.position = -1;
        gapCount++;
        clearIfPossible();
    }

    private boolean clearIfPossible() {
        if (gapCount > 0 && lastElementPosition + 1 == gapCount) { // All positions are gaps. Clear the list entirely.
            forceClear();
            return true;
        }
        return false;
    }

    private void forceClear() {
        elementList.clear();
        gapCount = 0;
        lastElementPosition = -1;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public int size() {
        return lastElementPosition - gapCount + 1;
    }

    /**
     * Performs the given action for each element of the collection
     * until all elements have been processed.
     *
     * @param elementConsumer the action to be performed for each element;
     *        mustn't modify the collection and mustn't throw exceptions,
     *        as that'd leave the collection in an inconsistent state
     * 
     */
    public void forEach(Consumer<T> elementConsumer) {
        if (gapCount == 0) {
            forEachWithoutGaps(elementConsumer);
        } else {
            // Compact the collection as we iterate.
            forEachCompacting(elementConsumer);
        }
    }

    private void forEachWithoutGaps(Consumer<T> elementConsumer) {
        for (var i = 0; i <= lastElementPosition; i++) {
            elementConsumer.accept(elementList.get(i).getElement());
        }
    }

    private void forEachCompacting(Consumer<T> elementConsumer) {
        if (clearIfPossible()) {
            return;
        }
        var indexesToRemove = new int[gapCount]; // Prevents iterating the entire list multiple times.
        var encounteredGaps = 0;
        for (var i = 0; i <= lastElementPosition; i++) {
            var element = elementList.get(i);
            if (element.isRemoved()) {
                if (clearTailGapsIfPossible(i, encounteredGaps + 1, indexesToRemove)) {
                    return;
                } else {
                    indexesToRemove[encounteredGaps++] = i;
                }
            } else {
                elementConsumer.accept(element.getElement());
                if (encounteredGaps > 0) {
                    element.position = i - encounteredGaps;
                }
            }
        }
        clearGaps(indexesToRemove);
    }

    /**
     * Clears all remaining gaps if all remaining elements are gaps.
     * 
     * @param currentGapPosition the current position in the iteration
     * @param encounteredGaps the number of gaps encountered so far in the iteration, including the current one
     * @param indexesToRemove the array of indexes where gaps were previously found; won't be modified
     * @return true if all remaining elements were gaps and have been cleared
     */
    private boolean clearTailGapsIfPossible(int currentGapPosition, int encounteredGaps, int... indexesToRemove) {
        var remainingGaps = gapCount - encounteredGaps;
        if (remainingGaps == 0) {
            return false;
        }
        var remainingElements = lastElementPosition - currentGapPosition;
        if (remainingElements == remainingGaps) { // All remaining elements are gaps; we can stop here.
            elementList.subList(currentGapPosition, lastElementPosition + 1).clear();
            clearGaps(indexesToRemove, encounteredGaps - 2); // -2 because current gap is not yet in the array.
            return true;
        } else if (remainingElements > remainingGaps) { // There are still non-gap elements remaining.
            return false;
        } else {
            throw new IllegalStateException(
                    "Impossible state: the number of remaining elements (%d) is less than the number of remaining gaps (%d)."
                            .formatted(remainingElements, remainingGaps));
        }
    }

    private void clearGaps(int[] gaps, int topMostIndexToInclude) {
        // Remove gaps from the back to shift only as little as we need.
        for (var index = topMostIndexToInclude; index >= 0; index--) {
            elementList.remove(gaps[index]);
        }
        lastElementPosition = elementList.size() - 1;
        gapCount = 0;
    }

    private void clearGaps(int[] indexesToRemove) {
        clearGaps(indexesToRemove, indexesToRemove.length - 1);
    }

    /**
     * Returns a standard {@link List} view of this collection.
     * Users mustn't modify the returned list, as that'd also change the underlying data structure.
     *
     * @return a standard list view of this element-aware list
     */
    public List<Entry<T>> asList() {
        if (isEmpty() || compact()) {
            return Collections.emptyList();
        }
        return elementList;
    }

    private boolean compact() {
        if (gapCount == 0) {
            return isEmpty();
        }
        if (clearIfPossible()) {
            return true;
        }
        var indexesToRemove = new int[gapCount]; // Prevents iterating the entire list multiple times.
        var encounteredGaps = 0;
        for (var i = 0; i <= lastElementPosition; i++) {
            var element = elementList.get(i);
            if (element.isRemoved()) {
                if (clearTailGapsIfPossible(i, encounteredGaps + 1, indexesToRemove)) {
                    return false;
                } else {
                    indexesToRemove[encounteredGaps++] = i;
                }
            } else if (encounteredGaps > 0) {
                element.position = i - encounteredGaps;
            }
        }
        clearGaps(indexesToRemove);
        return false;
    }

    public static final class Entry<T> implements ListEntry<T> {

        private final T element;
        private int position;

        private Entry(T element, int position) {
            this.element = element;
            this.position = position;
        }

        @Override
        public boolean isRemoved() {
            return position < 0;
        }

        @Override
        public T getElement() {
            return element;
        }

        @Override
        public String toString() {
            return isRemoved() ? "null" : element + "@" + position;
        }

    }

}