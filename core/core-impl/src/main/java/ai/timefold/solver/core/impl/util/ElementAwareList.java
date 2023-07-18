package ai.timefold.solver.core.impl.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * Linked list that allows to add and remove an element in O(1) time.
 * Ideal for incremental operations with frequent undo.
 *
 * @param <T> The element type. Often a tuple.
 */
public final class ElementAwareList<T> implements Iterable<T> {

    private int size = 0;
    private ElementAwareListEntry<T> first = null;
    private ElementAwareListEntry<T> last = null;

    public ElementAwareListEntry<T> add(T tuple) {
        ElementAwareListEntry<T> entry = new ElementAwareListEntry<>(this, tuple, last);
        if (first == null) {
            first = entry;
        } else {
            last.next = entry;
        }
        last = entry;
        size++;
        return entry;
    }

    public void remove(ElementAwareListEntry<T> entry) {
        if (first == entry) {
            first = entry.next;
        } else {
            entry.previous.next = entry.next;
        }
        if (last == entry) {
            last = entry.previous;
        } else {
            entry.next.previous = entry.previous;
        }
        entry.previous = null;
        entry.next = null;
        size--;
    }

    public ElementAwareListEntry<T> first() {
        return first;
    }

    public ElementAwareListEntry<T> last() {
        return last;
    }

    public int size() {
        return size;
    }

    @Override
    public void forEach(Consumer<? super T> tupleConsumer) {
        ElementAwareListEntry<T> entry = first;
        while (entry != null) {
            // Extract next before processing it, in case the entry is removed and entry.next becomes null
            ElementAwareListEntry<T> next = entry.next;
            tupleConsumer.accept(entry.getElement());
            entry = next;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {

            private ElementAwareListEntry<T> nextEntry = first;

            @Override
            public boolean hasNext() {
                if (size == 0) {
                    return false;
                }
                return nextEntry != null;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                T element = nextEntry.getElement();
                nextEntry = nextEntry.next;
                return element;
            }

        };
    }

    @Override
    public String toString() {
        return "size = " + size;
    }

}
