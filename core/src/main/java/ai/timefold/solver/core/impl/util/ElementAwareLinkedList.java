package ai.timefold.solver.core.impl.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Linked list that allows to add and remove an element in O(1) time.
 * Ideal for incremental operations with frequent undo.
 *
 * @param <T> The element type. Often a tuple.
 */
public final class ElementAwareLinkedList<T> implements Iterable<T> {

    private int size = 0;
    private Entry<T> first = null;
    private Entry<T> last = null;

    public Entry<T> add(T tuple) {
        Entry<T> entry = new Entry<>(this, tuple, last);
        if (first == null) {
            first = entry;
        } else {
            last.next = entry;
        }
        last = entry;
        size++;
        return entry;
    }

    public Entry<T> addFirst(T tuple) {
        if (first != null) {
            Entry<T> entry = new Entry<>(this, tuple, null);
            first.previous = entry;
            entry.next = first;
            first = entry;
            size++;
            return entry;
        } else {
            return add(tuple);
        }
    }

    public Entry<T> addAfter(T tuple, Entry<T> previous) {
        Objects.requireNonNull(previous);
        if (first == null || previous == last) {
            return add(tuple);
        } else {
            Entry<T> entry = new Entry<>(this, tuple, previous);
            Entry<T> currentNext = previous.next;
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

    public void remove(Entry<T> entry) {
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

    public Entry<T> first() {
        return first;
    }

    public Entry<T> last() {
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
        Entry<T> entry = first;
        while (entry != null) {
            // Extract next before processing it, in case the entry is removed and entry.next becomes null
            Entry<T> next = entry.next;
            tupleConsumer.accept(entry.element());
            entry = next;
        }
    }

    /**
     * Clears the list in one go, after consuming all elements with the provided consumer.
     *
     * @param tupleConsumer The action to be performed for each element before clearing.
     */
    public void clear(Consumer<? super T> tupleConsumer) {
        forEach(tupleConsumer);
        clear();
    }

    /**
     * See {@link #forEach(Consumer)} for a discussion on the correct use of this method.
     *
     * @return never null
     */
    @Override
    public Iterator<T> iterator() {
        if (size == 0) {
            return Collections.emptyIterator();
        }
        return new ElementAwareListIterator<>(first);
    }

    public void clear() {
        first = null;
        last = null;
        size = 0;
    }

    /**
     * Returns an iterator that will randomly iterate over the elements.
     * This iterator is exhaustive; once every element has been once iterated over,
     * the iterator returns false for every subsequent {@link Iterator#hasNext()}.
     * The iterator does not support the {@link Iterator#remove()} operation.
     *
     * @param random The random instance to use for shuffling.
     * @return never null
     */
    public Iterator<T> randomizedIterator(RandomGenerator random) {
        return switch (size) {
            case 0 -> Collections.emptyIterator();
            case 1 -> Collections.singleton(first.element()).iterator();
            case 2 -> {
                var list = random.nextBoolean() ? List.of(first.element(), last.element())
                        : List.of(last.element(), first.element());
                yield list.iterator();
            }
            default -> {
                var copy = new ArrayList<T>(size);
                var indexList = new ArrayList<Integer>(size);
                forEach(e -> { // Two lists, single iteration.
                    copy.add(e);
                    indexList.add(copy.size() - 1);
                });
                yield new RandomElementAwareListIterator<>(copy, indexList, random);
            }
        };
    }

    @Override
    public String toString() {
        switch (size) {
            case 0 -> {
                return "[]";
            }
            case 1 -> {
                return "[" + first.element() + "]";
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

        private Entry<T> nextEntry;

        public ElementAwareListIterator(Entry<T> nextEntry) {
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
            T element = nextEntry.element();
            nextEntry = nextEntry.next;
            return element;
        }

    }

    /**
     * The idea of this iterator is that the list will rarely ever be iterated over in its entirety.
     * In fact, Neighborhoods API is likely to only use the first few elements.
     * Therefore, shuffling the entire list would be a waste of time.
     * Instead, we pick random index every time and keep a list of unused indexes.
     *
     * @param <T> The element type. Often a tuple.
     */
    private static final class RandomElementAwareListIterator<T> implements Iterator<T> {

        private final List<T> elementList;
        private final List<Integer> unusedIndexList;
        private final RandomGenerator random;

        public RandomElementAwareListIterator(List<T> copiedList, List<Integer> unusedIndexList, RandomGenerator random) {
            this.random = random;
            this.elementList = copiedList;
            this.unusedIndexList = unusedIndexList;
        }

        @Override
        public boolean hasNext() {
            return !unusedIndexList.isEmpty();
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var randomUnusedIndex = random.nextInt(unusedIndexList.size());
            var elementIndex = unusedIndexList.remove(randomUnusedIndex);
            return elementList.get(elementIndex);
        }

    }

    @NullMarked
    public static final class Entry<T> implements ListEntry<T> {

        private @Nullable ElementAwareLinkedList<T> list;
        private final T element;
        @Nullable
        Entry<T> previous;
        @Nullable
        Entry<T> next;

        Entry(ElementAwareLinkedList<T> list, T element, @Nullable Entry<T> previous) {
            this.list = list;
            this.element = element;
            this.previous = previous;
            this.next = null;
        }

        public @Nullable Entry<T> previous() {
            return previous;
        }

        public @Nullable Entry<T> next() {
            return next;
        }

        public void remove() {
            if (list == null) {
                throw new IllegalStateException("The element (" + element + ") was already removed.");
            }
            list.remove(this);
            list = null;
        }

        @Override
        public T element() {
            return element;
        }

        public @Nullable ElementAwareLinkedList<T> getList() {
            return list;
        }

        @Override
        public String toString() {
            return element.toString();
        }

    }
}
