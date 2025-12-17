package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;
import ai.timefold.solver.core.impl.util.ListEntry;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class ComparisonIndexer<T, Key_ extends Comparable<Key_>>
        implements Indexer<T> {

    private final KeyRetriever<Key_> keyRetriever;
    private final Supplier<Indexer<T>> downstreamIndexerSupplier;
    private final boolean reverseOrder;
    private final boolean hasOrEquals;
    private final NavigableMap<Key_, Indexer<T>> comparisonMap;

    /**
     * Construct an {@link ComparisonIndexer} which immediately ends in the backend.
     * This means {@code compositeKey} must be a single key.
     *
     * @param comparisonJoinerType the type of comparison to use
     * @param downstreamIndexerSupplier the supplier of the downstream indexer
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
        var indexKey = keyRetriever.apply(compositeKey);
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
        var indexKey = keyRetriever.apply(compositeKey);
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

    @Override
    public int size(Object compositeKey) {
        return switch (comparisonMap.size()) {
            case 0 -> 0;
            case 1 -> sizeSingleIndexer(compositeKey);
            default -> sizeManyIndexers(compositeKey);
        };
    }

    private int sizeSingleIndexer(Object compositeKey) {
        var indexKey = keyRetriever.apply(compositeKey);
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
        var indexKey = keyRetriever.apply(compositeKey);
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
                /* Nothing to do. */ }
            case 1 -> forEachSingleIndexer(compositeKey, tupleConsumer);
            default -> forEachManyIndexers(compositeKey, tupleConsumer);
        }
    }

    private void forEachSingleIndexer(Object compositeKey, Consumer<T> tupleConsumer) {
        var indexKey = keyRetriever.apply(compositeKey);
        var entry = comparisonMap.firstEntry();
        if (!boundaryReached(entry.getKey(), indexKey)) {
            // Boundary condition not yet reached; include the indexer in the range.
            entry.getValue().forEach(compositeKey, tupleConsumer);
        }
    }

    private void forEachManyIndexers(Object compositeKey, Consumer<T> tupleConsumer) {
        var indexKey = keyRetriever.apply(compositeKey);
        for (var entry : comparisonMap.entrySet()) {
            if (boundaryReached(entry.getKey(), indexKey)) {
                return;
            }
            // Boundary condition not yet reached; include the indexer in the range.
            entry.getValue().forEach(compositeKey, tupleConsumer);
        }
    }

    @Override
    public boolean isEmpty() {
        return comparisonMap.isEmpty();
    }

    @Override
    public List<? extends ListEntry<T>> asList(Object compositeKey) {
        return switch (comparisonMap.size()) {
            case 0 -> Collections.emptyList();
            case 1 -> asListSingleIndexer(compositeKey);
            default -> asListManyIndexers(compositeKey);
        };
    }

    private List<? extends ListEntry<T>> asListSingleIndexer(Object compositeKey) {
        var indexKey = keyRetriever.apply(compositeKey);
        var entry = comparisonMap.firstEntry();
        return boundaryReached(entry.getKey(), indexKey) ? Collections.emptyList() : entry.getValue().asList(compositeKey);
    }

    @SuppressWarnings("unchecked")
    private List<? extends ListEntry<T>> asListManyIndexers(Object compositeKey) {
        // The index backend's asList() may take a while to build.
        // At the same time, the elements in these lists will be accessed randomly.
        // Therefore we build this abstraction to avoid building unnecessary lists that would never get accessed.
        var result = new ComposingList<ListEntry<T>>();
        var indexKey = keyRetriever.apply(compositeKey);
        for (var entry : comparisonMap.entrySet()) {
            if (boundaryReached(entry.getKey(), indexKey)) {
                return result;
            } else { // Boundary condition not yet reached; include the indexer in the range.
                var value = entry.getValue();
                result.addSubList(() -> (List<ListEntry<T>>) value.asList(compositeKey), value.size(compositeKey));
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "size = " + comparisonMap.size();
    }

}
