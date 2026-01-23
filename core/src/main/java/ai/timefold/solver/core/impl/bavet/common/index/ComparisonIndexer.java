package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;
import ai.timefold.solver.core.impl.util.ListEntry;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class ComparisonIndexer<T, Key_ extends Comparable<Key_>>
        implements Indexer<T> {

    private final KeyUnpacker<Key_> keyUnpacker;
    private final Supplier<Indexer<T>> downstreamIndexerSupplier;
    private final boolean reverseOrder;
    private final boolean hasOrEquals;
    private final NavigableMap<Key_, Indexer<T>> comparisonMap;

    /**
     * @param comparisonJoinerType the type of comparison to use
     * @param keyUnpacker determines if it immediately goes to a {@link IndexerBackend} or if it uses a {@link CompositeKey}.
     * @param downstreamIndexerSupplier the supplier of the downstream indexer
     */
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
        this.comparisonMap = reverseOrder ? new TreeMap<>(Comparator.reverseOrder()) : new TreeMap<>();
    }

    @Override
    public ListEntry<T> put(Object compositeKey, T tuple) {
        var indexKey = keyUnpacker.apply(compositeKey);
        // Avoids computeIfAbsent in order to not create lambdas on the hot path.
        var downstreamIndexer = comparisonMap.get(indexKey);
        if (downstreamIndexer == null) {
            downstreamIndexer = downstreamIndexerSupplier.get();
            comparisonMap.put(indexKey, downstreamIndexer);
        }
        return downstreamIndexer.put(compositeKey, tuple);
    }

    @Override
    public void remove(Object compositeKey, ListEntry<T> entry) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var downstreamIndexer = getDownstreamIndexer(compositeKey, indexKey, entry);
        downstreamIndexer.remove(compositeKey, entry);
        if (downstreamIndexer.isRemovable()) {
            comparisonMap.remove(indexKey);
        }
    }

    private Indexer<T> getDownstreamIndexer(Object compositeKey, Key_ indexerKey, ListEntry<T> entry) {
        var downstreamIndexer = comparisonMap.get(indexerKey);
        if (downstreamIndexer == null) {
            throw new IllegalStateException(
                    "Impossible state: the tuple (%s) with composite key (%s) doesn't exist in the indexer %s."
                            .formatted(entry, compositeKey, this));
        }
        return downstreamIndexer;
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

    private int sizeManyIndexers(Object compositeKey) {
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

    @Override
    public void forEach(Object compositeKey, Consumer<T> tupleConsumer) {
        switch (comparisonMap.size()) {
            case 0 -> {
                /* Nothing to do. */
            }
            case 1 -> forEachSingleIndexer(compositeKey, tupleConsumer);
            default -> forEachManyIndexers(compositeKey, tupleConsumer);
        }
    }

    private void forEachSingleIndexer(Object compositeKey, Consumer<T> tupleConsumer) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var entry = comparisonMap.firstEntry();
        if (!boundaryReached(entry.getKey(), indexKey)) {
            // Boundary condition not yet reached; include the indexer in the range.
            entry.getValue().forEach(compositeKey, tupleConsumer);
        }
    }

    private void forEachManyIndexers(Object compositeKey, Consumer<T> tupleConsumer) {
        var indexKey = keyUnpacker.apply(compositeKey);
        for (var entry : comparisonMap.entrySet()) {
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
            default -> new DefaultIterator(queryCompositeKey, indexer -> indexer.iterator(queryCompositeKey));
        };
    }

    private Iterator<T> iteratorSingleIndexer(Object compositeKey) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var entry = comparisonMap.firstEntry();
        if (boundaryReached(entry.getKey(), indexKey)) {
            return Collections.emptyIterator();
        }
        // Boundary condition not yet reached; include the indexer in the range.
        return entry.getValue().iterator(compositeKey);
    }

    @Override
    public Iterator<T> randomIterator(Object queryCompositeKey, Random workingRandom) {
        return switch (comparisonMap.size()) {
            case 0 -> Collections.emptyIterator();
            case 1 -> randomIteratorSingleIndexer(queryCompositeKey, workingRandom);
            default ->
                new DefaultIterator(queryCompositeKey, indexer -> indexer.randomIterator(queryCompositeKey, workingRandom));
        };
    }

    private Iterator<T> randomIteratorSingleIndexer(Object compositeKey, Random workingRandom) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var entry = comparisonMap.firstEntry();
        if (boundaryReached(entry.getKey(), indexKey)) {
            return Collections.emptyIterator();
        }
        // Boundary condition not yet reached; include the indexer in the range.
        return entry.getValue().randomIterator(compositeKey, workingRandom);
    }

    @Override
    public Iterator<T> randomIterator(Object queryCompositeKey, Random workingRandom, Predicate<T> filter) {
        return switch (comparisonMap.size()) {
            case 0 -> Collections.emptyIterator();
            case 1 -> randomIteratorSingleIndexer(queryCompositeKey, workingRandom, filter);
            default -> new DefaultIterator(queryCompositeKey,
                    indexer -> indexer.randomIterator(queryCompositeKey, workingRandom, filter));
        };
    }

    private Iterator<T> randomIteratorSingleIndexer(Object compositeKey, Random workingRandom, Predicate<T> filter) {
        var indexKey = keyUnpacker.apply(compositeKey);
        var entry = comparisonMap.firstEntry();
        if (boundaryReached(entry.getKey(), indexKey)) {
            return Collections.emptyIterator();
        }
        // Boundary condition not yet reached; include the indexer in the range.
        return entry.getValue().randomIterator(compositeKey, workingRandom, filter);
    }

    @Override
    public boolean isRemovable() {
        return comparisonMap.isEmpty();
    }

    @Override
    public String toString() {
        return "size = " + comparisonMap.size();
    }

    private final class DefaultIterator implements Iterator<T> {

        private final Key_ indexKey;
        private final Function<Indexer<T>, Iterator<T>> iteratorFunction;
        private final Iterator<Map.Entry<Key_, Indexer<T>>> indexerIterator = comparisonMap.entrySet().iterator();
        private @Nullable Iterator<T> downstreamIterator = null;
        private @Nullable T next = null;

        public DefaultIterator(Object compositeKey, Function<Indexer<T>, Iterator<T>> iteratorFunction) {
            this.indexKey = keyUnpacker.apply(compositeKey);
            this.iteratorFunction = iteratorFunction;
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
            while (indexerIterator.hasNext()) {
                var entry = indexerIterator.next();
                if (boundaryReached(entry.getKey(), indexKey)) {
                    return false;
                }
                // Boundary condition not yet reached; include the indexer in the range.
                downstreamIterator = iteratorFunction.apply(entry.getValue());
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

}
