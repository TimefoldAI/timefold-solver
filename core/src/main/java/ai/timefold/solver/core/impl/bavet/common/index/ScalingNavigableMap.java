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
 * Keys are always compared/stored/built by their natural {@link Comparable} order, in both the array and the
 * {@link TreeMap}, regardless of {@code reversed} - never by an explicit {@link java.util.Comparator}. A
 * {@link TreeMap} without a comparator uses a faster lookup path internally ({@code getEntry()}, direct
 * {@code compareTo()}) than one with a comparator ({@code getEntryUsingComparator()}, extra dispatch through
 * {@code Comparator.compare()}) - since point lookups ({@link #get}/{@link #getOrCreate}/{@link #remove}) don't
 * care about direction at all, giving them an explicit comparator just to support reversed iteration would be
 * paying that tax on every single lookup for no reason. {@code reversed} only changes how {@link #cursorFromStart()}
 * walks the entries: forward for the array (natural start/step) or the tree ({@code entrySet()}), backward
 * otherwise (reversed start/step, or {@link NavigableMap#descendingMap()} - an O(1) view, not a rebuild).
 *
 * @param <K> the key type; must be mutually comparable via {@link Comparable}
 * @param <V> the value type
 */
@NullMarked
final class ScalingNavigableMap<K, V> {

    // Package-private (not private) so tests in this package can read these directly
    // instead of via reflection or a dedicated getter.
    static final int ARRAY_THRESHOLD = 32;
    private static final int INITIAL_ARRAY_CAPACITY = 4;

    private final boolean reversed;

    boolean belowThreshold = true;
    // Interleaved: entries[2i] = key i, entries[2i + 1] = value i; always sorted ascending by natural order,
    // regardless of `reversed` - only the cursor's scan direction flips, never the storage order.
    private Object[] entries;
    private int size = 0;
    // Allocated lazily by treeify(); non-null exactly when !belowThreshold. Never built with an explicit
    // comparator - see the class javadoc for why.
    private @Nullable TreeMap<K, V> treeMap;

    ScalingNavigableMap(boolean reversed) {
        this.reversed = reversed;
        this.entries = new Object[INITIAL_ARRAY_CAPACITY * 2];
    }

    @SuppressWarnings("unchecked")
    @Nullable
    V get(K key) {
        if (belowThreshold) {
            var index = indexOfKey(key);
            return index >= 0 ? (V) entries[index * 2 + 1] : null;
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
            return (V) entries[index * 2 + 1];
        }
        var value = valueSupplier.get();
        insertIntoArray(-(index + 1), key, value);
        if (size > ARRAY_THRESHOLD) {
            treeify();
        }
        return value;
    }

    private void insertIntoArray(int insertionPoint, K key, V value) {
        if (size * 2 == entries.length) {
            entries = Arrays.copyOf(entries, entries.length * 2);
        }
        var shiftCount = (size - insertionPoint) * 2;
        if (shiftCount > 0) {
            System.arraycopy(entries, insertionPoint * 2, entries, (insertionPoint + 1) * 2, shiftCount);
        }
        var pos = insertionPoint * 2;
        entries[pos] = key;
        entries[pos + 1] = value;
        size++;
    }

    @SuppressWarnings("unchecked")
    private void treeify() {
        var newTreeMap = new TreeMap<K, V>();
        for (var i = 0; i < size; i++) {
            var pos = i * 2;
            newTreeMap.put((K) entries[pos], (V) entries[pos + 1]);
        }
        treeMap = newTreeMap;
        belowThreshold = false;
        Arrays.fill(entries, null);
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
        var shiftCount = (size - index - 1) * 2;
        if (shiftCount > 0) {
            System.arraycopy(entries, (index + 1) * 2, entries, index * 2, shiftCount);
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

    @SuppressWarnings("unchecked")
    private int indexOfKey(K key) {
        var low = 0;
        var high = size - 1;
        while (low <= high) {
            var mid = (low + high) >>> 1;
            var comparison = ((Comparable<? super K>) entries[mid * 2]).compareTo(key);
            if (comparison < 0) {
                low = mid + 1;
            } else if (comparison > 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -(low + 1);
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
            return (K) entries[index * 2];
        }

        @Override
        public V value() {
            return (V) entries[index * 2 + 1];
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
