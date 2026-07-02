package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Arrays;
import java.util.Comparator;
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
 * Keys and values are kept in two parallel {@code Object[]} arrays.
 * Point lookups ({@link #get}/{@link #getOrCreate}/{@link #remove}) binary-search over keys only
 * and never touch values until a match is found;
 * a separate {@code keys} array keeps every key a search might probe packed together,
 * instead of spread across double the memory by unneeded interleaved value slots.
 * <p>
 * Keys are always compared/stored/built by their natural {@link Comparable} order,
 * never by an explicit {@link Comparator}.
 * A {@link TreeMap} without a comparator uses a faster lookup path internally ({@code getEntry()},
 * direct {@code compareTo()}) than one with a comparator ({@code getEntryUsingComparator()}.
 * <p>
 * {@link ComparisonIndexer} branches on {@link #arrayBased} and calls {@link #keyAt}/{@link #valueAt} for range scans
 * (plain, final-class, trivially inlined methods; benchmarked to be optimal);
 * only the get/put/remove/treeify machinery is actually encapsulated here.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@NullMarked
final class ScalingNavigableMap<K extends Comparable<K>, V> {

    private static final Object[] EMPTY_ARRAY = new Object[0];

    // Package-private: tests in this package read arrayBased/ARRAY_THRESHOLD
    // The threshold was established experimentally.
    static final int ARRAY_THRESHOLD = 32;
    private static final int MINIMUM_ARRAY_CAPACITY = 4;

    boolean arrayBased = true;
    // keys[0, size) sorted ascending by natural order
    private @Nullable Object[] keys = EMPTY_ARRAY;
    // values[0, size) parallel to keys[]
    private @Nullable Object[] values = EMPTY_ARRAY;
    private int size = 0;
    // Allocated lazily by treeify(); non-null exactly when !arrayBased.
    @Nullable
    private TreeMap<K, V> treeMap;

    ScalingNavigableMap() {
        // No out-of-package instances.
    }

    @Nullable
    public V get(K key) {
        if (arrayBased) {
            var index = indexOf(key);
            return index >= 0 ? valueAt(index) : null;
        } else {
            return treeMap.get(key);
        }
    }

    public V getOrCreate(K key, Supplier<V> valueSupplier) {
        return arrayBased ? getOrCreateArray(key, valueSupplier) : getOrCreateTree(key, valueSupplier);
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

    private V getOrCreateArray(K key, Supplier<V> valueSupplier) {
        var index = indexOf(key);
        if (index >= 0) {
            return valueAt(index);
        }
        var value = valueSupplier.get();
        insertIntoArray(-(index + 1), key, value);
        if (size() > ARRAY_THRESHOLD) {
            treeify();
        }
        return value;
    }

    private void insertIntoArray(int insertionPoint, K key, V value) {
        if (size == keys.length) {
            var minSize = Math.max(keys.length * 2, MINIMUM_ARRAY_CAPACITY);
            keys = Arrays.copyOf(keys, minSize);
            values = Arrays.copyOf(values, minSize);
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

    private void treeify() {
        var newTreeMap = new TreeMap<K, V>();
        for (var i = 0; i < size; i++) {
            newTreeMap.put(keyAt(i), valueAt(i));
        }
        treeMap = newTreeMap;
        arrayBased = false;
        Arrays.fill(keys, 0, size, null);
        Arrays.fill(values, 0, size, null);
        size = -1;
    }

    /**
     * If in array-mode, consider using {@link #removeAt(int)} instead,
     * if the position is already known.
     */
    public void remove(K key) {
        if (arrayBased) {
            removeAt(indexOf(key));
        } else {
            treeMap.remove(key);
        }
    }

    /**
     * Array-mode only.
     * Exposed (alongside {@link #indexOf}) so a caller that already located an entry via {@link #indexOf} -
     * typically to inspect its value first, like {@link ComparisonIndexer#remove} does -
     * can remove it without a second, redundant binary search for the same key.
     */
    void removeAt(int index) {
        var shiftCount = size - index - 1;
        if (shiftCount > 0) {
            System.arraycopy(keys, index + 1, keys, index, shiftCount);
            keys[size - 1] = null;
            System.arraycopy(values, index + 1, values, index, shiftCount);
            values[size - 1] = null;
        }
        size--;
    }

    public int size() {
        return arrayBased ? size : treeMap.size();
    }

    public boolean isEmpty() {
        return arrayBased ? size == 0 : treeMap.isEmpty();
    }

    /**
     * Array-mode range scans only.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    K keyAt(int index) {
        return (K) keys[index];
    }

    /**
     * Array-mode only.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    V valueAt(int index) {
        return (V) values[index];
    }

    /**
     * Tree-mode only.
     */
    Map.Entry<K, V> firstEntry() {
        return treeMap.firstEntry();
    }

    /**
     * Tree-mode only.
     */
    Iterator<Map.Entry<K, V>> iterator(boolean reversed) {
        return reversed ? treeMap.descendingMap().entrySet().iterator() : treeMap.entrySet().iterator();
    }

    /**
     * Array-mode only.
     * Same contract as {@link Arrays#binarySearch(Object[], int, int, Object)}:
     * the index of {@code key} if present, else {@code -(insertionPoint) - 1}.
     * Exposed (see {@link #removeAt}) so a caller that needs both the value
     * and, conditionally, to remove it - like {@link ComparisonIndexer#remove} -
     * can reuse the result instead of searching for {@code key} a second time.
     */
    int indexOf(K key) {
        return Arrays.binarySearch(keys, 0, size, key);
    }

}
