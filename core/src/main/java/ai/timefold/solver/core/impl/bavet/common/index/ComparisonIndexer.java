package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;
import ai.timefold.solver.core.impl.util.ElementAwareListEntry;

final class ComparisonIndexer<T, Key_ extends Comparable<Key_>>
        implements Indexer<T> {

    private final KeyRetriever<Key_> keyRetriever;
    private final Supplier<Indexer<T>> downstreamIndexerSupplier;
    private final Comparator<Key_> keyComparator;
    private final boolean hasOrEquals;
    private final NavigableMap<Key_, Indexer<T>> comparisonMap;

    /**
     * Construct an {@link ComparisonIndexer} which immediately ends in a {@link NoneIndexer}.
     * This means {@code indexKeys} must be a single key.
     *
     * @param comparisonJoinerType the type of comparison to use
     */
    public ComparisonIndexer(JoinerType comparisonJoinerType) {
        this(comparisonJoinerType, new SingleKeyRetriever<>(), NoneIndexer::new);
    }

    /**
     * Construct an {@link ComparisonIndexer} which does not immediately go to a {@link NoneIndexer}.
     * This means {@code indexKeys} must be an instance of {@link IndexKeys}.
     *
     * @param comparisonJoinerType the type of comparison to use
     * @param keyIndex the index of the key to use within {@link IndexKeys}.
     * @param downstreamIndexerSupplier the supplier of the downstream indexer
     */
    public ComparisonIndexer(JoinerType comparisonJoinerType, int keyIndex, Supplier<Indexer<T>> downstreamIndexerSupplier) {
        this(comparisonJoinerType, new ManyKeyRetriever<>(keyIndex), downstreamIndexerSupplier);
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
    public ElementAwareListEntry<T> put(Object indexKeys, T tuple) {
        Key_ indexKey = keyRetriever.apply(indexKeys);
        // Avoids computeIfAbsent in order to not create lambdas on the hot path.
        var downstreamIndexer = comparisonMap.get(indexKey);
        if (downstreamIndexer == null) {
            downstreamIndexer = downstreamIndexerSupplier.get();
            comparisonMap.put(indexKey, downstreamIndexer);
        }
        return downstreamIndexer.put(indexKeys, tuple);
    }

    @Override
    public void remove(Object indexKeys, ElementAwareListEntry<T> entry) {
        Key_ indexKey = keyRetriever.apply(indexKeys);
        var downstreamIndexer = getDownstreamIndexer(indexKeys, indexKey, entry);
        downstreamIndexer.remove(indexKeys, entry);
        if (downstreamIndexer.isEmpty()) {
            comparisonMap.remove(indexKey);
        }
    }

    private Indexer<T> getDownstreamIndexer(Object indexKeys, Key_ indexerKey, ElementAwareListEntry<T> entry) {
        var downstreamIndexer = comparisonMap.get(indexerKey);
        if (downstreamIndexer == null) {
            throw new IllegalStateException(
                    "Impossible state: the tuple (%s) with indexKeys (%s) doesn't exist in the indexer %s."
                            .formatted(entry.getElement(), indexKeys, this));
        }
        return downstreamIndexer;
    }

    // TODO clean up DRY
    @Override
    public int size(Object indexKeys) {
        var mapSize = comparisonMap.size();
        if (mapSize == 0) {
            return 0;
        }
        Key_ indexKey = keyRetriever.apply(indexKeys);
        if (mapSize == 1) { // Avoid creation of the entry set and iterator.
            var entry = comparisonMap.firstEntry();
            var comparison = keyComparator.compare(entry.getKey(), indexKey);
            if (comparison >= 0) { // Possibility of reaching the boundary condition.
                if (comparison > 0 || !hasOrEquals) {
                    // Boundary condition reached when we're out of bounds entirely, or when GTE/LTE is not allowed.
                    return 0;
                }
            }
            return entry.getValue().size(indexKeys);
        } else {
            var size = 0;
            for (var entry : comparisonMap.entrySet()) {
                var comparison = keyComparator.compare(entry.getKey(), indexKey);
                if (comparison >= 0) { // Possibility of reaching the boundary condition.
                    if (comparison > 0 || !hasOrEquals) {
                        // Boundary condition reached when we're out of bounds entirely, or when GTE/LTE is not allowed.
                        break;
                    }
                }
                // Boundary condition not yet reached; include the indexer in the range.
                size += entry.getValue().size(indexKeys);
            }
            return size;
        }
    }

    @Override
    public void forEach(Object indexKeys, Consumer<T> tupleConsumer) {
        var size = comparisonMap.size();
        if (size == 0) {
            return;
        }
        Key_ indexKey = keyRetriever.apply(indexKeys);
        if (size == 1) { // Avoid creation of the entry set and iterator.
            var entry = comparisonMap.firstEntry();
            visitEntry(indexKeys, tupleConsumer, indexKey, entry);
        } else {
            for (var entry : comparisonMap.entrySet()) {
                var boundaryReached = visitEntry(indexKeys, tupleConsumer, indexKey, entry);
                if (boundaryReached) {
                    return;
                }
            }
        }
    }

    private boolean visitEntry(Object indexKeys, Consumer<T> tupleConsumer, Key_ indexKey,
            Map.Entry<Key_, Indexer<T>> entry) {
        // Comparator matches the order of iteration of the map, so the boundary is always found from the bottom up.
        var comparison = keyComparator.compare(entry.getKey(), indexKey);
        if (comparison >= 0) { // Possibility of reaching the boundary condition.
            if (comparison > 0 || !hasOrEquals) {
                // Boundary condition reached when we're out of bounds entirely, or when GTE/LTE is not allowed.
                return true;
            }
        }
        // Boundary condition not yet reached; include the indexer in the range.
        entry.getValue().forEach(indexKeys, tupleConsumer);
        return false;
    }

    @Override
    public boolean isEmpty() {
        return comparisonMap.isEmpty();
    }

    @Override
    public String toString() {
        return "size = " + comparisonMap.size();
    }

}
