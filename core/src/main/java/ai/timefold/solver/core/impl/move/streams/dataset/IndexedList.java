package ai.timefold.solver.core.impl.move.streams.dataset;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.impl.util.CollectionUtils;

final class IndexedList<E> extends AbstractList<E> {

    private final List<E> list;
    private final Map<E, Integer> indexMap;

    public IndexedList() {
        this(16);
    }

    public IndexedList(int expectedSize) {
        this.list = new ArrayList<>(expectedSize);
        this.indexMap = CollectionUtils.newIdentityHashMap(expectedSize);
    }

    @Override
    public E get(int index) {
        return list.get(index);
    }

    @Override
    public E set(int index, E element) {
        if (index < 0 || index >= list.size()) {
            throw new IndexOutOfBoundsException("The index (" + index + ") must be between 0 and " + (list.size() - 1) + ".");
        } else if (indexMap.containsKey(element)) {
            throw new IllegalArgumentException("The element (" + element + ") was already added to the list.");
        } else {
            E oldElement = list.set(index, element);
            indexMap.remove(oldElement);
            indexMap.put(element, index);
            return oldElement;
        }
    }

    @Override
    public void add(int index, E element) {
        if (index < 0 || index > list.size()) {
            throw new IndexOutOfBoundsException("The index (" + index + ") must be between 0 and " + list.size() + ".");
        } else if (indexMap.containsKey(element)) {
            throw new IllegalArgumentException("The element (" + element + ") was already added to the list.");
        }
        list.add(index, element);
        indexMap.put(element, index);
        reindex(index + 1);
    }

    private void reindex(int startingIndex) {
        int listSize = list.size();
        for (int i = startingIndex; i < listSize; i++) {
            indexMap.put(list.get(i), i);
        }
    }

    @Override
    public E remove(int index) {
        E oldElement = list.remove(index);
        indexMap.remove(oldElement);
        reindex(index);
        return oldElement;
    }

    @SuppressWarnings("unchecked")
    @Override
    public int indexOf(Object o) {
        return indexMap.getOrDefault((E) o, -1);
    }

    @Override
    public int size() {
        return list.size();
    }

}
