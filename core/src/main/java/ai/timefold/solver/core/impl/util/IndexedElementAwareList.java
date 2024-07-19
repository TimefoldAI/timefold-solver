package ai.timefold.solver.core.impl.util;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * This data structure differs from {@link ElementAwareList} because it provides a find operation in O(H).
 * 
 * The implementation relies on two different data structures: {@link java.util.IdentityHashMap} and {@link ElementAwareList}.
 *
 * The element comparison works in the same way as {@link java.util.IdentityHashMap},
 * and two keys k1 and k2 are considered equal if and only if (k1==k2).
 *
 * @param <T> The element type.
 */
public class IndexedElementAwareList<T> implements Iterable<T> {

    private final ElementAwareList<T> tupleList;
    private final IdentityHashMap<T, ElementAwareListEntry<T>> tupleIndexMap;

    public IndexedElementAwareList() {
        this.tupleList = new ElementAwareList<>();
        this.tupleIndexMap = new IdentityHashMap<>();
    }

    public boolean contains(T tuple) {
        return tupleIndexMap.containsKey(tuple);
    }

    public boolean add(T tuple) {
        var exists = tupleIndexMap.containsKey(tuple);
        if (!exists) {
            tupleIndexMap.put(tuple, tupleList.add(tuple));
        }
        return !exists;
    }

    public boolean addFirst(T tuple) {
        var exists = tupleIndexMap.containsKey(tuple);
        if (!exists) {
            tupleIndexMap.put(tuple, tupleList.addFirst(tuple));
        }
        return !exists;
    }

    public boolean addAfter(T tuple, T previousTuple) {
        Objects.requireNonNull(previousTuple);
        var exists = tupleIndexMap.containsKey(tuple);
        if (!exists) {
            var previous = tupleIndexMap.get(previousTuple);
            tupleIndexMap.put(tuple, tupleList.addAfter(tuple, previous));
        }
        return !exists;
    }

    public boolean remove(T tuple) {
        var exists = tupleIndexMap.containsKey(tuple);
        if (exists) {
            var removed = tupleIndexMap.remove(tuple);
            tupleList.remove(removed);
        }
        return exists;
    }

    public T first() {
        return tupleList.first() != null ? tupleList.first().getElement() : null;
    }

    public T last() {
        return tupleList.last() != null ? tupleList.last().getElement() : null;
    }

    public T previous(T element) {
        return computeIfNotNull(tupleIndexMap.get(element), ElementAwareListEntry::previous);
    }

    public T next(T element) {
        return computeIfNotNull(tupleIndexMap.get(element), ElementAwareListEntry::next);
    }

    private T computeIfNotNull(ElementAwareListEntry<T> entry, UnaryOperator<ElementAwareListEntry<T>> operation) {
        if (entry != null) {
            var entryValue = operation.apply(entry);
            if (entryValue != null) {
                return entryValue.getElement();
            }
        }
        return null;
    }

    public int size() {
        return tupleList.size();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        tupleList.forEach(action);
    }

    @Override
    public Iterator<T> iterator() {
        return tupleList.iterator();
    }

    @Override
    public String toString() {
        return tupleList.toString();
    }
}
