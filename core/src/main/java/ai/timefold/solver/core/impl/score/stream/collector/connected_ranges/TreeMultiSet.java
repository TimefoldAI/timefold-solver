package ai.timefold.solver.core.impl.score.stream.collector.connected_ranges;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TreeMap;

public final class TreeMultiSet<T> extends AbstractSet<T> {
    private final TreeMap<T, Integer> backingMap;
    private int size;

    public TreeMultiSet(Comparator<? super T> comparator) {
        backingMap = new TreeMap<>(comparator);
        size = 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new MultiSetIterator();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean add(T key) {
        backingMap.merge(key, 1, Integer::sum);
        size++;
        return true;
    }

    @Override
    public boolean remove(Object o) {
        var removed = backingMap.remove(o);
        if (removed != null) {
            if (removed != 1) {
                backingMap.put((T) o, removed - 1);
            }
            size--;
            return true;
        }
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return backingMap.containsKey(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return backingMap.keySet().containsAll(c);
    }

    private final class MultiSetIterator implements Iterator<T> {
        T currentKey = backingMap.isEmpty() ? null : backingMap.firstKey();
        int remainingForKey = currentKey != null ? backingMap.get(currentKey) : 0;

        @Override
        public boolean hasNext() {
            return remainingForKey > 0 || backingMap.higherKey(currentKey) != null;
        }

        @Override
        public T next() {
            if (remainingForKey > 0) {
                remainingForKey--;
                return currentKey;
            }
            currentKey = backingMap.higherKey(currentKey);
            if (currentKey == null) {
                throw new NoSuchElementException();
            }
            remainingForKey = backingMap.get(currentKey) - 1;
            return currentKey;
        }

        @Override
        public void remove() {
            TreeMultiSet.this.remove(currentKey);
        }
    }
}
