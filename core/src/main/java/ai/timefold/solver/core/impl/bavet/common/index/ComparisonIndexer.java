package ai.timefold.solver.core.impl.bavet.common.index;

import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;
import ai.timefold.solver.core.impl.util.ListEntry;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

@NullMarked
final class ComparisonIndexer<T, Key_ extends Comparable<Key_>>
        implements Indexer<T> {

    private final KeyRetriever<Key_> keyRetriever;
    private final Supplier<Indexer<T>> downstreamIndexerSupplier;
    private final Comparator<Key_> keyComparator;
    private final boolean hasOrEquals;
    private final NavigableMap<Key_, Indexer<T>> comparisonMap;

    /**
     * Construct an {@link ComparisonIndexer} which immediately ends in a {@link IndexerBackend}.
     * This means {@code compositeKey} must be a single key.
     *
     * @param comparisonJoinerType the type of comparison to use
     */
    public ComparisonIndexer(JoinerType comparisonJoinerType) {
        this(comparisonJoinerType, LinkedListIndexerBackend::new);
    }

    /**
     * Construct an {@link ComparisonIndexer} which immediately ends in a {@link IndexerBackend}.
     * This means {@code compositeKey} must be a single key.
     *
     * @param comparisonJoinerType the type of comparison to use
     */
    public ComparisonIndexer(JoinerType comparisonJoinerType, Supplier<Indexer<T>> downstreamIndexerSupplier) {
        this(comparisonJoinerType, new SingleKeyRetriever<>(), downstreamIndexerSupplier);
    }

    /**
     * Construct an {@link ComparisonIndexer} which does not immediately go to a {@link IndexerBackend}.
     * This means {@code compositeKey} must be an instance of {@link CompositeKey}.
     *
     * @param comparisonJoinerType the type of comparison to use
     * @param keyIndex the index of the key to use within {@link CompositeKey}.
     * @param downstreamIndexerSupplier the supplier of the downstream indexer
     */
    public ComparisonIndexer(JoinerType comparisonJoinerType, int keyIndex, Supplier<Indexer<T>> downstreamIndexerSupplier) {
        this(comparisonJoinerType, new CompositeKeyRetriever<>(keyIndex), downstreamIndexerSupplier);
    }

    private ComparisonIndexer(JoinerType comparisonJoinerType, KeyRetriever<Key_> keyRetriever,
            Supplier<Indexer<T>> downstreamIndexerSupplier) {
        this.keyRetriever = Objects.requireNonNull(keyRetriever);
        this.downstreamIndexerSupplier = Objects.requireNonNull(downstreamIndexerSupplier);
        /*
         * For GT/GTE, the iteration order is reversed.
         * This allows us to iterate over the entire map, stopping when the threshold is reached.
         * This is done so that we can avoid using head/tail sub maps, which are expensive.
         */
        this.keyComparator =
                (comparisonJoinerType == JoinerType.GREATER_THAN || comparisonJoinerType == JoinerType.GREATER_THAN_OR_EQUAL)
                        ? Comparator.<Key_> naturalOrder().reversed()
                        : Comparator.naturalOrder();
        this.hasOrEquals = comparisonJoinerType == JoinerType.GREATER_THAN_OR_EQUAL
                || comparisonJoinerType == JoinerType.LESS_THAN_OR_EQUAL;
        this.comparisonMap = new TreeMap<>(keyComparator);
    }

    @Override
    public ListEntry<T> put(Object compositeKey, T tuple) {
        Key_ indexKey = keyRetriever.apply(compositeKey);
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
        Key_ indexKey = keyRetriever.apply(compositeKey);
        var downstreamIndexer = getDownstreamIndexer(compositeKey, indexKey, entry);
        downstreamIndexer.remove(compositeKey, entry);
        if (downstreamIndexer.isEmpty()) {
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

    // TODO clean up DRY
    @Override
    public int size(Object compositeKey) {
        var mapSize = comparisonMap.size();
        if (mapSize == 0) {
            return 0;
        }
        Key_ indexKey = keyRetriever.apply(compositeKey);
        if (mapSize == 1) { // Avoid creation of the entry set and iterator.
            var entry = comparisonMap.firstEntry();
            if (boundaryReached(entry.getKey(), indexKey)) {
                return 0;
            }
            return entry.getValue().size(compositeKey);
        } else {
            var size = 0;
            for (var entry : comparisonMap.entrySet()) {
                if (boundaryReached(entry.getKey(), indexKey)) {
                    break;
                }
                // Boundary condition not yet reached; include the indexer in the range.
                size += entry.getValue().size(compositeKey);
            }
            return size;
        }
    }

    private boolean boundaryReached(Key_ entryKey, Key_ indexKey) {
        // Comparator matches the order of iteration of the map, so the boundary is always found from the bottom up.
        var comparison = keyComparator.compare(entryKey, indexKey);
        if (comparison >= 0) { // Possibility of reaching the boundary condition.
            // Boundary condition reached when we're out of bounds entirely, or when GTE/LTE is not allowed.
            return comparison > 0 || !hasOrEquals;
        }
        return false;
    }

    @Override
    public void forEach(Object compositeKey, Consumer<T> tupleConsumer) {
        var size = comparisonMap.size();
        if (size == 0) {
            return;
        }
        Key_ indexKey = keyRetriever.apply(compositeKey);
        if (size == 1) { // Avoid creation of the entry set and iterator.
            var entry = comparisonMap.firstEntry();
            visitEntry(compositeKey, tupleConsumer, indexKey, entry);
        } else {
            for (var entry : comparisonMap.entrySet()) {
                var boundaryReached = visitEntry(compositeKey, tupleConsumer, indexKey, entry);
                if (boundaryReached) {
                    return;
                }
            }
        }
    }

    private boolean visitEntry(Object compositeKey, Consumer<T> tupleConsumer, Key_ indexKey,
            Map.Entry<Key_, Indexer<T>> entry) {
        if (boundaryReached(entry.getKey(), indexKey)) {
            return true;
        }
        // Boundary condition not yet reached; include the indexer in the range.
        entry.getValue().forEach(compositeKey, tupleConsumer);
        return false;
    }

    @Override
    public boolean isEmpty() {
        return comparisonMap.isEmpty();
    }

    @Override
    public List<? extends ListEntry<T>> asList(Object compositeKey) {
        var size = comparisonMap.size();
        if (size == 0) {
            return Collections.emptyList();
        }
        var result = new ArrayList<ListEntry<T>>();
        Key_ indexKey = keyRetriever.apply(compositeKey);
        if (size == 1) { // Avoid creation of the entry set and iterator.
            var entry = comparisonMap.firstEntry();
            visitEntry(compositeKey, result, indexKey, entry);
        } else {
            for (var entry : comparisonMap.entrySet()) {
                var boundaryReached = visitEntry(compositeKey, result, indexKey, entry);
                if (boundaryReached) {
                    return result;
                }
            }
        }
        return result;
    }

    private boolean visitEntry(Object compositeKey, List<ListEntry<T>> result, Key_ indexKey,
            Map.Entry<Key_, Indexer<T>> entry) {
        if (boundaryReached(entry.getKey(), indexKey)) {
            return true;
        }
        // Boundary condition not yet reached; include the indexer in the range.
        result.addAll(entry.getValue().asList(compositeKey));
        return false;
    }

    @Override
    public String toString() {
        return "size = " + comparisonMap.size();
    }

}
