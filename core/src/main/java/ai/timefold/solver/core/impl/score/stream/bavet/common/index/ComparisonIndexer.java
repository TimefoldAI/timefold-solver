package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.JoinerType;
import ai.timefold.solver.core.impl.util.ElementAwareListEntry;

final class ComparisonIndexer<T, Key_ extends Comparable<Key_>>
        implements Indexer<T> {

    private final int propertyIndex;
    private final Supplier<Indexer<T>> downstreamIndexerSupplier;
    private final Comparator<Key_> keyComparator;
    private final boolean hasOrEquals;
    private final NavigableMap<Key_, Indexer<T>> comparisonMap;

    public ComparisonIndexer(JoinerType comparisonJoinerType, Supplier<Indexer<T>> downstreamIndexerSupplier) {
        this(comparisonJoinerType, 0, downstreamIndexerSupplier);
    }

    public ComparisonIndexer(JoinerType comparisonJoinerType, int propertyIndex,
            Supplier<Indexer<T>> downstreamIndexerSupplier) {
        this.propertyIndex = propertyIndex;
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
    public ElementAwareListEntry<T> put(IndexProperties indexProperties, T tuple) {
        Key_ indexKey = indexProperties.toKey(propertyIndex);
        // Avoids computeIfAbsent in order to not create lambdas on the hot path.
        var downstreamIndexer = comparisonMap.get(indexKey);
        if (downstreamIndexer == null) {
            downstreamIndexer = downstreamIndexerSupplier.get();
            comparisonMap.put(indexKey, downstreamIndexer);
        }
        return downstreamIndexer.put(indexProperties, tuple);
    }

    @Override
    public void remove(IndexProperties indexProperties, ElementAwareListEntry<T> entry) {
        Key_ indexKey = indexProperties.toKey(propertyIndex);
        var downstreamIndexer = getDownstreamIndexer(indexProperties, indexKey, entry);
        downstreamIndexer.remove(indexProperties, entry);
        if (downstreamIndexer.isEmpty()) {
            comparisonMap.remove(indexKey);
        }
    }

    private Indexer<T> getDownstreamIndexer(IndexProperties indexProperties, Key_ indexerKey, ElementAwareListEntry<T> entry) {
        var downstreamIndexer = comparisonMap.get(indexerKey);
        if (downstreamIndexer == null) {
            throw new IllegalStateException("Impossible state: the tuple (" + entry.getElement()
                    + ") with indexProperties (" + indexProperties
                    + ") doesn't exist in the indexer " + this + ".");
        }
        return downstreamIndexer;
    }

    // TODO clean up DRY
    @Override
    public int size(IndexProperties indexProperties) {
        var mapSize = comparisonMap.size();
        if (mapSize == 0) {
            return 0;
        }
        Key_ indexKey = indexProperties.toKey(propertyIndex);
        if (mapSize == 1) { // Avoid creation of the entry set and iterator.
            var entry = comparisonMap.firstEntry();
            var comparison = keyComparator.compare(entry.getKey(), indexKey);
            if (comparison >= 0) { // Possibility of reaching the boundary condition.
                if (comparison > 0 || !hasOrEquals) {
                    // Boundary condition reached when we're out of bounds entirely, or when GTE/LTE is not allowed.
                    return 0;
                }
            }
            return entry.getValue().size(indexProperties);
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
                size += entry.getValue().size(indexProperties);
            }
            return size;
        }
    }

    @Override
    public void forEach(IndexProperties indexProperties, Consumer<T> tupleConsumer) {
        var size = comparisonMap.size();
        if (size == 0) {
            return;
        }
        Key_ indexKey = indexProperties.toKey(propertyIndex);
        if (size == 1) { // Avoid creation of the entry set and iterator.
            var entry = comparisonMap.firstEntry();
            visitEntry(indexProperties, tupleConsumer, indexKey, entry);
        } else {
            for (var entry : comparisonMap.entrySet()) {
                var boundaryReached = visitEntry(indexProperties, tupleConsumer, indexKey, entry);
                if (boundaryReached) {
                    return;
                }
            }
        }
    }

    private boolean visitEntry(IndexProperties indexProperties, Consumer<T> tupleConsumer, Key_ indexKey,
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
        entry.getValue().forEach(indexProperties, tupleConsumer);
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
