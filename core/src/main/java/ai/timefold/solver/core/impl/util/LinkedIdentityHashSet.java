package ai.timefold.solver.core.impl.util;

import java.util.AbstractSet;
import java.util.IdentityHashMap;
import java.util.Iterator;

public class LinkedIdentityHashSet<V> extends AbstractSet<V> {

    private final ElementAwareList<V> delegate;
    private final IdentityHashMap<V, ElementAwareListEntry<V>> identityMap;

    public LinkedIdentityHashSet() {
        this.delegate = new ElementAwareList<>();
        this.identityMap = new IdentityHashMap<>();
    }

    @Override
    public Iterator<V> iterator() {
        return delegate.iterator();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean contains(Object o) {
        return identityMap.containsKey(o);
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
    public void clear() {
        this.identityMap.clear();
        this.delegate.clear();
    }
}
