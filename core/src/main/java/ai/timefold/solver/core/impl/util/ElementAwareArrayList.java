package ai.timefold.solver.core.impl.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * {@link AbstractList} implementation which allows for a cheap {@link Entry#remove() removal of an element},
 * while still providing fast iteration and random access.
 * The order of iteration is guaranteed to be the insertion order.
 * {@code null} is a valid value.
 * <p>
 * It uses internal state of the entry to track insertion position of the element.
 * When an entry is removed, its slot in the underlying collection is replaced with {@code null} (a gap);
 * therefore, the insertion position of later elements isn't changed.
 * Gaps are removed (the list is fully compacted) when {@link #forEach(Consumer)} or {@link #add(int, Object)} is called.
 * {@link #get(int)} and related index-based operations compact only the prefix up to the requested index.
 * This keeps the overhead low while giving us most benefits of {@link ArrayList}.
 * <p>
 * Primary fast-path methods are {@link #addEntry(Object)} and {@link Entry#remove()}, both run in O(1).
 * All standard {@link List} methods are also available and may run in O(n) or worse.
 * <p>
 * This class is so very not thread safe.
 *
 * @param <T>
 */
@NullMarked
public final class ElementAwareArrayList<T extends @Nullable Object> extends AbstractList<T> {

    private static final int REMOVED_POSITION = -1;

    private final List<@Nullable Entry> entryList = new ArrayList<>();
    private int lastElementPosition = -1;
    private int gapCount = 0; // Always equals the total number of null slots in entryList.

    /**
     * Appends the specified element to the end of this list.
     *
     * @return the entry for later O(1) removal via {@link Entry#remove()}
     */
    public Entry addEntry(T element) {
        modCount++;
        if (gapCount > 0 && entryList.get(lastElementPosition) == null) { // Reuse a gap if it exists.
            var newEntry = new Entry(element, lastElementPosition);
            entryList.set(lastElementPosition, newEntry);
            gapCount--;
            return newEntry;
        }
        var newEntry = new Entry(element, ++lastElementPosition);
        entryList.add(newEntry);
        return newEntry;
    }

    private Entry getEntry(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException(
                    "The index (%d) must be >= 0 and < size (%d).".formatted(index, size()));
        } else if (gapCount == 0) {
            return Objects.requireNonNull(entryList.get(index));
        }
        return partialCompact(index);
    }

    /**
     * Avoid calling this when {@code gapCount == 0}.
     */
    private Entry partialCompact(int rightBoundaryPosition) {
        var encounteredGaps = 0;
        var lastNonNullPosition = -1;
        for (var currentPosition = 0; currentPosition <= lastElementPosition; currentPosition++) {
            var entry = entryList.get(currentPosition);
            if (entry == null) {
                encounteredGaps++;
            } else {
                lastNonNullPosition++;
                if (encounteredGaps > 0) {
                    var targetPosition = currentPosition - encounteredGaps;
                    entry.moveTo(targetPosition);
                    entryList.set(targetPosition, entry);
                    entryList.set(currentPosition, null); // For consistency; the list is never in an invalid state.
                    modCount++;
                }
                if (lastNonNullPosition == rightBoundaryPosition) {
                    // Invariant: positions [0, index] are all non-null,
                    // so all gapCount nulls lie in [index+1, lastElementPosition].
                    // If that suffix is entirely nulls (equivalent to index == size()-1), trim it now.
                    if (gapCount == lastElementPosition - rightBoundaryPosition) {
                        entryList.subList(rightBoundaryPosition + 1, lastElementPosition + 1).clear();
                        lastElementPosition = rightBoundaryPosition;
                        gapCount = 0;
                        modCount++;
                    }
                    return entry;
                }
            }
        }
        throw new IndexOutOfBoundsException(
                "The index (%d) must be >= 0 and < size (%d).".formatted(rightBoundaryPosition, size()));
    }

    @Override
    public T get(int index) {
        return getEntry(index).element();
    }

    @Override
    public boolean add(T element) {
        addEntry(element);
        return true;
    }

    @Override
    @SuppressWarnings("DataFlowIssue")
    public void add(int index, T element) {
        var size = size();
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException(
                    "The index (%d) must be >= 0 and <= size (%d).".formatted(index, size));
        }
        if (index == size) {
            addEntry(element);
            return;
        }
        if (gapCount == 0) {
            modCount++;
            var newEntry = new Entry(element, index);
            entryList.add(index, newEntry);
            lastElementPosition++;
            for (var i = index + 1; i <= lastElementPosition; i++) {
                entryList.get(i).moveTo(i);
            }
            return;
        }
        // Compact prefix [0, index-1] so physical position k == logical position k for all k < index.
        if (index > 0) {
            partialCompact(index - 1); // Increases modCount.
        }
        var newEntry = new Entry(element, index);
        if (entryList.get(index) == null) {
            // Gap at the target position: fill it directly without shifting the array.
            entryList.set(index, newEntry);
            gapCount--;
        } else {
            // No gap at the target position: rotate entries rightward into the nearest gap in the suffix,
            // consuming that gap rather than growing the backing list.
            var displaced = newEntry;
            for (var i = index; i <= lastElementPosition; i++) {
                var current = entryList.get(i);
                displaced.moveTo(i);
                entryList.set(i, displaced);
                if (current == null) {
                    gapCount--;
                    break;
                }
                displaced = current;
            }
        }
    }

    @Override
    public T set(int index, T element) {
        return getEntry(index).replaceElement(element);
    }

    @Override
    public T remove(int index) {
        var entry = getEntry(index);
        var element = entry.element();
        remove(entry);
        return element;
    }

    /**
     * Removes the element referenced by the entry in O(1).
     *
     * @throws IllegalStateException if the entry was already removed
     */
    private void remove(Entry entry) {
        if (entry.isRemoved()) {
            throw new IllegalStateException("The entry (%s) was already removed."
                    .formatted(entry));
        }
        var positionPreRemoval = entry.position;
        if (positionPreRemoval == lastElementPosition) { // Removing the last element; just trim the list.
            entryList.remove(lastElementPosition--);
        } else {
            entryList.set(positionPreRemoval, null);
            gapCount++;
        }
        entry.moveTo(REMOVED_POSITION); // Mark the entry as removed.
        modCount++;
        clearIfPossible();
    }

    private void clearIfPossible() {
        if (gapCount == 0 || lastElementPosition + 1 != gapCount) {
            return;
        }
        // All positions are gaps. Clear the list entirely.
        entryList.clear();
        gapCount = 0;
        lastElementPosition = -1;
    }

    @Override
    public int size() {
        return lastElementPosition - gapCount + 1;
    }

    /**
     * Performs the given action for each element of the list
     * until all elements have been processed.
     *
     * @param action the action to be performed for each element;
     *        mustn't modify the list and mustn't throw exceptions,
     *        as that'd leave the list in an inconsistent state
     */
    @Override
    public void forEach(Consumer<? super T> action) {
        if (gapCount == 0) {
            forEachWithoutGaps(action);
        } else {
            // Compact the collection as we iterate.
            forEachCompacting(action);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private void forEachWithoutGaps(Consumer<? super T> elementConsumer) {
        for (var currentPosition = 0; currentPosition <= lastElementPosition; currentPosition++) {
            elementConsumer.accept(entryList.get(currentPosition).element());
        }
    }

    /**
     * Compacts during iteration.
     * Elements are moved to their new position (if needed) after the consumer is called on them,
     * so that the consumer sees the original insertion order.
     * Gaps end up at the end of the list, which is cleared in one go.
     *
     * @param elementConsumer to be executed over every element
     */
    private void forEachCompacting(Consumer<? super T> elementConsumer) {
        var liveCount = size();
        if (liveCount == 0) {
            clearIfPossible(); // The list may still contain gaps, so try to clear it entirely.
            return;
        }
        var compactPosition = 0;
        for (var currentPosition = 0; currentPosition <= lastElementPosition; currentPosition++) {
            var entry = entryList.get(currentPosition);
            if (entry == null) {
                continue;
            }
            elementConsumer.accept(entry.element());
            if (currentPosition != compactPosition) {
                entry.moveTo(compactPosition);
                entryList.set(compactPosition, entry);
                entryList.set(currentPosition, null); // Prevent stale data.
                modCount++;
            }
            if (++compactPosition == liveCount) {
                break;
            }
        }
        entryList.subList(compactPosition, lastElementPosition + 1).clear();
        lastElementPosition = compactPosition - 1;
        gapCount = 0;
        modCount++;
    }

    @Override
    public Iterator<T> iterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return new ElementAwareListIterator(index);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        return o instanceof List<?> other
                && this.size() == other.size()
                && super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode(); // Size not relevant; if sizes differ => lists do not equal => same hashCode is fine.
    }

    private final class ElementAwareListIterator implements ListIterator<T> {

        private int currentPosition;
        private int logicalPosition;
        private @Nullable Entry lastEntry;
        private boolean lastWasFwd;
        private int expectedModCount;

        private ElementAwareListIterator(int startingPosition) {
            var currentSize = size();
            if (startingPosition < 0 || startingPosition > currentSize) {
                throw new IndexOutOfBoundsException(
                        "The index (%d) must be >= 0 and <= size (%d).".formatted(startingPosition, currentSize));
            }
            if (startingPosition > 0 && gapCount > 0) {
                currentPosition = partialCompact(startingPosition - 1).position + 1;
            } else {
                currentPosition = startingPosition;
            }
            logicalPosition = startingPosition;
            expectedModCount = modCount;
        }

        @Override
        public boolean hasNext() {
            return logicalPosition < size();
        }

        @Override
        public boolean hasPrevious() {
            return logicalPosition > 0;
        }

        @Override
        public int nextIndex() {
            return logicalPosition;
        }

        @Override
        public int previousIndex() {
            return logicalPosition - 1;
        }

        @Override
        public T next() {
            checkModCount();
            if (logicalPosition >= size()) {
                throw new NoSuchElementException();
            }
            var entry = entryList.get(currentPosition);
            while (entry == null) {
                entry = entryList.get(++currentPosition);
            }
            currentPosition++;
            logicalPosition++;
            lastEntry = entry;
            lastWasFwd = true;
            return entry.element();
        }

        @Override
        public T previous() {
            checkModCount();
            if (logicalPosition <= 0) {
                throw new NoSuchElementException();
            }
            var entry = entryList.get(--currentPosition);
            while (entry == null) {
                entry = entryList.get(--currentPosition);
            }
            logicalPosition--;
            lastEntry = entry;
            lastWasFwd = false;
            return entry.element();
        }

        @Override
        public void remove() {
            if (lastEntry == null) {
                throw new IllegalStateException(
                        "remove() called without a preceding next() or previous().");
            }
            checkModCount();
            lastEntry.remove(); // Adjusts lastElementPosition.
            if (lastWasFwd) {
                logicalPosition--;
            }
            expectedModCount = modCount;
            lastEntry = null;
        }

        @Override
        public void set(T element) {
            if (lastEntry == null) {
                throw new IllegalStateException("set() called without a preceding next() or previous().");
            }
            checkModCount();
            lastEntry.replaceElement(element);
        }

        @Override
        public void add(T element) {
            checkModCount();
            ElementAwareArrayList.this.add(logicalPosition, element);
            logicalPosition++;
            currentPosition = logicalPosition;
            expectedModCount = modCount;
            lastEntry = null;
        }

        private void checkModCount() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }

    }

    public final class Entry implements ListEntry<T> {

        private T element; // Mutable so that ElementAwareArrayList.set(int, T) can be O(1).
        private int position; // Keeps the element's position in the list; must be kept in sync with its actual position.

        private Entry(T element, int position) {
            this.element = element;
            this.position = position;
        }

        public void remove() {
            ElementAwareArrayList.this.remove(this);
        }

        boolean isRemoved() {
            return position == REMOVED_POSITION;
        }

        void moveTo(int newPosition) {
            position = newPosition;
        }

        @Override
        public T element() {
            if (isRemoved()) {
                throw new IllegalStateException("The entry (%s) was already removed.".formatted(this));
            }
            return element;
        }

        public T replaceElement(T newElement) {
            var old = element();
            element = newElement;
            return old;
        }

        @Override
        public String toString() {
            return isRemoved() ? "null" : element + "@" + position;
        }

    }

}
