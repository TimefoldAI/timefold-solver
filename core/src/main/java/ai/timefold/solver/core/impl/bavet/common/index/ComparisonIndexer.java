package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
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
     * @param keyUnpacker determines if it immediately goes to a {@link LeafIndexer} or if it uses a {@link CompositeKey}.
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
            default -> new DefaultIterator(queryCompositeKey);
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
    public Iterator<T> randomIterator(Object queryCompositeKey, RandomGenerator workingRandom) {
        return createRandomIterator(queryCompositeKey, workingRandom, null);
    }

    private Iterator<T> createRandomIterator(Object queryCompositeKey, RandomGenerator workingRandom,
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
                Function<Indexer<T>, Iterator<T>> downstreamIteratorFunction = filter == null
                        ? indexer -> indexer.randomIterator(queryCompositeKey, workingRandom)
                        : indexer -> indexer.randomIterator(queryCompositeKey, workingRandom, filter);
                yield new RandomIterator(queryCompositeKey, workingRandom, downstreamIteratorFunction);
            }
        };
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

    private class DefaultIterator implements Iterator<T> {

        private final Key_ indexKey;
        private final Function<Indexer<T>, Iterator<T>> downstreamIteratorFunction;
        private final Iterator<Map.Entry<Key_, Indexer<T>>> indexerIterator = comparisonMap.entrySet().iterator();
        protected @Nullable Iterator<T> downstreamIterator = null;
        private @Nullable T next = null;

        public DefaultIterator(Object queryCompositeKey) {
            this(queryCompositeKey,
                    downstreamIndexer -> downstreamIndexer.iterator(queryCompositeKey));
        }

        protected DefaultIterator(Object queryCompositeKey, Function<Indexer<T>, Iterator<T>> downstreamIteratorFunction) {
            this.indexKey = keyUnpacker.apply(queryCompositeKey);
            this.downstreamIteratorFunction = downstreamIteratorFunction;
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
                downstreamIterator = downstreamIteratorFunction.apply(entry.getValue());
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

    /**
     * Iterates the in-range leaf indexers so that the selection is fair across all elements:
     * each leaf indexer is drawn with a probability proportional to the number of elements it
     * contributes to the query range, so every element has the same chance of being visited next.
     * Without this weighting, an element in a small leaf indexer would be over-represented
     * relative to an element in a large one.
     */
    private final class RandomIterator implements Iterator<T> {

        private final RandomGenerator workingRandom;
        private final List<Bucket> buckets = new ArrayList<>();
        private int remainingTotal = 0;

        private boolean hasNextComputed = false;
        private @Nullable T next = null;
        private @Nullable Bucket nextBucket = null;
        private @Nullable Bucket lastReturnedBucket = null;

        private RandomIterator(Object queryCompositeKey, RandomGenerator workingRandom,
                Function<Indexer<T>, Iterator<T>> downstreamIteratorFunction) {
            this.workingRandom = workingRandom;
            var indexKey = keyUnpacker.apply(queryCompositeKey);
            for (var entry : comparisonMap.entrySet()) {
                if (boundaryReached(entry.getKey(), indexKey)) {
                    // Boundary reached; the remaining leaf indexers are out of range.
                    break;
                }
                var downstreamIndexer = entry.getValue();
                var size = downstreamIndexer.size(queryCompositeKey);
                if (size <= 0) {
                    continue;
                }
                buckets.add(new Bucket(downstreamIteratorFunction.apply(downstreamIndexer), size));
                remainingTotal += size;
            }
        }

        @Override
        public boolean hasNext() {
            if (hasNextComputed) {
                return true;
            }
            while (remainingTotal > 0) {
                var bucket = pickBucket();
                if (bucket.iterator.hasNext()) {
                    next = bucket.iterator.next();
                    nextBucket = bucket;
                    hasNextComputed = true;
                    return true;
                }
                // The leaf indexer has no more matching elements (e.g. all filtered out); drop it from the draw.
                remainingTotal -= bucket.remaining;
                bucket.remaining = 0;
            }
            next = null;
            nextBucket = null;
            return false;
        }

        private Bucket pickBucket() {
            var threshold = workingRandom.nextInt(remainingTotal);
            var cumulative = 0;
            for (var bucket : buckets) {
                cumulative += bucket.remaining;
                if (threshold < cumulative) {
                    return bucket;
                }
            }
            throw new IllegalStateException(
                    "Impossible state: no leaf indexer selected for threshold (%d).".formatted(threshold));
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var result = next;
            lastReturnedBucket = nextBucket;
            hasNextComputed = false;
            next = null;
            nextBucket = null;
            return result;
        }

        @Override
        public void remove() {
            if (lastReturnedBucket == null) {
                throw new IllegalStateException("next() must be called before remove().");
            }
            lastReturnedBucket.iterator.remove();
            lastReturnedBucket.remaining--;
            remainingTotal--;
            lastReturnedBucket = null;
        }

    }

    private final class Bucket {

        private final Iterator<T> iterator;
        private int remaining;

        private Bucket(Iterator<T> iterator, int remaining) {
            this.iterator = iterator;
            this.remaining = remaining;
        }

    }

}
