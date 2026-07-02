package ai.timefold.solver.core.impl.bavet.common.tuple;

import java.util.function.Consumer;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Doubly-linked list for tuples that stores prev/next links in the tuple's own store slots.
 * No per-element object is allocated; the join node reserves two out-slots per side for the links.
 * One {@code TupleList} head is created per input-tuple insert (not per match).
 *
 * @param <T> The element type. Must be a {@link Tuple} so its store can hold the link pointers.
 */
@NullMarked
public final class TupleList<T extends Tuple> {

    private final int prevStoreIndex;
    private final int nextStoreIndex;
    private @Nullable T first;
    private @Nullable T last;
    private int size;
    private @Nullable T mark;
    private long markVersion;

    public TupleList(int prevStoreIndex, int nextStoreIndex) {
        this.prevStoreIndex = prevStoreIndex;
        this.nextStoreIndex = nextStoreIndex;
    }

    public void add(T tuple) {
        tuple.setStore(prevStoreIndex, last);
        if (first == null) {
            first = tuple;
        } else {
            last.setStore(nextStoreIndex, tuple);
        }
        last = tuple;
        size++;
    }

    public void remove(T tuple) {
        T prev = tuple.removeStore(prevStoreIndex);
        T next = tuple.removeStore(nextStoreIndex);
        if (first == tuple) {
            first = next;
        } else {
            prev.setStore(nextStoreIndex, next);
        }
        if (last == tuple) {
            last = prev;
        } else {
            next.setStore(prevStoreIndex, prev);
        }
        size--;
    }

    /**
     * Walks the list and calls the consumer for each element, then resets the head.
     * Captures {@code next} before the consumer call in case the consumer retains the tuple.
     * Member tuples' link slots are NOT nulled here — callers are responsible for any cleanup
     * needed on dying out-tuples.
     * For bulk-clear on retract this is safe: the tuples are GC'd.
     */
    public void clear(Consumer<T> consumer) {
        var entry = first;
        while (entry != null) {
            // Extract next before the consumer, in case consumer retains/removes the entry.
            var next = next(entry);
            consumer.accept(entry);
            entry = next;
        }
        first = null;
        last = null;
        size = 0;
    }

    public void forEach(Consumer<T> consumer) {
        var entry = first;
        while (entry != null) {
            var next = next(entry);
            consumer.accept(entry);
            entry = next;
        }
    }

    public @Nullable T first() {
        return first;
    }

    /**
     * Returns the element after {@code tuple} in this list, or {@code null} if it is the last.
     * Used for iterator-free manual walks: {@code for (var t = list.first(); t != null; t = list.next(t))}.
     */
    public @Nullable T next(T tuple) {
        return tuple.getStore(nextStoreIndex);
    }

    public int size() {
        return size;
    }

    /**
     * Marks {@code tuple} as significant for the single node-update operation identified by {@code version}.
     * The mark is only meaningful within that operation; see {@link #getMark(long)}.
     */
    public void mark(T tuple, long version) {
        this.mark = tuple;
        this.markVersion = version;
    }

    /**
     * Returns the tuple stamped by {@link #mark(Tuple, long)} with exactly this {@code version},
     * or {@code null} on a version mismatch, which means no mark.
     * A stale mark may reference a tuple no longer in this list —
     * callers must never read a mark with an old version.
     */
    public @Nullable T getMark(long version) {
        return markVersion == version ? mark : null;
    }

}
