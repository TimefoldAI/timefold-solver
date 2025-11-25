package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.util.ListEntry;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class EqualsIndexer<T, Key_> implements Indexer<T> {

    private final KeyRetriever<Key_> keyRetriever;
    private final Supplier<Indexer<T>> downstreamIndexerSupplier;
    private final Map<Key_, Indexer<T>> downstreamIndexerMap = new HashMap<>();

    /**
     * Construct an {@link EqualsIndexer} which immediately ends in the backend.
     * This means {@code compositeKey} must be a single key.
     */
    public EqualsIndexer(Supplier<Indexer<T>> downstreamIndexerSupplier) {
        this.keyRetriever = new SingleKeyRetriever<>();
        this.downstreamIndexerSupplier = downstreamIndexerSupplier;
    }

    /**
     * Construct an {@link EqualsIndexer} which does not immediately go to a {@link IndexerBackend}.
     * This means {@code compositeKey} must be an instance of {@link CompositeKey}.
     * 
     * @param keyIndex the index of the key to use within {@link CompositeKey}.
     * @param downstreamIndexerSupplier the supplier of the downstream indexer
     */
    public EqualsIndexer(int keyIndex, Supplier<Indexer<T>> downstreamIndexerSupplier) {
        this.keyRetriever = new CompositeKeyRetriever<>(keyIndex);
        this.downstreamIndexerSupplier = Objects.requireNonNull(downstreamIndexerSupplier);
    }

    @Override
    public ListEntry<T> put(Object compositeKey, T tuple) {
        Key_ indexKey = keyRetriever.apply(compositeKey);
        // Avoids computeIfAbsent in order to not create lambdas on the hot path.
        Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            downstreamIndexer = downstreamIndexerSupplier.get();
            downstreamIndexerMap.put(indexKey, downstreamIndexer);
        }
        return downstreamIndexer.put(compositeKey, tuple);
    }

    @Override
    public void remove(Object compositeKey, ListEntry<T> entry) {
        Key_ indexKey = keyRetriever.apply(compositeKey);
        Indexer<T> downstreamIndexer = getDownstreamIndexer(compositeKey, indexKey, entry);
        downstreamIndexer.remove(compositeKey, entry);
        if (downstreamIndexer.isEmpty()) {
            downstreamIndexerMap.remove(indexKey);
        }
    }

    private Indexer<T> getDownstreamIndexer(Object compositeKey, Key_ indexerKey, ListEntry<T> entry) {
        Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexerKey);
        if (downstreamIndexer == null) {
            throw new IllegalStateException(
                    "Impossible state: the tuple (%s) with composite key (%s) doesn't exist in the indexer %s."
                            .formatted(entry, compositeKey, this));
        }
        return downstreamIndexer;
    }

    @Override
    public int size(Object compositeKey) {
        Key_ indexKey = keyRetriever.apply(compositeKey);
        Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            return 0;
        }
        return downstreamIndexer.size(compositeKey);
    }

    @Override
    public void forEach(Object compositeKey, Consumer<T> tupleConsumer) {
        Key_ indexKey = keyRetriever.apply(compositeKey);
        Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            return;
        }
        downstreamIndexer.forEach(compositeKey, tupleConsumer);
    }

    @Override
    public boolean isEmpty() {
        return downstreamIndexerMap.isEmpty();
    }

    @Override
    public List<? extends ListEntry<T>> asList(Object compositeKey) {
        Key_ indexKey = keyRetriever.apply(compositeKey);
        Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            return Collections.emptyList();
        }
        return downstreamIndexer.asList(compositeKey);
    }

    @Override
    public String toString() {
        return "size = " + downstreamIndexerMap.size();
    }

}
