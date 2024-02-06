package ai.timefold.solver.core.impl.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * Linked list that allows to add and remove an element in O(1) time.
 * Ideal for incremental operations with frequent undo.
 * <p>
 * This class is not thread-safe.
 *
 * @param <T> The element type. Often a tuple.
 */
public final class ElementAwareList<T> implements Iterable<T> {

    private ElementAwareListIterator sharedIterator;

    private int size = 0;
    private ElementAwareListEntry<T> first = null;
    private ElementAwareListEntry<T> last = null;

    public ElementAwareList() {
    }

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
        if (sharedIterator == null || sharedIterator.nextEntry != null) {
            // Create a new instance on first access, or when the previous one is still in use (not fully consumed).
            sharedIterator = new ElementAwareListIterator();
        } else {
            // Otherwise the instance is reused, significantly reducing garbage collector pressure when on the hot path.
            sharedIterator.nextEntry = first;
        }
        return sharedIterator;
    }

    /**
     * In order for iterator sharing to work properly,
     * each iterator must be fully consumed.
     * For iterators which are not fully consumed,
     * this method can be used to free the iterator to be used by another iteration operation.
     * This technically breaks the abstraction of this class,
     * but the measured benefit of this change is +5% to 10% of score calculation speed
     * on account of not creating gigabytes of iterators per minute.
     */
    public void prematurelyTerminateIterator() {
        sharedIterator.nextEntry = null;
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

    private final class ElementAwareListIterator implements Iterator<T> {

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

    }
}
