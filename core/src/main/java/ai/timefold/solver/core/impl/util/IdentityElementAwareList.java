package ai.timefold.solver.core.impl.util;

import java.util.IdentityHashMap;
import java.util.Iterator;
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
public class IdentityElementAwareList<T> implements Iterable<T> {

    private final ElementAwareList<T> elementsList;
    private final IdentityHashMap<T, ElementAwareListEntry<T>> elementsMap;

    public IdentityElementAwareList() {
        this.elementsList = new ElementAwareList<>();
        this.elementsMap = new IdentityHashMap<>();
    }

    public boolean contains(T element) {
        return elementsMap.containsKey(element);
    }

    public boolean add(T element) {
        var exists = elementsMap.containsKey(element);
        if (!exists) {
            elementsMap.put(element, elementsList.add(element));
        }
        return !exists;
    }

    public boolean remove(T element) {
        var exists = elementsMap.containsKey(element);
        if (exists) {
            var removed = elementsMap.remove(element);
            elementsList.remove(removed);
        }
        return exists;
    }

    public T first() {
        return elementsList.first() != null ? elementsList.first().getElement() : null;
    }

    public T last() {
        return elementsList.last() != null ? elementsList.last().getElement() : null;
    }

    public T previous(T element) {
        return computeIfNotNull(elementsMap.get(element), ElementAwareListEntry::previous);
    }

    public T next(T element) {
        return computeIfNotNull(elementsMap.get(element), ElementAwareListEntry::next);
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
        return elementsList.size();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        elementsList.forEach(action);
    }

    @Override
    public Iterator<T> iterator() {
        return elementsList.iterator();
    }

    @Override
    public String toString() {
        return elementsList.toString();
    }
}
