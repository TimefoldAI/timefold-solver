package ai.timefold.solver.core.impl.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
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
    public Iterator<T> randomizedIterator(Random random) {
        return switch (size) {
            case 0 -> Collections.emptyIterator();
            case 1 -> Collections.singleton(first.getElement()).iterator();
            case 2 -> {
                var list = random.nextBoolean() ? List.of(first.getElement(), last.getElement())
                        : List.of(last.getElement(), first.getElement());
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

    /**
     * The idea of this iterator is that the list will rarely ever be iterated over in its entirety.
     * In fact, move streams are likely to only use the first few elements.
     * Therefore, shuffling the entire list would be a waste of time.
     * Instead, we pick random index every time and keep a list of unused indexes.
     *
     * @param <T> The element type. Often a tuple.
     */
    private static final class RandomElementAwareListIterator<T> implements Iterator<T> {

        private final List<T> elementList;
        private final List<Integer> unusedIndexList;
        private final Random random;

        public RandomElementAwareListIterator(List<T> copiedList, List<Integer> unusedIndexList, Random random) {
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

}
