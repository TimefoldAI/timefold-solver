package ai.timefold.solver.core.impl.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Immutable {@link Map} backed by a flat array using perfect hashing on identity hash codes.
 * All keys must be known at construction time and must have distinct identity hash codes.
 * Uses reference equality (==) for key lookup. Null keys and null values are both allowed.
 * Lookups are O(1) with no bucket chaining.
 */
@NullMarked
public final class ImmutableIdentityPerfectHashMap<@Nullable K, V> extends AbstractMap<K, V> {

    private final int size;
    private final int modulus;
    private final @Nullable Object @Nullable [] keys;
    private final @Nullable Object @Nullable [] values;
    private final boolean hasNullKey;
    private final @Nullable V nullKeyValue;
    private final Set<Entry<K, V>> entrySetCache;

    @SuppressWarnings("unchecked")
    public ImmutableIdentityPerfectHashMap(Map<K, V> source) {
        this.size = source.size();
        // Pre-allocate at source.size(); at most 1 slot unused if null key present.
        var keyArray = new Object[size];
        var valueArray = new Object[size];
        var hashCodes = new int[size];
        var nonNullCount = 0;
        var foundNullKey = false;
        var foundNullKeyValue = (V) null;
        // Single pass: separate null key inline, collect non-null keys.
        for (var entry : source.entrySet()) {
            var k = entry.getKey();
            if (k == null) {
                foundNullKey = true;
                foundNullKeyValue = entry.getValue();
                continue;
            }
            keyArray[nonNullCount] = k;
            valueArray[nonNullCount] = entry.getValue();
            hashCodes[nonNullCount] = System.identityHashCode(k);
            nonNullCount++;
        }
        this.hasNullKey = foundNullKey;
        this.nullKeyValue = foundNullKeyValue;

        if (nonNullCount == 0) {
            this.modulus = 1;
            this.keys = null;
            this.values = null;
            this.entrySetCache = buildEntrySet();
            return;
        }
        // Duplicate hash code check MUST run before findModulus — identical hash codes
        // cause findModulus to loop forever (no M ever separates them).
        var seenHashCodes = new HashSet<Integer>(nonNullCount);
        for (var j = 0; j < nonNullCount; j++) {
            if (!seenHashCodes.add(hashCodes[j])) {
                throw new IllegalArgumentException(
                        "The key (%s) already has the same hash code (%d) as another key."
                                .formatted(keyArray[j], hashCodes[j]));
            }
        }
        this.modulus = findModulus(hashCodes, nonNullCount);
        this.keys = new Object[modulus];
        this.values = new Object[modulus];
        for (var j = 0; j < nonNullCount; j++) {
            var slot = Math.floorMod(hashCodes[j], modulus);
            this.keys[slot] = keyArray[j];
            this.values[slot] = valueArray[j];
        }
        this.entrySetCache = buildEntrySet();
    }

    private static int findModulus(int[] hashCodes, int length) {
        outer: for (var m = length;; m++) {
            var seen = new boolean[m];
            for (var i = 0; i < length; i++) {
                var slot = Math.floorMod(hashCodes[i], m);
                if (seen[slot]) {
                    continue outer;
                }
                seen[slot] = true;
            }
            return m;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable V get(Object key) {
        if (key == null) {
            return hasNullKey ? nullKeyValue : null;
        }
        if (keys == null) {
            return null;
        }
        var slot = Math.floorMod(System.identityHashCode(key), modulus);
        var candidate = keys[slot];
        if (candidate != key) {
            return null;
        }
        return (V) values[slot];
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            return hasNullKey;
        }
        if (keys == null) {
            return false;
        }
        var slot = Math.floorMod(System.identityHashCode(key), modulus);
        return keys[slot] == key;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return entrySetCache;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @SuppressWarnings("unchecked")
    private Set<Entry<K, V>> buildEntrySet() {
        return new AbstractSet<>() {
            @Override
            public int size() {
                return size;
            }

            @Override
            public boolean contains(Object o) {
                if (!(o instanceof Map.Entry<?, ?> entry)) {
                    return false;
                }
                var k = entry.getKey();
                if (k == null) {
                    return hasNullKey && Objects.equals(nullKeyValue, entry.getValue());
                }
                if (keys == null || values == null) {
                    return false;
                }
                var slot = Math.floorMod(System.identityHashCode(k), modulus);
                return keys[slot] == k && Objects.equals(values[slot], entry.getValue());
            }

            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new Iterator<>() {
                    // Yield null key first (if present), then scan the array.
                    private boolean nullKeyEmitted = !hasNullKey;
                    private int index = 0;
                    private int remaining = size;

                    @Override
                    public boolean hasNext() {
                        return remaining > 0;
                    }

                    @Override
                    public Entry<K, V> next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        remaining--;
                        if (!nullKeyEmitted) {
                            nullKeyEmitted = true;
                            return new SimpleImmutableEntry<>(null, nullKeyValue);
                        }
                        while (keys[index] == null) {
                            index++;
                        }
                        var k = (K) keys[index];
                        var v = (V) values[index];
                        index++;
                        return new SimpleImmutableEntry<>(k, v);
                    }
                };
            }
        };
    }
}
