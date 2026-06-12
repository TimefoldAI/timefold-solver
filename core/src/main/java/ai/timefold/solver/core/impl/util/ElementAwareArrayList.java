package ai.timefold.solver.core.impl.util;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
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
 * This keeps the overhead low while giving us most benefits of an array-backed list.
 * <p>
 * Primary fast-path methods are {@link #addEntry(Object)} and {@link Entry#remove()}, both run in O(1).
 * All standard {@link List} methods are also available and may run in O(n) or worse.
 * <p>
 * This class is so very not thread safe.
 *
 * @param <T>
 */
@NullMarked
public final class ElementAwareArrayList<T extends @Nullable Object>
        extends AbstractList<T> {

    private static final Object[] EMPTY_ARRAY = new Object[0];
    private static final int REMOVED_POSITION = -1;

    private static final int DEFAULT_CAPACITY = 16;
    private static final int RETAIN_THRESHOLD = DEFAULT_CAPACITY; // Retain backing array when length <= this.
    private Object @Nullable [] entries = EMPTY_ARRAY;
    private int lastElementPosition = -1;
    private int gapCount = 0; // Always equals the total number of null slots in entryList.
    private int firstGapPosition = 0; // Pessimistic lower bound: positions [0, firstGapPosition) are guaranteed gap-free and positionally compact (logical i == physical i).

    /**
     * Appends the specified element to the end of this list.
     *
     * @return the entry for later O(1) removal via {@link Entry#remove()}
     */
    public Entry addEntry(T element) {
        modCount++;
        if (gapCount > 0 && entries[lastElementPosition] == null) { // Reuse a gap if it exists.
            var newEntry = new Entry(element, lastElementPosition);
            entries[lastElementPosition] = newEntry;
            gapCount--;
            return newEntry;
        }
        var newEntry = new Entry(element, ++lastElementPosition);
        resize(lastElementPosition + 1);
        entries[lastElementPosition] = newEntry;
        return newEntry;
    }

    private void resize(int minCapacity) {
        if (entries.length == 0) {
            entries = new Object[Math.max(DEFAULT_CAPACITY, minCapacity)];
            return;
        }
        if (minCapacity <= entries.length) {
            return;
        }
        entries = Arrays.copyOf(entries, Math.max(entries.length * 2, minCapacity));
    }

    /**
     * Returns the entry at the given physical position, or {@code null} if the slot is a gap.
     * Callers in fast-path loops can skip calling this and check {@code entries[i] == null} directly.
     */
    @SuppressWarnings("unchecked")
    private @Nullable Entry entryAt(int position) {
        return (Entry) entries[position];
    }

    @Override
    public T get(int index) {
        return getEntry(index).element();
    }

    private Entry getEntry(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException(
                    "The index (%d) must be >= 0 and < size (%d).".formatted(index, size()));
        } else if (gapCount == 0 || index < firstGapPosition) {
            return entryAt(index);
        }
        return partialCompact(index);
    }

    /**
     * Removes all gaps from the list in O(n), preserving insertion order.
     * After this call, {@code gapCount == 0} and every subsequent {@code get(int)} runs in O(1).
     * No-op when the list is already compact or empty.
     */
    void compact() {
        if (gapCount > 0 && !isEmpty()) {
            partialCompact(size() - 1);
        }
    }

    /**
     * Avoid calling this when {@code gapCount == 0}.
     */
    private Entry partialCompact(int rightBoundaryPosition) {
        if (rightBoundaryPosition < firstGapPosition) {
            // The entire target range is in the already-compacted prefix; no work needed.
            return entryAt(rightBoundaryPosition);
        }
        var encounteredGaps = 0;
        var lastNonNullPosition = firstGapPosition - 1; // firstGapPosition non-nulls are already in place before us.
        for (var currentPosition = firstGapPosition; currentPosition <= lastElementPosition; currentPosition++) {
            var entry = entryAt(currentPosition);
            if (entry == null) {
                encounteredGaps++;
            } else {
                lastNonNullPosition++;
                if (encounteredGaps > 0) {
                    var targetPosition = currentPosition - encounteredGaps;
                    entry.moveTo(targetPosition);
                    entries[targetPosition] = entry;
                    entries[currentPosition] = null; // For consistency; the list is never in an invalid state.
                    modCount++;
                }
                if (lastNonNullPosition == rightBoundaryPosition) {
                    // Invariant: positions [0, rightBoundaryPosition] are all non-null,
                    // so all gapCount nulls lie in [rightBoundaryPosition+1, lastElementPosition].
                    // If that suffix is entirely nulls (equivalent to rightBoundaryPosition == size()-1), trim it now.
                    if (gapCount == lastElementPosition - rightBoundaryPosition) {
                        truncateTo(rightBoundaryPosition);
                    } else {
                        firstGapPosition = rightBoundaryPosition + 1;
                    }
                    return entry;
                }
            }
        }
        throw new IndexOutOfBoundsException(
                "The index (%d) must be >= 0 and < size (%d).".formatted(rightBoundaryPosition, size()));
    }

    private void truncateTo(int newLastPosition) {
        if (newLastPosition < 0) {
            clear();
            return;
        }
        Arrays.fill(entries, newLastPosition + 1, lastElementPosition + 1, null);
        lastElementPosition = newLastPosition;
        gapCount = 0;
        firstGapPosition = lastElementPosition + 1; // [0, lastElementPosition] are all non-null.
        modCount++;
    }

    @Override
    public boolean add(T element) {
        addEntry(element);
        return true;
    }

    @Override
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
            addWithoutGaps(index, element);
            return;
        }
        // Compact prefix [0, index-1] so physical position k == logical position k for all k < index.
        if (index > 0) {
            partialCompact(index - 1); // Increases modCount.
        }
        if (entries[index] == null) {
            // Gap at the target position: fill it directly without shifting the array.
            entries[index] = new Entry(element, index);
            gapCount--;
        } else {
            // No gap at the target position: rotate entries rightward into the nearest gap in the suffix,
            // consuming that gap rather than growing the backing list.
            addWithGaps(index, new Entry(element, index));
        }
    }

    private void addWithoutGaps(int index, T element) {
        modCount++;
        var newEntry = new Entry(element, index);
        resize(lastElementPosition + 2);
        for (var i = lastElementPosition; i >= index; i--) {
            var shifted = entryAt(i);
            entries[i + 1] = shifted;
            shifted.moveTo(i + 1);
        }
        entries[index] = newEntry;
        lastElementPosition++;
    }

    private void addWithGaps(int index, Entry newEntry) {
        modCount++;
        var displaced = newEntry;
        for (var i = index; i <= lastElementPosition; i++) {
            var current = entryAt(i);
            displaced.moveTo(i);
            entries[i] = displaced;
            if (current == null) {
                gapCount--;
                break;
            }
            displaced = current;
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
        if (positionPreRemoval == lastElementPosition) { // Removing the last element; trim list and retract trailing gaps.
            entries[lastElementPosition--] = null;
            while (lastElementPosition >= 0 && entries[lastElementPosition] == null) {
                lastElementPosition--;
                gapCount--;
            }
        } else {
            entries[positionPreRemoval] = null;
            gapCount++;
            firstGapPosition = Math.min(firstGapPosition, positionPreRemoval);
        }
        entry.moveTo(REMOVED_POSITION); // Mark the entry as removed.
        modCount++;
        clearIfPossible();
    }

    private void clearIfPossible() {
        if (!isEmpty()) {
            return;
        }
        // List is empty: either retain the backing array (cheap re-add) or free it (large arrays).
        // Trailing-gap retraction in remove() guarantees lastElementPosition == -1, gapCount == 0,
        // and every slot already null — no fill is needed.
        var length = entries.length;
        if (length > 0 && length <= RETAIN_THRESHOLD) {
            lastElementPosition = -1; // Already -1; explicit for clarity.
            gapCount = 0; // Required: guards addEntry's gap-reuse branch (entries[-1]).
            firstGapPosition = 0; // Already 0; explicit for clarity.
        } else {
            innerClear();
        }
    }

    @Override
    public void clear() {
        innerClear();
        modCount++;
    }

    private void innerClear() {
        entries = EMPTY_ARRAY;
        gapCount = 0;
        lastElementPosition = -1;
        firstGapPosition = 0;
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
            elementConsumer.accept(entryAt(currentPosition).element); // entries[i] is provably non-null (gapCount==0)
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
            clear();
            return;
        }
        var compactPosition = 0;
        for (var currentPosition = 0; currentPosition <= lastElementPosition; currentPosition++) {
            var entry = entryAt(currentPosition);
            if (entry == null) {
                continue;
            }
            elementConsumer.accept(entry.element); // entry is provably live (post null-skip)
            if (currentPosition != compactPosition) {
                entry.moveTo(compactPosition);
                entries[compactPosition] = entry;
                entries[currentPosition] = null; // Prevent stale data.
                modCount++;
            }
            if (++compactPosition == liveCount) {
                break;
            }
        }
        truncateTo(compactPosition - 1);
    }

    @Override
    public Iterator<T> iterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<T> listIterator() {
        return this.listIterator(0);
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        compact(); // Ensure fast-path iteration; remove all gaps at once.
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
            var entry = entryAt(currentPosition);
            while (entry == null) {
                entry = entryAt(++currentPosition);
            }
            currentPosition++;
            logicalPosition++;
            lastEntry = entry;
            lastWasFwd = true;
            return entry.element; // provably live: entry is from a non-null slot after the null-skip loop
        }

        @Override
        public T previous() {
            checkModCount();
            if (logicalPosition <= 0) {
                throw new NoSuchElementException();
            }
            Entry entry = null;
            while (entry == null) {
                entry = entryAt(--currentPosition);
            }
            logicalPosition--;
            lastEntry = entry;
            lastWasFwd = false;
            return entry.element; // provably live: entry is from a non-null slot after the null-skip loop
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
