package ai.timefold.solver.core.impl.util;

import java.io.Closeable;
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
    /**
     * It is a frequent pattern that an entry is added immediately after one is removed.
     * By reusing the entry, we can reduce the considerable GC pressure this creates.
     */
    private ElementAwareListEntry<T> availableBlankEntry = null;
    /**
     * See {@link #iterator()} for details.
     */
    private ElementAwareListIterator<T> iterator = null;

    public ElementAwareListEntry<T> add(T element) {
        var entry = newInstance(element, last);
        if (first == null) {
            first = entry;
        } else {
            last.next = entry;
        }
        last = entry;
        size++;
        return entry;
    }

    private ElementAwareListEntry<T> newInstance(T element, ElementAwareListEntry<T> previous) {
        if (availableBlankEntry == null) {
            return new ElementAwareListEntry<>(this, element, previous);
        }
        var entry = availableBlankEntry;
        availableBlankEntry = null;
        entry.list = this;
        entry.element = element;
        entry.previous = previous;
        return entry;
    }

    public void remove(ElementAwareListEntry<T> entry) {
        relinkNext(entry);
        relinkPrevious(entry);
        entry.clear();
        availableBlankEntry = entry;
        size--;
    }

    private void relinkNext(ElementAwareListEntry<T> entry) {
        if (first == entry) {
            first = entry.next;
        } else {
            entry.previous.next = entry.next;
        }
    }

    private void relinkPrevious(ElementAwareListEntry<T> entry) {
        if (last == entry) {
            last = entry.previous;
        } else {
            entry.next.previous = entry.previous;
        }
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
     * Since these lists are iterated over frequently and on the hot path,
     * creating new iterators all the time would create a lot of garbage collector pressure.
     * Therefore this method returns a {@link Closeable} iterator.
     * <p>
     * When the iterator is closed, it is returned to the list for reuse and must not be used any more by the caller.
     * When the iterator isn't explicitly closed, the iterator will not be reused,
     * and a new one is created on the next call to this method.
     * <p>
     * For use cases not specifically pinpointed to cause excessive garbage pressure,
     * regular iteration or {@link #forEach(Consumer)} is recommended.
     * 
     * @return never null
     */
    @Override
    public ElementAwareListIterator<T> iterator() {
        if (iterator == null) {
            return new ElementAwareListIterator<>(this, first);
        } else {
            var toReturn = iterator;
            iterator = null;
            toReturn.nextEntry = first;
            return toReturn;
        }
    }

    /**
     * Convenience method for where it is easy to use a non-capturing lambda.
     * If a capturing lambda consumer were to be created for this method, use the {@link #iterator()} instead.
     * 
     * <p>
     * For example, the following code is perfectly fine:
     * <code>
     *     for (int i = 0; i &lt; 3; i++) {
     *         elementAwareList.forEach(entry -&gt; doSomething(entry));
     *     }
     * </code>
     * It will create only one lambda instance, regardless of the number of iterations;
     * it doesn't need to capture any state.
     * On the contrary, the following code will create three instances of a capturing lambda,
     * one for each iteration of the for loop:
     * <code>
     *     for (int a: List.of(1, 2, 3)) {
     *         elementAwareList.forEach(entry -&gt; doSomething(entry, a));
     *     }
     * </code>
     * In this case, the lambda would need to capture "a" which is different in every iteration.
     * It will generally be better to use the iterator in this case.
     * <p>
     * This is only an issue on the hot path,
     * where this method can create quite a large garbage collector pressure
     * on account of creating throw-away instances of capturing lambdas.
     *
     * @param elementConsumer The action to be performed for each element
     */
    @Override
    public void forEach(Consumer<? super T> elementConsumer) {
        var entry = first;
        while (entry != null) {
            // Extract next before processing it, in case the entry is removed and entry.next becomes null
            var next = entry.next;
            elementConsumer.accept(entry.element);
            entry = next;
        }
    }

    /**
     * @see ElementAwareList#iterator() Information about the reuse of the iterator.
     */
    public static final class ElementAwareListIterator<T> implements Iterator<T>, Closeable {

        private final ElementAwareList<T> list;
        private ElementAwareListEntry<T> nextEntry;

        private ElementAwareListIterator(ElementAwareList<T> list, ElementAwareListEntry<T> nextEntry) {
            this.list = list;
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

        /**
         * When called, the iterator is made available for reuse.
         */
        @Override
        public void close() {
            nextEntry = null;
            list.iterator = this;
        }

    }

    @Override
    public String toString() {
        var content = switch (size) {
            case 0 -> "";
            case 1 -> first.element;
            default -> {
                var builder = new StringBuilder();
                for (var element : this) {
                    builder.append(element).append(", ");
                }
                var length = builder.length();
                builder.replace(length - 2, length, ""); // Remove the final ", ".
                yield builder.toString();
            }
        };
        return "[" + content + "]";
    }

}
