package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.util.ListEntry;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * As defined by {@link Joiners#containedIn(Function, Function)}
 */
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
        if (downstreamIndexer.isRemovable()) {
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
    public Iterator<T> iterator(Object queryCompositeKey) {
        return new DefaultIterator(queryCompositeKey);
    }

    @Override
    public ListEntry<T> get(Object queryCompositeKey, int index) {
        KeyCollection_ indexKeyCollection = queryKeyRetriever.apply(queryCompositeKey);
        var seenCount = 0;
        for (Key_ indexKey : indexKeyCollection) {
            Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKey);
            if (downstreamIndexer == null) {
                continue;
            }
            int downstreamSize = downstreamIndexer.size(queryCompositeKey);
            if (index < seenCount + downstreamSize) {
                return downstreamIndexer.get(queryCompositeKey, index - seenCount);
            } else {
                seenCount += downstreamSize;
            }
        }
        throw new IndexOutOfBoundsException("Index: %d".formatted(index));
    }

    @Override
    public boolean isRemovable() {
        return downstreamIndexerMap.isEmpty();
    }

    @Override
    public String toString() {
        return "size = " + downstreamIndexerMap.size();
    }

    private final class DefaultIterator implements Iterator<T> {

        private final Object queryCompositeKey;
        private final Iterator<Key_> indexerIterator;
        private @Nullable Iterator<T> downstreamIterator = null;
        private @Nullable T next = null;

        public DefaultIterator(Object queryCompositeKey) {
            this.queryCompositeKey = queryCompositeKey;
            var keyCollection = queryKeyRetriever.apply(queryCompositeKey);
            this.indexerIterator = keyCollection.iterator();
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
                var indexKey = indexerIterator.next();
                // Boundary condition not yet reached; include the indexer in the range.
                Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKey);
                if (downstreamIndexer == null) {
                    continue;
                }
                downstreamIterator = downstreamIndexer.iterator(queryCompositeKey);
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
