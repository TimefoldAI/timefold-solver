package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
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
 * An {@link Indexer} for LT/LTE/GT/GTE joins, keyed by a single {@link Comparable} value
 * and backed by a {@link ScalingNavigableMap} of key to downstream {@link Indexer}.
 * <p>
 * GT/GTE are handled by iterating the map in reverse ({@link #reverseOrder})
 * rather than by using a reversed comparator or sub-map views,
 * so both directions can share the same ascending, comparator-free {@link ScalingNavigableMap}
 * and its fast, non-comparator {@code TreeMap} lookup path.
 * {@link #boundaryReached} folds that direction, plus LTE/GTE inclusivity ({@link #hasOrEquals}),
 * into a single check used to stop a range scan.
 * <p>
 * Every query operation ({@link #size}, {@link #forEach}, {@link #iterator}, {@link #randomIterator})
 * has separate array-mode and tree-mode implementations,
 * dispatched on {@link ScalingNavigableMap#arrayBased}.
 * <p>
 * This class was heavily benchmarked;
 * it is recommended to assume that most decisions made here
 * are performance-driven and not necessarily obvious or intuitive.
 * Any changes should also be based on benchmarks.
 *
 * @param <T> the element type, see {@link Indexer}
 * @param <Key_> the type of the comparison key, unpacked from the composite key by {@link #keyUnpacker}
 */
@NullMarked
final class ComparisonIndexer<T, Key_ extends Comparable<Key_>> implements Indexer<T> {

    private final KeyUnpacker<Key_> keyUnpacker;
    private final Supplier<Indexer<T>> downstreamIndexerSupplier;
    private final boolean reverseOrder;
    private final boolean hasOrEquals;
    private final ScalingNavigableMap<Key_, Indexer<T>> comparisonMap;

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
        this.comparisonMap = new ScalingNavigableMap<>();
    }

    @Override
    public ListEntry<T> put(Object compositeKey, T tuple) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var downstreamIndexer = comparisonMap.getOrCreate(indexKey, downstreamIndexerSupplier);
        return downstreamIndexer.put(compositeKey, tuple);
    }

    @Override
    public void remove(Object compositeKey, ListEntry<T> entry) {
        var indexKey = keyUnpacker.apply(compositeKey);
        if (comparisonMap.arrayBased) {
            removeArray(compositeKey, indexKey, entry);
        } else {
            removeTree(compositeKey, indexKey, entry);
        }
    }

    /**
     * Looks up the entry once (comparisonMap.indexOf(), one binary search)
     * and reuses the same index to remove it if needed,
     * instead of a second indexOf()-equivalent lookup for the same key
     * (as comparisonMap.remove(key) alone would do).
     */
    private void removeArray(Object compositeKey, Key_ indexKey, ListEntry<T> entry) {
        var index = comparisonMap.indexOf(indexKey);
        if (index < 0) {
            throw notFoundError(compositeKey, entry);
        }
        var downstreamIndexer = comparisonMap.valueAt(index);
        downstreamIndexer.remove(compositeKey, entry);
        if (downstreamIndexer.isRemovable()) {
            comparisonMap.removeAt(index);
        }
    }

    private void removeTree(Object compositeKey, Key_ indexKey, ListEntry<T> entry) {
        var downstreamIndexer = comparisonMap.get(indexKey);
        if (downstreamIndexer == null) {
            throw notFoundError(compositeKey, entry);
        }
        downstreamIndexer.remove(compositeKey, entry);
        if (downstreamIndexer.isRemovable()) {
            comparisonMap.remove(indexKey);
        }
    }

    private IllegalStateException notFoundError(Object compositeKey, ListEntry<T> entry) {
        return new IllegalStateException(
                "Impossible state: the tuple (%s) with composite key (%s) doesn't exist in the indexer %s.".formatted(entry,
                        compositeKey, this));
    }

    @Override
    public int size(Object compositeKey) {
        return switch (comparisonMap.size()) {
            case 0 -> 0;
            case 1 -> sizeSingleIndexer(compositeKey);
            default -> sizeManyIndexers(compositeKey);
        };
    }

    private int sizeSingleIndexer(Object compositeKey) {
        return comparisonMap.arrayBased ? sizeSingleIndexerArray(compositeKey) : sizeSingleIndexerTree(compositeKey);
    }

    private int sizeSingleIndexerArray(Object compositeKey) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var entryKey = comparisonMap.keyAt(0);
        var entryValue = comparisonMap.valueAt(0);
        return boundaryReached(entryKey, indexKey) ? 0 : entryValue.size(compositeKey);
    }

    private int sizeSingleIndexerTree(Object compositeKey) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var entry = comparisonMap.firstEntry();
        return boundaryReached(entry.getKey(), indexKey) ? 0 : entry.getValue().size(compositeKey);
    }

    /**
     * Whether {@code entryKey}, and every key past it in iteration order, fails to match {@code indexKey}
     * and the range scan (in {@code size}/{@code forEach}/{@code iterator}/{@code randomIterator}) should stop.
     * <p>
     * {@code comparisonMap} is always iterated ascending by natural order (see {@link ScalingNavigableMap});
     * {@link #reverseOrder} instead flips the sign of the comparison here,
     * so GT/GTE effectively scan the same ascending storage from the other end,
     * without needing a reversed comparator or sub-map view.
     */
    private boolean boundaryReached(Key_ entryKey, Key_ indexKey) {
        var comparison = entryKey.compareTo(indexKey);
        if (reverseOrder) {
            // Comparator matches the order of iteration of the map, so the boundary is always found from the bottom up.
            comparison = -comparison;
        }
        if (comparison >= 0) {
            // Boundary condition reached when we're out of bounds entirely, or when GTE/LTE is not allowed.
            return comparison > 0 || !hasOrEquals;
        }
        return false;
    }

    private int sizeManyIndexers(Object compositeKey) {
        return comparisonMap.arrayBased ? sizeManyIndexersArray(compositeKey) : sizeManyIndexersTree(compositeKey);
    }

    private int sizeManyIndexersArray(Object compositeKey) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var size = 0;
        var arraySize = comparisonMap.size();
        var i = reverseOrder ? arraySize - 1 : 0;
        var step = reverseOrder ? -1 : 1;
        while (i >= 0 && i < arraySize) {
            if (boundaryReached(comparisonMap.keyAt(i), indexKey)) {
                return size;
            }
            // Boundary condition not yet reached; include the indexer in the range.
            size += comparisonMap.valueAt(i).size(compositeKey);
            i += step;
        }
        return size;
    }

    private int sizeManyIndexersTree(Object compositeKey) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var size = 0;
        var entryIterator = comparisonMap.iterator(reverseOrder);
        while (entryIterator.hasNext()) {
            var entry = entryIterator.next();
            if (boundaryReached(entry.getKey(), indexKey)) {
                return size;
            }
            // Boundary condition not yet reached; include the indexer in the range.
            size += entry.getValue().size(compositeKey);
        }
        return size;
    }

    @Override
    public void forEach(Object compositeKey, Consumer<T> tupleConsumer) {
        switch (comparisonMap.size()) {
            case 0 -> {
                // Nothing to do.
            }
            case 1 -> forEachSingleIndexer(compositeKey, tupleConsumer);
            default -> forEachManyIndexers(compositeKey, tupleConsumer);
        }
    }

    private void forEachSingleIndexer(Object compositeKey, Consumer<T> tupleConsumer) {
        if (comparisonMap.arrayBased) {
            forEachSingleIndexerArray(compositeKey, tupleConsumer);
        } else {
            forEachSingleIndexerTree(compositeKey, tupleConsumer);
        }
    }

    private void forEachSingleIndexerArray(Object compositeKey, Consumer<T> tupleConsumer) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var entryKey = comparisonMap.keyAt(0);
        var entryValue = comparisonMap.valueAt(0);
        if (!boundaryReached(entryKey, indexKey)) {
            // Boundary condition not yet reached; include the indexer in the range.
            entryValue.forEach(compositeKey, tupleConsumer);
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

    private void forEachManyIndexers(Object compositeKey, Consumer<T> tupleConsumer) {
        if (comparisonMap.arrayBased) {
            forEachManyIndexersArray(compositeKey, tupleConsumer);
        } else {
            forEachManyIndexersTree(compositeKey, tupleConsumer);
        }
    }

    private void forEachManyIndexersArray(Object compositeKey, Consumer<T> tupleConsumer) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var arraySize = comparisonMap.size();
        var i = reverseOrder ? arraySize - 1 : 0;
        var step = reverseOrder ? -1 : 1;
        while (i >= 0 && i < arraySize) {
            if (boundaryReached(comparisonMap.keyAt(i), indexKey)) {
                return;
            }
            // Boundary condition not yet reached; include the indexer in the range.
            comparisonMap.valueAt(i).forEach(compositeKey, tupleConsumer);
            i += step;
        }
    }

    private void forEachManyIndexersTree(Object compositeKey, Consumer<T> tupleConsumer) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var entryIterator = comparisonMap.iterator(reverseOrder);
        while (entryIterator.hasNext()) {
            var entry = entryIterator.next();
            if (boundaryReached(entry.getKey(), indexKey)) {
                return;
            }
            // Boundary condition not yet reached; include the indexer in the range.
            entry.getValue().forEach(compositeKey, tupleConsumer);
        }
    }

    @Override
    public Iterator<T> iterator(Object queryCompositeKey) {
        return switch (comparisonMap.size()) {
            case 0 -> Collections.emptyIterator();
            case 1 -> iteratorSingleIndexer(queryCompositeKey);
            default -> new DefaultIterator(queryCompositeKey);
        };
    }

    private Iterator<T> iteratorSingleIndexer(Object compositeKey) {
        return comparisonMap.arrayBased ? iteratorSingleIndexerArray(compositeKey) : iteratorSingleIndexerTree(compositeKey);
    }

    private Iterator<T> iteratorSingleIndexerArray(Object compositeKey) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var entryKey = comparisonMap.keyAt(0);
        var entryValue = comparisonMap.valueAt(0);
        if (boundaryReached(entryKey, indexKey)) {
            return Collections.emptyIterator();
        }
        // Boundary condition not yet reached; include the indexer in the range.
        return entryValue.iterator(compositeKey);
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

    @Override
    public Iterator<T> randomIterator(Object queryCompositeKey, RandomGenerator workingRandom) {
        return createRandomIterator(queryCompositeKey, workingRandom, null);
    }

    private Iterator<T> createRandomIterator(Object queryCompositeKey, RandomGenerator workingRandom,
            @Nullable Predicate<T> filter) {
        return switch (comparisonMap.size()) {
            case 0 -> Collections.emptyIterator();
            case 1 ->
                comparisonMap.arrayBased ? randomIteratorSingleIndexerArray(queryCompositeKey, workingRandom, filter)
                        : randomIteratorSingleIndexerTree(queryCompositeKey, workingRandom, filter);
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

    private Iterator<T> randomIteratorSingleIndexerArray(Object queryCompositeKey, RandomGenerator workingRandom,
            @Nullable Predicate<T> filter) {
        var indexKey = keyUnpacker.apply(queryCompositeKey);
        var entryKey = comparisonMap.keyAt(0);
        var entryValue = comparisonMap.valueAt(0);
        if (boundaryReached(entryKey, indexKey)) {
            return Collections.emptyIterator();
        }
        // Boundary condition not yet reached; include the indexer in the range.
        return filter == null ? entryValue.randomIterator(queryCompositeKey, workingRandom)
                : entryValue.randomIterator(queryCompositeKey, workingRandom, filter);
    }

    private Iterator<T> randomIteratorSingleIndexerTree(Object queryCompositeKey, RandomGenerator workingRandom,
            @Nullable Predicate<T> filter) {
        var indexKey = keyUnpacker.apply(queryCompositeKey);
        var entry = comparisonMap.firstEntry();
        if (boundaryReached(entry.getKey(), indexKey)) {
            return Collections.emptyIterator();
        }
        // Boundary condition not yet reached; include the indexer in the range.
        return filter == null ? entry.getValue().randomIterator(queryCompositeKey, workingRandom)
                : entry.getValue().randomIterator(queryCompositeKey, workingRandom, filter);
    }

    @Override
    public Iterator<T> randomIterator(Object queryCompositeKey, RandomGenerator workingRandom, Predicate<T> filter) {
        return createRandomIterator(queryCompositeKey, workingRandom, filter);
    }

    @Override
    public boolean isRemovable() {
        return comparisonMap.isEmpty();
    }

    @Override
    public String toString() {
        return "size = " + comparisonMap.size();
    }

    /**
     * Handles both array-mode and tree-mode iteration in a single class,
     * branching internally ({@link #advanceFromArray}/{@link #advanceFromTree})
     * rather than splitting into two {@code Iterator} implementations behind a shared type.
     */
    private class DefaultIterator implements Iterator<T> {

        private final Key_ indexKey;
        private final Function<Indexer<T>, Iterator<T>> downstreamIteratorFunction;
        // Tree mode: entries pulled from here. Array mode: this is null and the array cursor fields are used instead.
        private final @Nullable Iterator<Map.Entry<Key_, Indexer<T>>> indexerIterator;
        private int arrayCursor;
        private final int arrayStep;
        protected @Nullable Iterator<T> downstreamIterator = null;
        private @Nullable T next = null;

        public DefaultIterator(Object queryCompositeKey) {
            this(queryCompositeKey, downstreamIndexer -> downstreamIndexer.iterator(queryCompositeKey));
        }

        protected DefaultIterator(Object queryCompositeKey, Function<Indexer<T>, Iterator<T>> downstreamIteratorFunction) {
            this.indexKey = keyUnpacker.apply(queryCompositeKey);
            this.downstreamIteratorFunction = downstreamIteratorFunction;
            if (comparisonMap.arrayBased) {
                this.indexerIterator = null;
                this.arrayCursor = reverseOrder ? comparisonMap.size() - 1 : 0;
                this.arrayStep = reverseOrder ? -1 : 1;
            } else {
                this.indexerIterator = comparisonMap.iterator(reverseOrder);
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
            return indexerIterator != null ? advanceFromTree() : advanceFromArray();
        }

        private boolean advanceFromTree() {
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

        private boolean advanceFromArray() {
            var size = comparisonMap.size();
            while (arrayCursor >= 0 && arrayCursor < size) {
                var key = comparisonMap.keyAt(arrayCursor);
                var indexer = comparisonMap.valueAt(arrayCursor);
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
