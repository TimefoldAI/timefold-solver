package ai.timefold.solver.core.impl.util;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.function.QuadConsumer;
import ai.timefold.solver.core.api.function.TriConsumer;

/**
 * Linked list that allows to add and remove an element in O(1) time.
 * Ideal for incremental operations with frequent undo.
 *
 * @param <T> The element type. Often a tuple.
 */
public final class ElementAwareList<T> {

    /**
     * It is a frequent pattern that an entry is added immediately after one is removed.
     * By reusing the entry, we can reduce the considerable GC pressure this creates.
     */
    private Entry availableBlankEntry = null;
    private int size = 0;
    private Entry first = null;
    private Entry last = null;

    public Entry add(T tuple) {
        Entry entry = newInstance(tuple, last);
        if (first == null) {
            first = entry;
        } else {
            last.next = entry;
        }
        last = entry;
        size++;
        return entry;
    }

    private Entry newInstance(T tuple, Entry previous) {
        if (availableBlankEntry != null) {
            Entry entry = availableBlankEntry;
            availableBlankEntry = null;
            entry.element = tuple;
            entry.previous = previous;
            entry.next = null;
            return entry;
        } else {
            return new Entry(tuple, previous);
        }
    }

    public Entry addFirst(T tuple) {
        if (first != null) {
            Entry entry = newInstance(tuple, null);
            first.previous = entry;
            entry.next = first;
            first = entry;
            size++;
            return entry;
        } else {
            return add(tuple);
        }
    }

    public Entry addAfter(T tuple, Entry previous) {
        Objects.requireNonNull(previous);
        if (first == null || previous == last) {
            return add(tuple);
        } else {
            Entry entry = newInstance(tuple, previous);
            Entry currentNext = previous.next;
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

    public void remove(Entry entry) {
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
        if (availableBlankEntry == null) {
            entry.element = null;
            availableBlankEntry = entry;
        }
        size--;
    }

    public Entry first() {
        return first;
    }

    public Entry last() {
        return last;
    }

    public int size() {
        return size;
    }

    /**
     * Convenience method for where it is easy to use a non-capturing lambda.
     * If a capturing lambda consumer were to be created for this method, use either of the following instead,
     * which will consume less memory:
     * 
     * <ul>
     * <li>{@link #forEach(Object, BiConsumer)} for when one extra argument is required.</li>
     * <li>{@link #forEach(Object, Object, TriConsumer)} for when two extra arguments are required.</li>
     * <li>{@link #forEach(Object, Object, Object, QuadConsumer)} for when three extra arguments are required.</li>
     * </ul>
     * 
     * <p>
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
     * It will generally be better to use a {@link #forEach(Object, BiConsumer)} variant with one extra argument,
     * as that will not result in any new instances of the lambda (or iterator) being created:
     *
     * <code>
     *     for (int a: List.of(1, 2, 3)) {
     *         elementAwareList.forEach(a, (entry, a_) -&gt; doSomething(entry, a_));
     *     }
     * </code>
     *
     * This is only an issue on the hot path,
     * where this method can create quite a large garbage collector pressure
     * on account of creating throw-away instances of capturing lambdas.
     *
     * @param tupleConsumer The action to be performed for each element
     */
    public void forEach(Consumer<? super T> tupleConsumer) {
        Entry entry = first;
        while (entry != null) {
            // Extract next before processing it, in case the entry is removed and entry.next becomes null
            Entry next = entry.next;
            tupleConsumer.accept(entry.getElement());
            entry = next;
        }
    }

    /**
     * As {@link #forEach(Consumer)}, but with an extra argument.
     *
     * @param other Extra argument to be passed to the consumer.
     * @param tupleConsumer The action to be performed for each element
     */
    public <U> void forEach(U other, BiConsumer<? super T, U> tupleConsumer) {
        Entry entry = first;
        while (entry != null) {
            // Extract next before processing it, in case the entry is removed and entry.next becomes null
            Entry next = entry.next;
            tupleConsumer.accept(entry.getElement(), other);
            entry = next;
        }
    }

    /**
     * As {@link #forEach(Consumer)}, but with two extra arguments.
     *
     * @param other Extra argument to be passed to the consumer.
     * @param another Another extra argument to be passed to the consumer.
     * @param tupleConsumer The action to be performed for each element
     */
    public <U, V> void forEach(U other, V another, TriConsumer<? super T, U, V> tupleConsumer) {
        Entry entry = first;
        while (entry != null) {
            // Extract next before processing it, in case the entry is removed and entry.next becomes null
            Entry next = entry.next;
            tupleConsumer.accept(entry.getElement(), other, another);
            entry = next;
        }
    }

    /**
     * As {@link #forEach(Consumer)}, but with three extra arguments.
     *
     * @param other Extra argument to be passed to the consumer.
     * @param another Another extra argument to be passed to the consumer.
     * @param yetAnother Yet another extra argument to be passed to the consumer.
     * @param tupleConsumer The action to be performed for each element
     */
    public <U, V, W> void forEach(U other, V another, W yetAnother, QuadConsumer<? super T, U, V, W> tupleConsumer) {
        Entry entry = first;
        while (entry != null) {
            // Extract next before processing it, in case the entry is removed and entry.next becomes null
            Entry next = entry.next;
            tupleConsumer.accept(entry.getElement(), other, another, yetAnother);
            entry = next;
        }
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
                for (Entry entry = first; entry != null; entry = entry.next) {
                    builder.append(entry.getElement()).append(", ");
                }
                builder.replace(builder.length() - 2, builder.length(), "");
                return builder.append("]").toString();
            }
        }
    }

    /**
     * An entry of {@link ElementAwareList}
     */
    public final class Entry {

        private T element;
        Entry previous;
        Entry next;

        Entry(T element, Entry previous) {
            this.element = element;
            this.previous = previous;
            this.next = null;
        }

        public Entry previous() {
            return previous;
        }

        public Entry next() {
            return next;
        }

        public void remove() {
            if (element == null && previous == null && next == null) { // The element may be null if the entry was reused.
                throw new IllegalStateException("The entry was already removed.");
            }
            ElementAwareList.this.remove(this);
        }

        public T getElement() {
            return element;
        }

        public ElementAwareList<T> getList() {
            return ElementAwareList.this;
        }

        @Override
        public String toString() {
            return element.toString();
        }

    }

}
