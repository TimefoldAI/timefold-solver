package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.util.ElementAwareListEntry;

final class EqualsIndexer<T, Key_> implements Indexer<T> {

    private final int keyIndex;
    private final Supplier<Indexer<T>> downstreamIndexerSupplier;
    private final Map<Key_, Indexer<T>> downstreamIndexerMap = new HashMap<>();

    public EqualsIndexer(Supplier<Indexer<T>> downstreamIndexerSupplier) {
        this(0, downstreamIndexerSupplier);
    }

    public EqualsIndexer(int keyIndex, Supplier<Indexer<T>> downstreamIndexerSupplier) {
        this.keyIndex = keyIndex;
        this.downstreamIndexerSupplier = Objects.requireNonNull(downstreamIndexerSupplier);
    }

    @Override
    public ElementAwareListEntry<T> put(Object indexKeys, T tuple) {
        Key_ indexKey = Indexer.of(indexKeys, keyIndex);
        // Avoids computeIfAbsent in order to not create lambdas on the hot path.
        Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            downstreamIndexer = downstreamIndexerSupplier.get();
            downstreamIndexerMap.put(indexKey, downstreamIndexer);
        }
        return downstreamIndexer.put(indexKeys, tuple);
    }

    @Override
    public void remove(Object indexKeys, ElementAwareListEntry<T> entry) {
        Key_ indexKey = Indexer.of(indexKeys, keyIndex);
        Indexer<T> downstreamIndexer = getDownstreamIndexer(indexKeys, indexKey, entry);
        downstreamIndexer.remove(indexKeys, entry);
        if (downstreamIndexer.isEmpty()) {
            downstreamIndexerMap.remove(indexKey);
        }
    }

    private Indexer<T> getDownstreamIndexer(Object indexKeys, Key_ indexerKey, ElementAwareListEntry<T> entry) {
        Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexerKey);
        if (downstreamIndexer == null) {
            throw new IllegalStateException(
                    "Impossible state: the tuple (%s) with indexKey (%s) doesn't exist in the indexer %s."
                            .formatted(entry.getElement(), indexKeys, this));
        }
        return downstreamIndexer;
    }

    @Override
    public int size(Object indexKeys) {
        Key_ indexKey = Indexer.of(indexKeys, keyIndex);
        Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            return 0;
        }
        return downstreamIndexer.size(indexKeys);
    }

    @Override
    public void forEach(Object indexKeys, Consumer<T> tupleConsumer) {
        Key_ indexKey = Indexer.of(indexKeys, keyIndex);
        Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            return;
        }
        downstreamIndexer.forEach(indexKeys, tupleConsumer);
    }

    @Override
    public boolean isEmpty() {
        return downstreamIndexerMap.isEmpty();
    }

    @Override
    public String toString() {
        return "size = " + downstreamIndexerMap.size();
    }

}
