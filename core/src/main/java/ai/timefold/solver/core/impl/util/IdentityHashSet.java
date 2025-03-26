package ai.timefold.solver.core.impl.util;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class IdentityHashSet<V> implements Set<V> {

    private ElementAwareList<V> delegate;
    private IdentityHashMap<V, ElementAwareListEntry<V>> identityMap;

    public IdentityHashSet() {
        this.delegate = new ElementAwareList<>();
        this.identityMap = new IdentityHashMap<>();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        return identityMap.containsKey(o);
    }

    @Override
    public Iterator<V> iterator() {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        var result = new Object[delegate.size()];
        var idx = new MutableInt(0);
        delegate.iterator().forEachRemaining(v -> {
            result[idx.intValue()] = v;
            idx.increment();
        });
        return result;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        var result = toArray();
        if (a.length < result.length) {
            return (T[]) result;
        }
        System.arraycopy(result, 0, a, 0, result.length);
        if (a.length > result.length) {
            a[result.length] = null;
        }
        return a;
    }

    @Override
    public boolean add(V v) {
        var size = delegate.size();
        identityMap.computeIfAbsent(v, v1 -> {
            var item = delegate.add(v1);
            identityMap.put(v1, item);
            return item;
        });
        return delegate.size() != size;
    }

    @Override
    public boolean remove(Object o) {
        var size = delegate.size();
        if (identityMap.containsKey(o)) {
            var item = identityMap.remove(o);
            delegate.remove(item);
        }
        return delegate.size() != size;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return c.stream().allMatch(this::contains);
    }

    @Override
    public boolean addAll(Collection<? extends V> c) {
        var size = delegate.size();
        c.forEach(this::add);
        return delegate.size() != size;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        boolean modified = false;
        var newIdentityMap = new IdentityHashMap<V, ElementAwareListEntry<V>>();
        var newDelegate = new ElementAwareList<V>();
        for (Object item : c) {
            if (identityMap.containsKey(item)) {
                var retainedItem = newDelegate.add((V) item);
                newIdentityMap.put((V) item, retainedItem);
                modified = true;
            }
        }
        this.identityMap = newIdentityMap;
        this.delegate = newDelegate;
        return modified;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        var size = delegate.size();
        c.forEach(this::remove);
        return size != delegate.size();
    }

    @Override
    public void clear() {
        this.identityMap.clear();
        this.delegate = new ElementAwareList<>();
    }
}
