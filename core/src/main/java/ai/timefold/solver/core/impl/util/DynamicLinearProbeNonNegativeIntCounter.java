package ai.timefold.solver.core.impl.util;

import java.util.Arrays;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A class representing a non-negative int-to-int map that is compacted so the backing int
 * arrays are as small as possible.
 * <p>
 * IMPORTANT: As the class does a linear probe, it is only appropriate to use
 * when counting up to a few dozen keys. When using more keys, use a
 * {@link java.util.Map} instead.
 */
@NullMarked
public final class DynamicLinearProbeNonNegativeIntCounter {
    /**
     * Sentinel for removed/missing entries in the {@link #keys} array
     */
    private static final int MISSING = -1;

    @Nullable
    private int[] keys;

    @Nullable
    private int[] counts;

    public DynamicLinearProbeNonNegativeIntCounter() {
        keys = null;
        counts = null;
    }

    /**
     * Used for testing
     */
    int capacity() {
        if (keys == null) {
            return 0;
        }
        return keys.length;
    }

    /**
     * Does a linear probe to find the entry corresponding to a given key.
     * 
     * @param key The key of the entry
     * @return The index of key in {@link #keys}, or {@link #MISSING} if key not in the map
     */
    private int getEntryIndex(int key) {
        for (var i = 0; i < keys.length; i++) {
            if (keys[i] == key) {
                return i;
            }
        }
        return MISSING;
    }

    /**
     * Creates an entry for key and set its count to 1.
     * Does a linear probe to find a {@link #MISSING} entry in the keys/counts arrays,
     * or expand the arrays if all entries have a key.
     *
     * @param key The key of the entry to be created
     */
    private void createEntry(int key) {
        // Pick a missing entry first
        for (var i = 0; i < keys.length; i++) {
            if (keys[i] == MISSING) {
                // Found a missing entry; so use it
                keys[i] = key;
                counts[i] = 1;
                return;
            }
        }

        // No missing entries, so expand the arrays
        var keyIndex = keys.length;
        var newSize = keyIndex + 1;

        keys = Arrays.copyOf(keys, newSize);
        counts = Arrays.copyOf(counts, newSize);

        keys[keyIndex] = key;
        counts[keyIndex] = 1;
    }

    /**
     * Get the count of key, or 0 if it not present in the counter.
     */
    public int getCount(int key) {
        if (keys == null) {
            return 0;
        }
        var index = getEntryIndex(key);
        if (index == MISSING) {
            return 0;
        }
        return counts[index];
    }

    /**
     * Increment the count of key, creating an entry for it if none is present.
     */
    public void increment(int key) {
        if (keys == null) {
            // Empty map, so initialize the keys and counts arrays
            keys = new int[] { key };
            counts = new int[] { 1 };
            return;
        }
        var index = getEntryIndex(key);
        if (index == MISSING) {
            createEntry(key);
        } else {
            counts[index]++;
        }
    }

    /**
     * Decrement the count of key, deleting the entry for it if count is 0.
     */
    public void decrement(int key) {
        if (keys == null) {
            throw new IllegalArgumentException("key %d is not in the map %s".formatted(key, this));
        }

        var index = getEntryIndex(key);
        if (index == MISSING) {
            throw new IllegalArgumentException("key %d is not in the map %s".formatted(key, this));
        } else {
            var newCount = --counts[index];
            if (newCount == 0) {
                // Count is 0, so erase the key from the map,
                // so a new key can reclaim its space
                keys[index] = MISSING;
            }
        }
    }

    @Override
    public String toString() {
        if (keys == null) {
            return "{}";
        }
        var out = new StringBuilder();
        var isFirst = true;
        out.append("{");
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != MISSING) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    out.append(", ");
                }
                out.append(keys[i]).append(": ").append(counts[i]);
            }
        }
        out.append("}");
        return out.toString();
    }
}
