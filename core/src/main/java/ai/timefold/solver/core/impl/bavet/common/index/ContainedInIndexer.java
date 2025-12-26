package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.util.ListEntry;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class ContainedInIndexer<T, Key_, KeyCollection_ extends Collection<Key_>> implements Indexer<T> {

    private final KeyRetriever<Key_> modifyKeyRetriever;
    private final KeyRetriever<KeyCollection_> queryKeyRetriever;
    private final Supplier<Indexer<T>> downstreamIndexerSupplier;
    private final Map<Key_, Indexer<T>> downstreamIndexerMap = new HashMap<>();

    /**
     * @param keyRetriever determines if it immediately goes to a {@link IndexerBackend} or if it uses a {@link CompositeKey}.
     * @param downstreamIndexerSupplier the supplier of the downstream indexer
     */
    public ContainedInIndexer(KeyRetriever<Key_> keyRetriever, Supplier<Indexer<T>> downstreamIndexerSupplier) {
        this.modifyKeyRetriever = Objects.requireNonNull(keyRetriever);
        this.queryKeyRetriever = Objects.requireNonNull((KeyRetriever<KeyCollection_>) keyRetriever);
        this.downstreamIndexerSupplier = Objects.requireNonNull(downstreamIndexerSupplier);
    }

    @Override
    public ListEntry<T> put(Object modifyCompositeKey, T tuple) {
        Key_ indexKey = modifyKeyRetriever.apply(modifyCompositeKey);
        // Avoids computeIfAbsent in order to not create lambdas on the hot path.
        Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            downstreamIndexer = downstreamIndexerSupplier.get();
            downstreamIndexerMap.put(indexKey, downstreamIndexer);
        }
        return downstreamIndexer.put(modifyCompositeKey, tuple);
    }

    @Override
    public void remove(Object modifyCompositeKey, ListEntry<T> entry) {
        Key_ indexKey = modifyKeyRetriever.apply(modifyCompositeKey);
        Indexer<T> downstreamIndexer = getDownstreamIndexer(modifyCompositeKey, indexKey, entry);
        downstreamIndexer.remove(modifyCompositeKey, entry);
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
    public int size(Object queryCompositeKey) {
        KeyCollection_ indexKeyCollection = queryKeyRetriever.apply(queryCompositeKey);
        int size = 0;
        for (Key_ indexKey : indexKeyCollection) {
            Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKey);
            if (downstreamIndexer != null) {
                size += downstreamIndexer.size(queryCompositeKey);
            }
        }
        return size;
    }

    @Override
    public void forEach(Object queryCompositeKey, Consumer<T> tupleConsumer) {
        KeyCollection_ indexKeyCollection = queryKeyRetriever.apply(queryCompositeKey);
        for (Key_ indexKey : indexKeyCollection) {
            Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKey);
            if (downstreamIndexer != null) {
                downstreamIndexer.forEach(queryCompositeKey, tupleConsumer);
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return downstreamIndexerMap.isEmpty();
    }

    @Override
    public List<? extends ListEntry<T>> asList(Object queryCompositeKey) {
        KeyCollection_ indexKeyCollection = queryKeyRetriever.apply(queryCompositeKey);
        List<ListEntry<T>> list = new ArrayList<>(downstreamIndexerMap.size() * 16);
        for (Key_ indexKey : indexKeyCollection) {
            Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKey);
            if (downstreamIndexer != null) {
                list.addAll(downstreamIndexer.asList(queryCompositeKey));
            }
        }
        return list;
    }

    @Override
    public String toString() {
        return "size = " + downstreamIndexerMap.size();
    }

}
