package ai.timefold.solver.core.impl.util;

import java.util.IdentityHashMap;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Identity map whose {@link #clear()} replaces the table when the map stays far below its old size,
 * because {@link IdentityHashMap#clear()} costs O(capacity), not O(size).
 * <p>
 * Only implements those methods of {@link Map} which the code needed at the time.
 */
@NullMarked
public final class ShrinkingIdentityHashMap<K, V> {

    private static final int CHEAP_CLEAR_THRESHOLD = 32; // Selected mostly arbitrarily.

    // Intentionally not just any Map; that would prevent inlining,
    // replacing a performance issue in clear() with another in delegation.
    private IdentityHashMap<K, V> delegate = new IdentityHashMap<>();
    private int allocatedSize = 0;
    private int recentPeakSize = 0;
    private int cyclePeakSize = 0;

    public @Nullable V get(Object key) {
        return delegate.get(key);
    }

    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    public @Nullable V put(K key, V value) {
        var old = delegate.put(key, value);
        var size = delegate.size();
        if (size > cyclePeakSize) {
            cyclePeakSize = size;
        }
        return old;
    }

    public @Nullable V remove(Object key) {
        return delegate.remove(key);
    }

    public int size() {
        return delegate.size();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public void clear() {
        if (delegate.isEmpty()) { // IdentityHashMap.clear() is O(capacity) even when empty.
            return;
        }
        allocatedSize = Math.max(allocatedSize, cyclePeakSize);
        recentPeakSize = Math.max(recentPeakSize, cyclePeakSize);
        if (allocatedSize > CHEAP_CLEAR_THRESHOLD && recentPeakSize < allocatedSize / 2) {
            delegate = new IdentityHashMap<>(cyclePeakSize);
            allocatedSize = cyclePeakSize;
            recentPeakSize = cyclePeakSize;
        } else {
            recentPeakSize -= recentPeakSize / 8; // Decay; a replacement needs several small cycles in a row.
            delegate.clear();
        }
        cyclePeakSize = 0;
    }

    IdentityHashMap<K, V> delegate() { // For tests.
        return delegate;
    }

}
