package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;
import ai.timefold.solver.core.impl.util.ListEntry;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Backed by a sorted array while the distinct key count is small, and by a {@link TreeMap} once it isn't.
 * <p>
 * Most buckets stay small for the life of the solver, where a sorted array is more cache-friendly than a
 * red-black tree; a handful of buckets in adversarial datasets may grow large, where a {@link TreeMap} remains
 * the safer O(log n) choice. The switch from array to tree ({@link #treeify()}) is one-way: once a bucket has
 * proven it can grow large, going back to an array on removal only reintroduces the cost of resizing/copying for
 * a bucket that has already demonstrated it churns near or above the threshold.
 */
@NullMarked
final class ComparisonIndexer<T, Key_ extends Comparable<Key_>>
        implements Indexer<T> {

    private static final int ARRAY_THRESHOLD = 32;
    private static final int INITIAL_ARRAY_CAPACITY = 4;

    private final KeyUnpacker<Key_> keyUnpacker;
    private final Supplier<Indexer<T>> downstreamIndexerSupplier;
    private final boolean reverseOrder;
    private final boolean hasOrEquals;

    private boolean belowThreshold;
    // Below threshold: keys[0..size) sorted ascending (regardless of reverseOrder), indexers[0..size) parallel to it.
    private Key_[] keys;
    private Indexer<T>[] indexers;
    private int size;
    // Allocated lazily by treeify(); non-null exactly when !belowThreshold.
    private @Nullable NavigableMap<Key_, Indexer<T>> comparisonMap;

    /**
     * @param comparisonJoinerType the type of comparison to use
     * @param keyUnpacker determines if it immediately goes to a {@link LeafIndexer} or if it uses a {@link CompositeKey}.
     * @param downstreamIndexerSupplier the supplier of the downstream indexer
     */
    @SuppressWarnings("unchecked")
    public ComparisonIndexer(JoinerType comparisonJoinerType, KeyUnpacker<?> keyUnpacker,
            Supplier<Indexer<T>> downstreamIndexerSupplier) {
        this.keyUnpacker = Objects.requireNonNull((KeyUnpacker<Key_>) keyUnpacker);
        this.downstreamIndexerSupplier = Objects.requireNonNull(downstreamIndexerSupplier);
        // For GT/GTE, the iteration order is reversed.
        // This allows us to iterate over the entire map from the start, stopping when the threshold is reached.
        // This is done so that we can avoid using head/tail sub maps, which are expensive.
        this.reverseOrder =
                comparisonJoinerType == JoinerType.GREATER_THAN || comparisonJoinerType == JoinerType.GREATER_THAN_OR_EQUAL;
        this.hasOrEquals = comparisonJoinerType == JoinerType.GREATER_THAN_OR_EQUAL
                || comparisonJoinerType == JoinerType.LESS_THAN_OR_EQUAL;
        this.belowThreshold = true;
        this.keys = (Key_[]) new Comparable[INITIAL_ARRAY_CAPACITY];
        this.indexers = (Indexer<T>[]) new Indexer[INITIAL_ARRAY_CAPACITY];
    }

    @Override
    public ListEntry<T> put(Object compositeKey, T tuple) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var downstreamIndexer = belowThreshold ? putArray(indexKey) : putTree(indexKey);
        return downstreamIndexer.put(compositeKey, tuple);
    }

    private Indexer<T> putTree(Key_ indexKey) {
        // Avoids computeIfAbsent in order to not create lambdas on the hot path.
        var downstreamIndexer = comparisonMap.get(indexKey);
        if (downstreamIndexer == null) {
            downstreamIndexer = downstreamIndexerSupplier.get();
            comparisonMap.put(indexKey, downstreamIndexer);
        }
        return downstreamIndexer;
    }

    private Indexer<T> putArray(Key_ indexKey) {
        var searchResult = Arrays.binarySearch(keys, 0, size, indexKey);
        if (searchResult >= 0) {
            return indexers[searchResult];
        }
        var downstreamIndexer = downstreamIndexerSupplier.get();
        insertIntoArrays(-(searchResult + 1), indexKey, downstreamIndexer);
        if (size > ARRAY_THRESHOLD) {
            treeify();
        }
        return downstreamIndexer;
    }

    private void insertIntoArrays(int insertionPoint, Key_ indexKey, Indexer<T> downstreamIndexer) {
        if (size == keys.length) {
            var newCapacity = keys.length * 2;
            keys = Arrays.copyOf(keys, newCapacity);
            indexers = Arrays.copyOf(indexers, newCapacity);
        }
        var shiftCount = size - insertionPoint;
        if (shiftCount > 0) {
            System.arraycopy(keys, insertionPoint, keys, insertionPoint + 1, shiftCount);
            System.arraycopy(indexers, insertionPoint, indexers, insertionPoint + 1, shiftCount);
        }
        keys[insertionPoint] = indexKey;
        indexers[insertionPoint] = downstreamIndexer;
        size++;
    }

    private void treeify() {
        NavigableMap<Key_, Indexer<T>> newComparisonMap =
                reverseOrder ? new TreeMap<>(Comparator.reverseOrder()) : new TreeMap<>();
        for (var i = 0; i < size; i++) {
            newComparisonMap.put(keys[i], indexers[i]);
        }
        comparisonMap = newComparisonMap;
        belowThreshold = false;
        // Arrays are left populated (harmless: bounded size, elements stay reachable via comparisonMap anyway).
    }

    @Override
    public void remove(Object compositeKey, ListEntry<T> entry) {
        var indexKey = keyUnpacker.apply(compositeKey);
        if (belowThreshold) {
            removeArray(compositeKey, indexKey, entry);
        } else {
            removeTree(compositeKey, indexKey, entry);
        }
    }

    private void removeTree(Object compositeKey, Key_ indexKey, ListEntry<T> entry) {
        var downstreamIndexer = getDownstreamIndexerTree(compositeKey, indexKey, entry);
        downstreamIndexer.remove(compositeKey, entry);
        if (downstreamIndexer.isRemovable()) {
            comparisonMap.remove(indexKey);
        }
    }

    private Indexer<T> getDownstreamIndexerTree(Object compositeKey, Key_ indexKey, ListEntry<T> entry) {
        var downstreamIndexer = comparisonMap.get(indexKey);
        if (downstreamIndexer == null) {
            throw new IllegalStateException(
                    "Impossible state: the tuple (%s) with composite key (%s) doesn't exist in the indexer %s."
                            .formatted(entry, compositeKey, this));
        }
        return downstreamIndexer;
    }

    private void removeArray(Object compositeKey, Key_ indexKey, ListEntry<T> entry) {
        var searchResult = Arrays.binarySearch(keys, 0, size, indexKey);
        if (searchResult < 0) {
            throw new IllegalStateException(
                    "Impossible state: the tuple (%s) with composite key (%s) doesn't exist in the indexer %s."
                            .formatted(entry, compositeKey, this));
        }
        var downstreamIndexer = indexers[searchResult];
        downstreamIndexer.remove(compositeKey, entry);
        if (downstreamIndexer.isRemovable()) {
            removeFromArrays(searchResult);
        }
    }

    private void removeFromArrays(int index) {
        var shiftCount = size - index - 1;
        if (shiftCount > 0) {
            System.arraycopy(keys, index + 1, keys, index, shiftCount);
            System.arraycopy(indexers, index + 1, indexers, index, shiftCount);
        }
        size--;
    }

    @Override
    public int size(Object compositeKey) {
        return belowThreshold ? sizeArray(compositeKey) : sizeTree(compositeKey);
    }

    private int sizeTree(Object compositeKey) {
        return switch (comparisonMap.size()) {
            case 0 -> 0;
            case 1 -> sizeSingleIndexerTree(compositeKey);
            default -> sizeManyIndexersTree(compositeKey);
        };
    }

    private int sizeSingleIndexerTree(Object compositeKey) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var entry = comparisonMap.firstEntry();
        return boundaryReached(entry.getKey(), indexKey) ? 0 : entry.getValue().size(compositeKey);
    }

    private boolean boundaryReached(Key_ entryKey, Key_ indexKey) {
        var comparison = entryKey.compareTo(indexKey);
        if (reverseOrder) {
            // Comparator matches the order of iteration of the map, so the boundary is always found from the bottom up.
            comparison = -comparison;
        }
        if (comparison >= 0) { // Possibility of reaching the boundary condition.
            // Boundary condition reached when we're out of bounds entirely, or when GTE/LTE is not allowed.
            return comparison > 0 || !hasOrEquals;
        }
        return false;
    }

    private int sizeManyIndexersTree(Object compositeKey) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var size = 0;
        for (var entry : comparisonMap.entrySet()) {
            if (boundaryReached(entry.getKey(), indexKey)) {
                return size;
            }
            // Boundary condition not yet reached; include the indexer in the range.
            size += entry.getValue().size(compositeKey);
        }
        return size;
    }

    private int sizeArray(Object compositeKey) {
        if (size == 0) {
            return 0;
        }
        var indexKey = keyUnpacker.apply(compositeKey);
        var total = 0;
        var i = arrayStart();
        var step = arrayStep();
        while (i >= 0 && i < size) {
            if (boundaryReached(keys[i], indexKey)) {
                break;
            }
            total += indexers[i].size(compositeKey);
            i += step;
        }
        return total;
    }

    @Override
    public void forEach(Object compositeKey, Consumer<T> tupleConsumer) {
        if (belowThreshold) {
            forEachArray(compositeKey, tupleConsumer);
        } else {
            forEachTree(compositeKey, tupleConsumer);
        }
    }

    private void forEachTree(Object compositeKey, Consumer<T> tupleConsumer) {
        switch (comparisonMap.size()) {
            case 0 -> {
                /* Nothing to do. */
            }
            case 1 -> forEachSingleIndexerTree(compositeKey, tupleConsumer);
            default -> forEachManyIndexersTree(compositeKey, tupleConsumer);
        }
    }

    private void forEachSingleIndexerTree(Object compositeKey, Consumer<T> tupleConsumer) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var entry = comparisonMap.firstEntry();
        if (!boundaryReached(entry.getKey(), indexKey)) {
            // Boundary condition not yet reached; include the indexer in the range.
            entry.getValue().forEach(compositeKey, tupleConsumer);
        }
    }

    private void forEachManyIndexersTree(Object compositeKey, Consumer<T> tupleConsumer) {
        var indexKey = keyUnpacker.apply(compositeKey);
        for (var entry : comparisonMap.entrySet()) {
            if (boundaryReached(entry.getKey(), indexKey)) {
                return;
            }
            // Boundary condition not yet reached; include the indexer in the range.
            entry.getValue().forEach(compositeKey, tupleConsumer);
        }
    }

    private void forEachArray(Object compositeKey, Consumer<T> tupleConsumer) {
        if (size == 0) {
            return;
        }
        var indexKey = keyUnpacker.apply(compositeKey);
        var i = arrayStart();
        var step = arrayStep();
        while (i >= 0 && i < size) {
            if (boundaryReached(keys[i], indexKey)) {
                return;
            }
            // Boundary condition not yet reached; include the indexer in the range.
            indexers[i].forEach(compositeKey, tupleConsumer);
            i += step;
        }
    }

    @Override
    public Iterator<T> iterator(Object queryCompositeKey) {
        return belowThreshold ? iteratorArray(queryCompositeKey) : iteratorTree(queryCompositeKey);
    }

    private Iterator<T> iteratorTree(Object queryCompositeKey) {
        return switch (comparisonMap.size()) {
            case 0 -> Collections.emptyIterator();
            case 1 -> iteratorSingleIndexerTree(queryCompositeKey);
            default -> new DefaultIterator(queryCompositeKey);
        };
    }

    private Iterator<T> iteratorSingleIndexerTree(Object compositeKey) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var entry = comparisonMap.firstEntry();
        if (boundaryReached(entry.getKey(), indexKey)) {
            return Collections.emptyIterator();
        }
        // Boundary condition not yet reached; include the indexer in the range.
        return entry.getValue().iterator(compositeKey);
    }

    private Iterator<T> iteratorArray(Object queryCompositeKey) {
        return size == 0 ? Collections.emptyIterator() : new DefaultIterator(queryCompositeKey);
    }

    @Override
    public Iterator<T> randomIterator(Object queryCompositeKey, RandomGenerator workingRandom) {
        return createRandomIterator(queryCompositeKey, workingRandom, null);
    }

    private Iterator<T> createRandomIterator(Object queryCompositeKey, RandomGenerator workingRandom,
            @Nullable Predicate<T> filter) {
        return belowThreshold ? createRandomIteratorArray(queryCompositeKey, workingRandom, filter)
                : createRandomIteratorTree(queryCompositeKey, workingRandom, filter);
    }

    private Iterator<T> createRandomIteratorTree(Object queryCompositeKey, RandomGenerator workingRandom,
            @Nullable Predicate<T> filter) {
        return switch (comparisonMap.size()) {
            case 0 -> Collections.emptyIterator();
            case 1 -> {
                var indexKey = keyUnpacker.apply(queryCompositeKey);
                var entry = comparisonMap.firstEntry();
                if (boundaryReached(entry.getKey(), indexKey)) {
                    yield Collections.emptyIterator();
                } else { // Boundary condition not yet reached; include the indexer in the range.
                    if (filter == null) {
                        yield entry.getValue().randomIterator(queryCompositeKey, workingRandom);
                    } else {
                        yield entry.getValue().randomIterator(queryCompositeKey, workingRandom, filter);
                    }
                }
            }
            default -> {
                if (filter == null) {
                    yield new RandomIterator(queryCompositeKey,
                            indexer -> indexer.randomIterator(queryCompositeKey, workingRandom));
                } else {
                    yield new RandomIterator(queryCompositeKey,
                            indexer -> indexer.randomIterator(queryCompositeKey, workingRandom, filter));
                }
            }
        };
    }

    private Iterator<T> createRandomIteratorArray(Object queryCompositeKey, RandomGenerator workingRandom,
            @Nullable Predicate<T> filter) {
        if (size == 0) {
            return Collections.emptyIterator();
        }
        if (filter == null) {
            return new RandomIterator(queryCompositeKey, indexer -> indexer.randomIterator(queryCompositeKey, workingRandom));
        } else {
            return new RandomIterator(queryCompositeKey,
                    indexer -> indexer.randomIterator(queryCompositeKey, workingRandom, filter));
        }
    }

    @Override
    public Iterator<T> randomIterator(Object queryCompositeKey, RandomGenerator workingRandom, Predicate<T> filter) {
        return createRandomIterator(queryCompositeKey, workingRandom, filter);
    }

    @Override
    public boolean isRemovable() {
        return belowThreshold ? size == 0 : comparisonMap.isEmpty();
    }

    @Override
    public String toString() {
        return "size = " + (belowThreshold ? size : comparisonMap.size());
    }

    /**
     * Below threshold, keys are always stored ascending regardless of {@link #reverseOrder}; only the scan
     * direction changes, so that array mode reproduces the same iteration order the tree mode's
     * reverse-ordered {@link TreeMap} produces today.
     */
    private int arrayStart() {
        return reverseOrder ? size - 1 : 0;
    }

    private int arrayStep() {
        return reverseOrder ? -1 : 1;
    }

    private class DefaultIterator implements Iterator<T> {

        private final Key_ indexKey;
        private final Function<Indexer<T>, Iterator<T>> downstreamIteratorFunction;
        // Tree mode: entries pulled from here. Array mode: this is null and the cursor fields are used instead.
        private final @Nullable Iterator<Map.Entry<Key_, Indexer<T>>> indexerIterator;
        private int arrayCursor;
        private final int arrayStep;
        protected @Nullable Iterator<T> downstreamIterator = null;
        private @Nullable T next = null;

        public DefaultIterator(Object queryCompositeKey) {
            this(queryCompositeKey,
                    downstreamIndexer -> downstreamIndexer.iterator(queryCompositeKey));
        }

        protected DefaultIterator(Object queryCompositeKey, Function<Indexer<T>, Iterator<T>> downstreamIteratorFunction) {
            this.indexKey = keyUnpacker.apply(queryCompositeKey);
            this.downstreamIteratorFunction = downstreamIteratorFunction;
            if (belowThreshold) {
                this.indexerIterator = null;
                this.arrayCursor = arrayStart();
                this.arrayStep = arrayStep();
            } else {
                this.indexerIterator = comparisonMap.entrySet().iterator();
                this.arrayCursor = 0;
                this.arrayStep = 0;
            }
        }

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            if (downstreamIterator != null && downstreamIterator.hasNext()) {
                next = downstreamIterator.next();
                return true;
            }
            if (indexerIterator != null) {
                while (indexerIterator.hasNext()) {
                    var entry = indexerIterator.next();
                    if (boundaryReached(entry.getKey(), indexKey)) {
                        return false;
                    }
                    // Boundary condition not yet reached; include the indexer in the range.
                    downstreamIterator = downstreamIteratorFunction.apply(entry.getValue());
                    if (downstreamIterator.hasNext()) {
                        next = downstreamIterator.next();
                        return true;
                    }
                }
                return false;
            }
            while (arrayCursor >= 0 && arrayCursor < size) {
                var key = keys[arrayCursor];
                var indexer = indexers[arrayCursor];
                arrayCursor += arrayStep;
                if (boundaryReached(key, indexKey)) {
                    return false;
                }
                // Boundary condition not yet reached; include the indexer in the range.
                downstreamIterator = downstreamIteratorFunction.apply(indexer);
                if (downstreamIterator.hasNext()) {
                    next = downstreamIterator.next();
                    return true;
                }
            }
            return false;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var result = next;
            next = null;
            return result;
        }
    }

    private final class RandomIterator extends DefaultIterator {

        public RandomIterator(Object queryCompositeKey, Function<Indexer<T>, Iterator<T>> downstreamIteratorFunction) {
            super(queryCompositeKey, downstreamIteratorFunction);
        }

        @Override
        public void remove() {
            if (downstreamIterator == null) {
                throw new IllegalStateException("next() must be called before remove().");
            }
            downstreamIterator.remove();
        }

    }

}
