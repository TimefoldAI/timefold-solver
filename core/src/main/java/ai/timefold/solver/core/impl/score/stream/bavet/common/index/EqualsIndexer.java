package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.util.ElementAwareListEntry;

final class EqualsIndexer<T, Key_> implements Indexer<T> {

    static <T> Index<T> buildIndex() {
        return new IndexerBasedIndex<>(new EqualsIndexer<>());
    }

    static <T> Index<T> buildIndex(int keyId, Supplier<Index<T>> downstreamIndexSupplier) {
        return new IndexerBasedIndex<>(new EqualsIndexer<>(keyId, downstreamIndexSupplier));
    }

    private final KeyRetriever<Key_> keyRetriever;
    private final Supplier<Index<T>> downstreamStorageSupplier;
    private final Map<Key_, Index<T>> downstreamStorageMap = new HashMap<>();

    /**
     * Construct an {@link EqualsIndexer} which immediately ends in a {@link NoneIndexer}.
     * This means {@code indexKeys} must be a single key.
     */
    public EqualsIndexer() {
        this.keyRetriever = new SingleKeyRetriever<>();
        this.downstreamStorageSupplier = LinkedListBasedIndex::build;
    }

    /**
     * Construct an {@link EqualsIndexer} which does not immediately go to a {@link NoneIndexer}.
     * This means {@code indexKeys} must be an instance of {@link IndexKeys}.
     * 
     * @param keyIndex the index of the key to use within {@link IndexKeys}.
     * @param downstreamIndexSupplier the supplier of the downstream indexer
     */
    public EqualsIndexer(int keyIndex, Supplier<Index<T>> downstreamIndexSupplier) {
        this.keyRetriever = new ManyKeyRetriever<>(keyIndex);
        this.downstreamStorageSupplier = downstreamIndexSupplier;
    }

    @Override
    public ElementAwareListEntry<T> put(Object indexKeys, T tuple) {
        var indexKey = keyRetriever.apply(indexKeys);
        // Avoids computeIfAbsent in order to not create lambdas on the hot path.
        var downstreamStorage = downstreamStorageMap.get(indexKey);
        if (downstreamStorage == null) {
            downstreamStorage = downstreamStorageSupplier.get();
            downstreamStorageMap.put(indexKey, downstreamStorage);
        }
        return downstreamStorage.put(indexKeys, tuple);
    }

    @Override
    public void remove(Object indexKeys, ElementAwareListEntry<T> entry) {
        var indexKey = keyRetriever.apply(indexKeys);
        var downstreamStorage = getDownstreamStorage(indexKeys, indexKey, entry);
        downstreamStorage.remove(indexKeys, entry);
        if (downstreamStorage.isEmpty()) {
            downstreamStorageMap.remove(indexKey);
        }
    }

    private Index<T> getDownstreamStorage(Object indexKeys, Key_ indexerKey, ElementAwareListEntry<T> entry) {
        var downstreamStorage = downstreamStorageMap.get(indexerKey);
        if (downstreamStorage == null) {
            throw new IllegalStateException(
                    "Impossible state: the tuple (%s) with indexKey (%s) doesn't exist in the indexer %s."
                            .formatted(entry.getElement(), indexKeys, this));
        }
        return downstreamStorage;
    }

    @Override
    public int size(Object indexKeys) {
        var indexKey = keyRetriever.apply(indexKeys);
        var downstreamStorage = downstreamStorageMap.get(indexKey);
        if (downstreamStorage == null) {
            return 0;
        }
        return downstreamStorage.size(indexKeys);
    }

    @Override
    public void forEach(Object indexKeys, Consumer<T> tupleConsumer) {
        var indexKey = keyRetriever.apply(indexKeys);
        var downstreamStorage = downstreamStorageMap.get(indexKey);
        if (downstreamStorage == null) {
            return;
        }
        downstreamStorage.forEach(indexKeys, tupleConsumer);
    }

    @Override
    public boolean isEmpty() {
        return downstreamStorageMap.isEmpty();
    }

    @Override
    public String toString() {
        return "size = " + downstreamStorageMap.size();
    }

}
