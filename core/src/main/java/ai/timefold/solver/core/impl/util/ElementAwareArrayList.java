package ai.timefold.solver.core.impl.util;

import java.util.AbstractList;
import java.util.Arrays;
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
 * Reads resolve around the gaps and never move anything; writes and full traversals compact:
 * <ul>
 * <li>{@link #forEach(Consumer)} and {@link #listIterator(int)} compact, as they visit every slot anyway.
 * <li>{@link #add(int, Object)} compacts, being a write which relocates entries regardless.
 * <li>{@link Entry#remove()} compacts once the gaps pass a quarter of {@link #size()},
 * which is what stops the backing array from growing without bound on a list nothing ever traverses.
 * <li>{@link #get(int)}, {@link #set(int, Object)} and {@link #entryAt(int)} never compact,
 * so they cannot relocate an element out from under a caller holding a physical slot.
 * </ul>
 * This keeps the overhead low while giving us most benefits of an array-backed list.
 * <p>
 * Primary fast-path methods are {@link #addEntry(Object)} and {@link Entry#remove()},
 * which run in O(1) and amortised O(1) respectively.
 * All standard {@link List} methods are also available and may run in O(n) or worse.
 * <p>
 * This class is so very not thread safe.
 * {@code modCount} is intentionally not maintained; iteration is not fail-fast (matches {@link ElementAwareLinkedList}).
 * Beyond that, callers holding a physical slot from {@link #entryAt(int)} across a mutation get no protection at all:
 * {@link Entry#remove()} may compact, which moves every surviving element to a different slot.
 * Consumers which keep a slot alive across a step, such as the neighborhood random iterators,
 * rely on the solver suppressing dataset mutation for the duration of move iteration.
 *
 * @param <T>
 */
@NullMarked
public final class ElementAwareArrayList<T extends @Nullable Object>
        extends AbstractList<T> {

    private static final Object[] EMPTY_ARRAY = new Object[0];
    private static final int REMOVED_POSITION = -1;

    private static final int DEFAULT_CAPACITY = 2;
    private static final int RETAIN_THRESHOLD = DEFAULT_CAPACITY; // Retain backing array when length <= this.
    private @Nullable Object[] entries = EMPTY_ARRAY;
    private int lastElementPosition = -1;
    private int gapCount = 0; // Always equals the total number of null slots in entryList.
    private int size = 0;

    /**
     * Appends the specified element to the end of this list.
     *
     * @return the entry for later O(1) removal via {@link Entry#remove()}
     */
    public Entry addEntry(T element) {
        var newPosition = ++lastElementPosition;
        if (newPosition == entries.length) { // Full (also covers EMPTY_ARRAY); grow on the cold path only.
            resize(newPosition + 1);
        }
        var newEntry = new Entry(element, newPosition);
        entries[newPosition] = newEntry;
        size++;
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
     * The width of the physical slot space, which is {@code >= size()} because removed elements leave gaps.
     * Every live element sits at some slot in {@code [0, slotCount())}; the rest of those slots are gaps.
     *
     * @return the number of physical slots, gaps included
     */
    public int slotCount() {
        return lastElementPosition + 1;
    }

    /**
     * Reads a physical slot directly, without compacting.
     * Unlike {@link #get(int)}, the argument is a physical slot and not a logical index;
     * use {@link #slotCount()}, not {@link #size()}, to bound it.
     * <p>
     * Returns the {@link Entry} rather than the element because {@code null} is a valid element value,
     * so only a {@code null} entry can unambiguously mean "gap".
     *
     * @param slot the physical slot to read
     * @return {@code null} if the slot is a gap
     */
    public @Nullable Entry entryAt(int slot) {
        if (slot < 0 || slot >= slotCount()) {
            throw slotOutOfBounds(slot);
        }
        return uncheckedEntryAt(slot);
    }

    @SuppressWarnings("unchecked")
    private @Nullable Entry uncheckedEntryAt(int slot) {
        return (Entry) entries[slot];
    }

    private Entry uncheckedNonNullEntryAt(int slot) {
        return Objects.requireNonNull(uncheckedEntryAt(slot));
    }

    /**
     * Builds {@link #entryAt(int)}'s exception in a method of its own,
     * because {@code .formatted()} boxes both arguments into a varargs array,
     * which is two thirds of the bytecode of a method whose actual work is one array read.
     * Inline, that pushes {@link #entryAt(int)} over the JIT's inlining threshold,
     * and it is drawn from once per random draw.
     * The other throw sites in this class are left inline on purpose:
     * no profile has flagged the methods they sit in.
     */
    private IndexOutOfBoundsException slotOutOfBounds(int slot) {
        return new IndexOutOfBoundsException(
                "The slot (%d) must be >= 0 and < slotCount (%d).".formatted(slot, slotCount()));
    }

    @Override
    public T get(int index) {
        return getEntry(index).element();
    }

    /**
     * Resolves a logical index to its entry without compacting,
     * so that reads never relocate an element out from under a caller holding a physical slot.
     * Runs in O(1) on a gapless list, and otherwise scans;
     * {@link Entry#remove()} keeps the scan bounded by capping {@link #slotCount()} at {@code 1.25 * size()}.
     */
    private Entry getEntry(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException(
                    "The index (%d) must be >= 0 and < size (%d).".formatted(index, size()));
        } else if (gapCount == 0) {
            return uncheckedNonNullEntryAt(index);
        }
        var remaining = index;
        for (var position = 0; position <= lastElementPosition; position++) {
            var entry = uncheckedEntryAt(position);
            if (entry != null && remaining-- == 0) {
                return entry;
            }
        }
        throw new IllegalStateException(
                "Impossible state: the index (%d) is below size (%d), but only %d elements were found."
                        .formatted(index, size(), index - remaining));
    }

    /**
     * Removes all gaps from the list in O(n), preserving insertion order.
     * After this call, {@code gapCount == 0} and every subsequent {@code get(int)} runs in O(1).
     * No-op when the list is already compact or empty.
     */
    void compact() {
        if (gapCount == 0 || isEmpty()) {
            return;
        }
        var compactPosition = 0;
        for (var currentPosition = 0; currentPosition <= lastElementPosition; currentPosition++) {
            var entry = uncheckedEntryAt(currentPosition);
            if (entry == null) {
                continue;
            }
            if (currentPosition != compactPosition) {
                entry.moveTo(compactPosition);
                entries[compactPosition] = entry;
                entries[currentPosition] = null; // Prevent stale data.
            }
            compactPosition++;
        }
        truncateTo(compactPosition - 1);
    }

    private void truncateTo(int newLastPosition) {
        if (newLastPosition < 0) {
            clear();
            return;
        }
        Arrays.fill(entries, newLastPosition + 1, lastElementPosition + 1, null);
        lastElementPosition = newLastPosition;
        gapCount = 0;
        size = newLastPosition + 1;
    }

    @Override
    public boolean add(T element) {
        addEntry(element);
        return true;
    }

    @Override
    public void add(int index, T element) {
        var currentSize = size;
        if (index < 0 || index > currentSize) {
            throw new IndexOutOfBoundsException(
                    "The index (%d) must be >= 0 and <= size (%d).".formatted(index, currentSize));
        }
        if (index == currentSize) {
            addEntry(element);
            return;
        }
        // Unlike the reads, this is already a relocating write, so compact rather than resolve around the gaps;
        // physical position k == logical position k afterwards.
        compact();
        addWithoutGaps(index, element);
    }

    private void addWithoutGaps(int index, T element) {
        var newEntry = new Entry(element, index);
        resize(lastElementPosition + 2);
        for (var i = lastElementPosition; i >= index; i--) {
            var shifted = uncheckedNonNullEntryAt(i);
            entries[i + 1] = shifted;
            shifted.moveTo(i + 1);
        }
        entries[index] = newEntry;
        lastElementPosition++;
        size++;
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
     * Removes the element referenced by the entry in amortised O(1).
     * Most calls only null out a slot, but a call which pushes the gap count past a quarter of {@link #size()}
     * also compacts, which is O(n) and relocates every surviving entry.
     * That bounds {@link #slotCount()} at {@code 1.25 * size()}, so the backing array cannot grow without bound
     * on a list which is never fully traversed.
     *
     * @throws IllegalStateException if the entry was already removed
     */
    private void remove(Entry entry) {
        var position = entry.position;
        if (position == REMOVED_POSITION) {
            throw new IllegalStateException("The entry (%s) was already removed."
                    .formatted(entry));
        }
        entry.moveTo(REMOVED_POSITION); // Mark the entry as removed.
        size--;
        entries[position] = null;
        if (position == lastElementPosition) { // Removing the last element; trim and retract trailing gaps.
            lastElementPosition--;
            while (lastElementPosition >= 0 && entries[lastElementPosition] == null) {
                lastElementPosition--;
                gapCount--;
            }
            if (lastElementPosition < 0) { // List now empty: retain a small backing array, free a large one.
                gapCount = 0; // Already 0 after retraction; explicit for clarity.
                if (entries.length > RETAIN_THRESHOLD) {
                    entries = EMPTY_ARRAY;
                }
            }
        } else { // Interior removal; cannot empty the list, so no empty-handling needed.
            gapCount++;
            if (gapCount * 4 > size) { // Gaps past a quarter of the live size; reclaim them in one O(n) pass.
                compact();
            }
        }
    }

    @Override
    public void clear() {
        innerClear();
    }

    private void innerClear() {
        entries = EMPTY_ARRAY;
        gapCount = 0;
        lastElementPosition = -1;
        size = 0;
    }

    @Override
    public int size() {
        return size;
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

    private void forEachWithoutGaps(Consumer<? super T> elementConsumer) {
        for (var currentPosition = 0; currentPosition <= lastElementPosition; currentPosition++) {
            elementConsumer.accept(uncheckedNonNullEntryAt(currentPosition).element); // entries[i] is provably non-null (gapCount==0)
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
            var entry = uncheckedEntryAt(currentPosition);
            if (entry == null) {
                continue;
            }
            elementConsumer.accept(entry.element); // entry is provably live (post null-skip)
            if (currentPosition != compactPosition) {
                entry.moveTo(compactPosition);
                entries[compactPosition] = entry;
                entries[currentPosition] = null; // Prevent stale data.
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

        private ElementAwareListIterator(int startingPosition) {
            var currentSize = size();
            if (startingPosition < 0 || startingPosition > currentSize) {
                throw new IndexOutOfBoundsException(
                        "The index (%d) must be >= 0 and <= size (%d).".formatted(startingPosition, currentSize));
            }
            // listIterator() compacts before construction ⟹ gapless: logical position == physical position.
            currentPosition = startingPosition;
            logicalPosition = startingPosition;
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
            if (logicalPosition >= size()) {
                throw new NoSuchElementException();
            }
            var entry = uncheckedEntryAt(currentPosition);
            while (entry == null) {
                entry = uncheckedEntryAt(++currentPosition);
            }
            currentPosition++;
            logicalPosition++;
            lastEntry = entry;
            lastWasFwd = true;
            return entry.element; // provably live: entry is from a non-null slot after the null-skip loop
        }

        @Override
        public T previous() {
            if (logicalPosition <= 0) {
                throw new NoSuchElementException();
            }
            Entry entry = null;
            while (entry == null) {
                entry = uncheckedEntryAt(--currentPosition);
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
            lastEntry.remove(); // Adjusts lastElementPosition; may also compact.
            if (lastWasFwd) {
                logicalPosition--;
            }
            if (gapCount == 0) {
                // A compaction may have relocated every entry; the list is gapless, so physical == logical again.
                currentPosition = logicalPosition;
            }
            lastEntry = null;
        }

        @Override
        public void set(T element) {
            if (lastEntry == null) {
                throw new IllegalStateException("set() called without a preceding next() or previous().");
            }
            lastEntry.replaceElement(element);
        }

        @Override
        public void add(T element) {
            var appending = logicalPosition == size();
            ElementAwareArrayList.this.add(logicalPosition, element);
            logicalPosition++;
            // Appended entries land at physical lastElementPosition (may exceed logicalPosition when gaps exist);
            // interior inserts land at physical logicalPosition (prefix is compacted by add(int,T)).
            currentPosition = appending ? lastElementPosition + 1 : logicalPosition;
            lastEntry = null;
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
