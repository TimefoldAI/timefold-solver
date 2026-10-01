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
 * direct {@code compareTo()}) than one with a comparator ({@code getEntryUsingComparator()}).
 * <p>
 * {@link ComparisonIndexer} branches on {@link #arrayBased} and calls {@link #keyAt}/{@link #valueAt} for range scans
 * (plain, final-class, trivially inlined methods; benchmarked to be optimal);
 * only the get/put/remove/treeify machinery is actually encapsulated here.
 * <p>
 * The switch from array to tree ({@link #treeify()}) is one-way, and is triggered by two
 * independent conditions, because array mode has two unrelated costs:
 * <ul>
 * <li>Filling a sorted array with n distinct keys costs O(n) element copies per insert,
 * so ~n²/2 in total - bounded by the final size, and paid once.
 * Negligible at hundreds of keys, prohibitive at tens of thousands;
 * hence the hard {@link #MAXIMUM_ARRAY_SIZE} cap.</li>
 * <li>A bucket that keeps adding and removing distinct keys while large pays that O(n) copy
 * over and over, with no bound in time at all. That is what {@link #CHURN_TOLERANCE} detects.</li>
 * </ul>
 * Growth on its own therefore proves nothing. A bucket keyed on immutable facts fills up once while
 * the problem is loaded and is read-only for the rest of the solve,
 * where a sorted array stays more cache-friendly than a red-black tree indefinitely;
 * only removals observed at scale justify the tree.
 * Promotion remains one-way: a bucket that churned once will churn again,
 * and demoting on removal would only reintroduce the resize/copy cost
 * it has just demonstrated it cannot afford.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@NullMarked
final class ScalingNavigableMap<K extends Comparable<K>, V> {

    private static final Object[] EMPTY_ARRAY = new Object[0];

    /**
     * The size above which a *churning* bucket is better off as a tree.
     * Not a promotion trigger on its own; see CHURN_TOLERANCE.
     * Established experimentally: get/scan/churn all favor 64 over 32,
     * while pushing to 96 or 128 improved scan only marginally and made get and churn worse.
     * Package-private: tests in this package read {@link #isArrayBased()} and the constants below.
     */
    static final int ARRAY_THRESHOLD = 64;
    /**
     * Hard cap on array mode, independent of churn, because building the array is O(n) per insert:
     * ~n²/2 element copies for n distinct keys.
     * At 1024 that is ~5*10^5 copies (once, negligible) for 10 binary-search probes per get;
     * at 16384 it would be ~1.3*10^8, and every extra probe is a dependent load into a Comparable,
     * i.e. a likely cache miss on the much hotter get path.
     * Absolute rather than a multiple of {@link #ARRAY_THRESHOLD}:
     * the two answer unrelated questions,
     * and retuning one does not imply retuning the other.
     * A power of two also lands exactly on a doubling step from {@link #MINIMUM_ARRAY_CAPACITY},
     * so a maxed-out bucket wastes no array capacity.
     */
    static final int MAXIMUM_ARRAY_SIZE = 1024;
    /**
     * How many at-scale removals count as proof of churn.
     * Deliberately not 1:
     * an isolated removal can come from a problem change, a pinning change,
     * or a move that happens to empty a single downstream indexer,
     * and permanently treeifying an otherwise read-only bucket over one such event
     * costs far more than tolerating it -
     * 32 false events at size 1024 is ~3*10^4 reference copies,
     * once, for the lifetime of the bucket.
     * A genuinely churning bucket performs structural mutations millions of times per second
     * and trips this within microseconds.
     */
    static final int CHURN_TOLERANCE = 32;
    private static final int MINIMUM_ARRAY_CAPACITY = 4;

    // keys[0, size) sorted ascending by natural order
    private @Nullable Object[] keys = EMPTY_ARRAY;
    // values[0, size) parallel to keys[]
    private @Nullable Object[] values = EMPTY_ARRAY;
    private int size = 0;
    // Distinct-key removals seen while at or above ARRAY_THRESHOLD; see CHURN_TOLERANCE.
    private int churnAtScaleCount = 0;
    // Allocated lazily by treeify(); non-null exactly when !arrayBased.
    @Nullable
    private TreeMap<K, V> treeMap;

    ScalingNavigableMap() {
        // No out-of-package instances.
    }

    boolean isArrayBased() {
        return treeMap == null;
    }

    @Nullable
    public V get(K key) {
        if (treeMap == null) {
            var index = indexOf(key);
            return index >= 0 ? valueAt(index) : null;
        } else {
            return treeMap.get(key);
        }
    }

    public V getOrCreate(K key, Supplier<V> valueSupplier) {
        return isArrayBased() ? getOrCreateArray(key, valueSupplier) : getOrCreateTree(key, valueSupplier);
    }

    private V getOrCreateTree(K key, Supplier<V> valueSupplier) {
        // Avoids computeIfAbsent to not create lambdas on the hot path.
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
        if (size + 1 > MAXIMUM_ARRAY_SIZE) {
            treeify();
            return getOrCreateTree(key, valueSupplier);
        }
        var value = valueSupplier.get();
        insertIntoArray(-(index + 1), key, value);
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
        Arrays.fill(keys, 0, size, null);
        Arrays.fill(values, 0, size, null);
        size = -1;
    }

    /**
     * No-op if {@code key} isn't present.
     * If in array-mode, consider using {@link #removeAt(int)} instead,
     * if the position is already known.
     */
    public void remove(K key) {
        if (treeMap == null) {
            var index = indexOf(key);
            if (index >= 0) {
                removeAt(index);
            }
        } else {
            treeMap.remove(key);
        }
    }

    /**
     * Array-mode only.
     * {@code index} must be a valid, currently occupied position,
     * as returned by a non-negative {@link #indexOf(Comparable)}.
     * Exposed (alongside {@link #indexOf(Comparable)}) so a caller that already located an entry via {@link #indexOf} -
     * typically to inspect its value first, like {@link ComparisonIndexer#remove} does -
     * can remove it without a second, redundant binary search for the same key.
     * May {@link #treeify()} as its last step,
     * so callers must not read array-mode state afterward
     * ({@link #size()}, {@link #keyAt}, {@link #valueAt}, {@link #indexOf}).
     * Every current caller already treats this as its last operation on the map.
     */
    void removeAt(int index) {
        var shiftCount = size - index - 1;
        if (shiftCount > 0) {
            System.arraycopy(keys, index + 1, keys, index, shiftCount);
            System.arraycopy(values, index + 1, values, index, shiftCount);
        }
        // Whether shifted or not, the last occupied slot is now stale; null it so it doesn't outlive size.
        keys[size - 1] = null;
        values[size - 1] = null;
        size--;
        // Tested after the decrement: the bucket is still large, and still shrinking.
        churnAtScaleCount++;
        if (size >= ARRAY_THRESHOLD && churnAtScaleCount >= CHURN_TOLERANCE) {
            treeify();
        }
    }

    public int size() {
        return isArrayBased() ? size : treeMap.size();
    }

    public boolean isEmpty() {
        return isArrayBased() ? size == 0 : treeMap.isEmpty();
    }

    /**
     * Array-mode range scans only.
     */
    @SuppressWarnings("unchecked")
    K keyAt(int index) {
        return (K) keys[index];
    }

    /**
     * Array-mode only.
     */
    @SuppressWarnings("unchecked")
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
     * Exposed (see {@link #removeAt}) so a caller that needs both the value and, conditionally, to remove it -
     * like {@link ComparisonIndexer#remove} -
     * can reuse the result instead of searching for {@code key} a second time.
     */
    int indexOf(K key) {
        return Arrays.binarySearch(keys, 0, size, key);
    }

}
