package ai.timefold.jpyinterpreter.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Source: <a href=
 * "https://github.com/spring-projects/spring-loaded/blob/be39be1624c50b83741c0add3213ff02d23c6fd5/springloaded/src/main/java/org/springsource/loaded/support/ConcurrentWeakIdentityHashMap.java">Spring
 * Loaded ConcurrentWeakIdentityHashMap</a>
 */
public class ConcurrentWeakIdentityHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {
    private final ConcurrentMap<Key<K>, V> map;
    private final ReferenceQueue<K> queue = new ReferenceQueue<>();
    private transient Set<Map.Entry<K, V>> entry;

    public ConcurrentWeakIdentityHashMap(int initialCapacity) {
        this.map = new ConcurrentHashMap<>(initialCapacity);
    }

    public ConcurrentWeakIdentityHashMap() {
        this.map = new ConcurrentHashMap<>();
    }

    @Override
    public V get(Object key) {
        purgeKeys();
        return map.get(new Key<>(key, null));
    }

    @Override
    public V put(K key, V value) {
        purgeKeys();
        return map.put(new Key<>(key, queue), value);
    }

    @Override
    public int size() {
        purgeKeys();
        return map.size();
    }

    private void purgeKeys() {
        Reference<? extends K> reference;
        while ((reference = queue.poll()) != null) {
            map.remove(reference);
        }
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> entrySet;
        return ((entrySet = this.entry) == null) ? entry = new EntrySet() : entrySet;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        purgeKeys();
        return map.putIfAbsent(new Key<>(key, queue), value);
    }

    @Override
    public V remove(Object key) {
        return map.remove(new Key<>(key, null));
    }

    @Override
    public boolean remove(Object key, Object value) {
        purgeKeys();
        return map.remove(new Key<>(key, null), value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        purgeKeys();
        return map.replace(new Key<>(key, null), oldValue, newValue);
    }

    @Override
    public V replace(K key, V value) {
        purgeKeys();
        return map.replace(new Key<>(key, null), value);
    }

    @Override
    public boolean containsKey(Object key) {
        purgeKeys();
        return map.containsKey(new Key<>(key, null));
    }

    @Override
    public void clear() {
        while (queue.poll() != null)
            ;
        map.clear();
    }

    @Override
    public boolean containsValue(Object value) {
        purgeKeys();
        return map.containsValue(value);
    }

    private static class Key<T> extends WeakReference<T> {

        private final int hash;

        Key(T t, ReferenceQueue<T> queue) {
            super(t, queue);
            if (t == null) {
                throw new NullPointerException();
            } else {
                hash = System.identityHashCode(t);
            }
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || obj instanceof Key && ((Key<?>) obj).get() == get();
        }

        @Override
        public int hashCode() {
            return hash;
        }

    }

    private class Iter implements Iterator<Map.Entry<K, V>> {

        private final Iterator<Map.Entry<Key<K>, V>> it;
        private Map.Entry<K, V> nextValue;

        Iter(Iterator<Map.Entry<Key<K>, V>> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            if (nextValue != null) {
                return true;
            }
            while (it.hasNext()) {
                Map.Entry<Key<K>, V> entry = it.next();
                K key = entry.getKey().get();
                if (key != null) {
                    nextValue = new Entry(key, entry.getValue());
                    return true;
                } else {
                    it.remove();
                }
            }
            return false;
        }

        @Override
        public Map.Entry<K, V> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Map.Entry<K, V> entry = nextValue;
            nextValue = null;
            return entry;
        }

        @Override
        public void remove() {
            it.remove();
            nextValue = null;
        }

    }

    private class EntrySet extends AbstractSet<Map.Entry<K, V>> {

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new Iter(map.entrySet().iterator());
        }

        @Override
        public int size() {
            return ConcurrentWeakIdentityHashMap.this.size();
        }

        @Override
        public void clear() {
            ConcurrentWeakIdentityHashMap.this.clear();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) {
                return false;
            }
            return ConcurrentWeakIdentityHashMap.this.get(e.getKey()) == e.getValue();
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) {
                return false;
            }
            return ConcurrentWeakIdentityHashMap.this.remove(e.getKey(), e.getValue());
        }
    }

    private class Entry extends AbstractMap.SimpleEntry<K, V> {
        Entry(K key, V value) {
            super(key, value);
        }

        @Override
        public V setValue(V value) {
            ConcurrentWeakIdentityHashMap.this.put(getKey(), value);
            return super.setValue(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Map.Entry<?, ?> e) {
                return getKey() == e.getKey() && getValue() == e.getValue();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(getKey())
                    ^ System.identityHashCode(getValue());
        }
    }
}
