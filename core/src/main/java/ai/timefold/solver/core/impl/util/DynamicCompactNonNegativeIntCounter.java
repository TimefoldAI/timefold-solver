package ai.timefold.solver.core.impl.util;

import java.util.Arrays;

import org.jspecify.annotations.NullMarked;

/**
 * A class representing a non-negative int-to-int map that is compacted so the backing int
 * arrays are as small as possible.
 */
@NullMarked
public final class DynamicCompactNonNegativeIntCounter {
    /**
     * Sentinel for removed/missing entries in the {@link #keys} array
     */
    private final static int MISSING = -1;

    private int[] keys;
    private int[] counts;

    public DynamicCompactNonNegativeIntCounter() {
        keys = new int[0];
        counts = new int[0];
    }

    /**
     * Used for testing
     */
    int capacity() {
        return keys.length;
    }

    /**
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

    public int getCount(int key) {
        var index = getEntryIndex(key);
        if (index == MISSING) {
            return 0;
        }
        return counts[index];
    }

    public void increment(int key) {
        var index = getEntryIndex(key);
        if (index == MISSING) {
            createEntry(key);
        } else {
            counts[index]++;
        }
    }

    public void decrement(int key) {
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
