package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * An ordered {@code key -> value} structure,
 * backed by a sorted array while the distinct key count is small,
 * and by a {@link TreeMap} once it isn't.
 * <p>
 * Despite the name, this does not implement {@link NavigableMap}.
 * It exposes only the handful of operations {@link ComparisonIndexer} needs;
 * there is no {@code ceilingKey()}, {@code subMap()}, etc.
 * <p>
 * Most buckets stay small for the life of the solver,
 * where a sorted array is more cache-friendly than a red-black tree;
 * a handful of buckets in adversarial datasets may grow large,
 * where a {@link TreeMap} remains the safer O(log n) choice.
 * The switch from array to tree ({@link #treeify()}) is one-way:
 * once a bucket has proven it can grow large,
 * going back to an array on removal only reintroduces the cost of resizing/copying
 * for a bucket that has already demonstrated it churns near or above the threshold.
 * <p>
 * Keys and values are kept in two parallel arrays, not one interleaved array. Point lookups
 * ({@link #get}/{@link #getOrCreate}/{@link #remove}) binary-search over keys only and never touch values until a
 * match is found; a separate {@code keys} array keeps every key a search might probe packed together, instead of
 * spread across double the memory by unneeded interleaved value slots. Point lookups happen once per tuple
 * insertion/removal, more often than the full range scans that would actually benefit from key+value being
 * co-located, so this is the better default for this access pattern.
 * <p>
 * Keys are always compared/stored/built by their natural {@link Comparable} order, in both the array and the
 * {@link TreeMap}, regardless of {@code reversed} - never by an explicit {@link java.util.Comparator}. A
 * {@link TreeMap} without a comparator uses a faster lookup path internally ({@code getEntry()}, direct
 * {@code compareTo()}) than one with a comparator ({@code getEntryUsingComparator()}, extra dispatch through
 * {@code Comparator.compare()}) - since point lookups don't care about direction at all, giving them an explicit
 * comparator just to support reversed iteration would be paying that tax on every single lookup for no reason.
 * {@code reversed} only changes how {@link #cursorFromStart()} walks the entries: forward for the array (natural
 * start/step) or the tree ({@code entrySet()}), backward otherwise (reversed start/step, or
 * {@link NavigableMap#descendingMap()} - an O(1) view, not a rebuild).
 *
 * @param <K> the key type; must be mutually comparable via {@link Comparable}
 * @param <V> the value type
 */
@NullMarked
final class ScalingNavigableMap<K extends Comparable<K>, V> {

    // Package-private (not private) so tests in this package can read these directly
    // instead of via reflection or a dedicated getter.
    static final int ARRAY_THRESHOLD = 32;
    private static final int INITIAL_ARRAY_CAPACITY = 4;

    private final boolean reversed;

    boolean belowThreshold = true;
    // keys[0..size) sorted ascending by natural order (regardless of `reversed`), values[0..size) parallel to it.
    private Object[] keys;
    private Object[] values;
    private int size = 0;
    // Allocated lazily by treeify(); non-null exactly when !belowThreshold. Never built with an explicit
    // comparator - see the class javadoc for why.
    private @Nullable TreeMap<K, V> treeMap;

    ScalingNavigableMap(boolean reversed) {
        this.reversed = reversed;
        this.keys = new Object[INITIAL_ARRAY_CAPACITY];
        this.values = new Object[INITIAL_ARRAY_CAPACITY];
    }

    @SuppressWarnings("unchecked")
    @Nullable
    V get(K key) {
        if (belowThreshold) {
            var index = indexOfKey(key);
            return index >= 0 ? (V) values[index] : null;
        } else {
            return treeMap.get(key);
        }
    }

    V getOrCreate(K key, Supplier<V> valueSupplier) {
        return belowThreshold ? getOrCreateArray(key, valueSupplier) : getOrCreateTree(key, valueSupplier);
    }

    private V getOrCreateTree(K key, Supplier<V> valueSupplier) {
        // Avoids computeIfAbsent in order to not create lambdas on the hot path.
        var value = treeMap.get(key);
        if (value == null) {
            value = valueSupplier.get();
            treeMap.put(key, value);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private V getOrCreateArray(K key, Supplier<V> valueSupplier) {
        var index = indexOfKey(key);
        if (index >= 0) {
            return (V) values[index];
        }
        var value = valueSupplier.get();
        insertIntoArray(-(index + 1), key, value);
        if (size > ARRAY_THRESHOLD) {
            treeify();
        }
        return value;
    }

    private void insertIntoArray(int insertionPoint, K key, V value) {
        if (size == keys.length) {
            keys = Arrays.copyOf(keys, keys.length * 2);
            values = Arrays.copyOf(values, values.length * 2);
        }
        var shiftCount = size - insertionPoint;
        if (shiftCount > 0) {
            System.arraycopy(keys, insertionPoint, keys, insertionPoint + 1, shiftCount);
            System.arraycopy(values, insertionPoint, values, insertionPoint + 1, shiftCount);
        }
        keys[insertionPoint] = key;
        values[insertionPoint] = value;
        size++;
    }

    @SuppressWarnings("unchecked")
    private void treeify() {
        var newTreeMap = new TreeMap<K, V>();
        for (var i = 0; i < size; i++) {
            newTreeMap.put((K) keys[i], (V) values[i]);
        }
        treeMap = newTreeMap;
        belowThreshold = false;
        Arrays.fill(keys, null);
        Arrays.fill(values, null);
    }

    void remove(K key) {
        if (belowThreshold) {
            removeFromArray(key);
        } else {
            treeMap.remove(key);
        }
    }

    private void removeFromArray(K key) {
        var index = indexOfKey(key);
        var shiftCount = size - index - 1;
        if (shiftCount > 0) {
            System.arraycopy(keys, index + 1, keys, index, shiftCount);
            System.arraycopy(values, index + 1, values, index, shiftCount);
        }
        size--;
    }

    int size() {
        return belowThreshold ? size : treeMap.size();
    }

    boolean isEmpty() {
        return belowThreshold ? size == 0 : treeMap.isEmpty();
    }

    Cursor<K, V> cursorFromStart() {
        if (belowThreshold) {
            return new ArrayCursor();
        }
        var entryIterator = reversed ? treeMap.descendingMap().entrySet().iterator() : treeMap.entrySet().iterator();
        return new TreeCursor<>(entryIterator);
    }

    private int indexOfKey(K key) {
        return Arrays.binarySearch(keys, 0, size, key);
    }

    /**
     * Variant of the iterator that allows to return two things at once,
     * without allocating any carrier type.
     *
     * @param <K>
     * @param <V>
     */
    sealed interface Cursor<K, V> {

        /**
         * Moves to the next entry. Must be called before the first {@link #key()}/{@link #value()} call.
         *
         * @return false if there is no next entry; {@link #key()}/{@link #value()} must not be called in that case
         */
        boolean advance();

        K key();

        V value();

    }

    @SuppressWarnings("unchecked")
    private final class ArrayCursor implements Cursor<K, V> {

        private final int step = reversed ? -1 : 1;
        private int index = reversed ? size : -1;

        @Override
        public boolean advance() {
            index += step;
            return index >= 0 && index < size;
        }

        @Override
        public K key() {
            return (K) keys[index];
        }

        @Override
        public V value() {
            return (V) values[index];
        }

    }

    private static final class TreeCursor<K, V> implements Cursor<K, V> {

        private final Iterator<Map.Entry<K, V>> iterator;
        private Map.@Nullable Entry<K, V> current;

        private TreeCursor(Iterator<Map.Entry<K, V>> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean advance() {
            if (!iterator.hasNext()) {
                return false;
            }
            current = iterator.next();
            return true;
        }

        @Override
        public K key() {
            return current.getKey();
        }

        @Override
        public V value() {
            return current.getValue();
        }

    }

}
