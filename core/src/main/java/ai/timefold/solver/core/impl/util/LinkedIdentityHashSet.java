package ai.timefold.solver.core.impl.util;

import java.util.AbstractSet;
import java.util.IdentityHashMap;
import java.util.Iterator;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class LinkedIdentityHashSet<V> extends AbstractSet<V> {

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
        var entry = identityMap.get(v);
        if (entry == null) {
            identityMap.put(v, delegate.add(v));
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        var entry = identityMap.remove(o);
        if (entry == null) {
            return false;
        }
        entry.remove();
        return true;
    }

    @Override
    public void clear() {
        this.identityMap.clear();
        this.delegate.clear();
    }
}
