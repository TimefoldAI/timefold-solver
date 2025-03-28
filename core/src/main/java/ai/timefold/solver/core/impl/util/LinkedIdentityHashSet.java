package ai.timefold.solver.core.impl.util;

import java.util.AbstractSet;
import java.util.IdentityHashMap;
import java.util.Iterator;

import org.jspecify.annotations.NullMarked;

/**
 * This set does not support null keys.
 * 
 * @param <V>
 */
@NullMarked
public final class LinkedIdentityHashSet<V> extends AbstractSet<V> {

    private final ElementAwareList<V> delegate;
    private final IdentityHashMap<V, ElementAwareListEntry<V>> identityMap;
    private int size = 0; // Avoid method calls to underlying collections.

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
        return size;
    }

    @Override
    public boolean contains(Object o) {
        if (size == 0) { // Micro-optimization; contains() on an empty map is not entirely free.
            return false;
        }
        return identityMap.containsKey(o);
    }

    @Override
    public boolean add(V v) {
        var entry = identityMap.get(v);
        if (entry == null) {
            identityMap.put(v, delegate.add(v));
            size += 1;
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        if (size == 0) { // Micro-optimization; remove() on an empty map is not entirely free.
            return false;
        }
        var entry = identityMap.remove(o);
        if (entry == null) {
            return false;
        }
        entry.remove();
        size -= 1;
        return true;
    }

    @Override
    public void clear() {
        if (size == 0) { // Micro-optimization; clearing empty maps is not entirely free.
            return;
        }
        this.identityMap.clear();
        this.delegate.clear();
        size = 0;
    }
}
