package ai.timefold.solver.core.impl.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
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

    public ElementAwareListEntry<T> addFirst(T tuple) {
        if (first != null) {
            ElementAwareListEntry<T> entry = new ElementAwareListEntry<>(this, tuple, null);
            first.previous = entry;
            entry.next = first;
            first = entry;
            size++;
            return entry;
        } else {
            return add(tuple);
        }
    }

    public ElementAwareListEntry<T> addAfter(T tuple, ElementAwareListEntry<T> previous) {
        Objects.requireNonNull(previous);
        if (first == null || previous == last) {
            return add(tuple);
        } else {
            ElementAwareListEntry<T> entry = new ElementAwareListEntry<>(this, tuple, previous);
            ElementAwareListEntry<T> currentNext = previous.next;
            if (currentNext != null) {
                currentNext.previous = entry;
            } else {
                last = entry;
            }
            previous.next = entry;
            entry.next = currentNext;
            size++;
            return entry;
        }
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

    /**
     * Convenience method for where it is easy to use a non-capturing lambda.
     * If a capturing lambda consumer were to be created for this method, use {@link #iterator()} instead,
     * which will consume less memory.
     * <p>
     *
     * For example, the following code is perfectly fine:
     *
     * <code>
     *     for (int i = 0; i &lt; 3; i++) {
     *         elementAwareList.forEach(entry -&gt; doSomething(entry));
     *     }
     * </code>
     *
     * It will create only one lambda instance, regardless of the number of iterations;
     * it doesn't need to capture any state.
     * On the contrary, the following code will create three instances of a capturing lambda,
     * one for each iteration of the for loop:
     *
     * <code>
     *     for (int a: List.of(1, 2, 3)) {
     *         elementAwareList.forEach(entry -&gt; doSomething(entry, a));
     *     }
     * </code>
     *
     * In this case, the lambda would need to capture "a" which is different in every iteration.
     * Therefore, it will generally be better to use the iterator variant,
     * as that will only ever create one instance of the iterator,
     * regardless of the number of iterations:
     *
     * <code>
     *     for (int a: List.of(1, 2, 3)) {
     *         for (var entry: elementAwareList) {
     *             doSomething(entry, a);
     *         }
     *     }
     * </code>
     *
     * This is only an issue on the hot path,
     * where this method can create quite a large garbage collector pressure
     * on account of creating throw-away instances of capturing lambdas.
     *
     * @param tupleConsumer The action to be performed for each element
     */
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

    /**
     * See {@link #forEach(Consumer)} for a discussion on the correct use of this method.
     *
     * @return never null
     */
    @Override
    public Iterator<T> iterator() {
        return new ElementAwareListIterator<>(first);
    }

    @Override
    public String toString() {
        switch (size) {
            case 0 -> {
                return "[]";
            }
            case 1 -> {
                return "[" + first.getElement() + "]";
            }
            default -> {
                StringBuilder builder = new StringBuilder("[");
                for (T entry : this) {
                    builder.append(entry).append(", ");
                }
                builder.replace(builder.length() - 2, builder.length(), "");
                return builder.append("]").toString();
            }
        }
    }

    private static final class ElementAwareListIterator<T> implements Iterator<T> {

        private ElementAwareListEntry<T> nextEntry;

        public ElementAwareListIterator(ElementAwareListEntry<T> nextEntry) {
            this.nextEntry = nextEntry;
        }

        @Override
        public boolean hasNext() {
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

    }
}
