package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.util.CompositeListEntry;
import ai.timefold.solver.core.impl.util.ListEntry;
import ai.timefold.solver.core.impl.util.Pair;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * As defined by {@link Joiners#containAny(Function, Function)}
 */
@NullMarked
final class ContainAnyIndexer<T, Key_, KeyCollection_ extends Collection<Key_>> implements Indexer<T> {

    private final KeyRetriever<KeyCollection_> modifyKeyRetriever;
    private final KeyRetriever<KeyCollection_> queryKeyRetriever;
    private final Supplier<Indexer<T>> downstreamIndexerSupplier;
    private final Map<Key_, Indexer<T>> downstreamIndexerMap = new HashMap<>();
    private long unremovedSize = 0;

    /**
     * @param keyRetriever determines if it immediately goes to a {@link IndexerBackend} or if it uses a {@link CompositeKey}.
     * @param downstreamIndexerSupplier the supplier of the downstream indexer
     */
    public ContainAnyIndexer(KeyRetriever<Key_> keyRetriever, Supplier<Indexer<T>> downstreamIndexerSupplier) {
        this.modifyKeyRetriever = Objects.requireNonNull((KeyRetriever<KeyCollection_>) keyRetriever);
        this.queryKeyRetriever = Objects.requireNonNull((KeyRetriever<KeyCollection_>) keyRetriever);
        this.downstreamIndexerSupplier = Objects.requireNonNull(downstreamIndexerSupplier);
    }

    @Override
    public ListEntry<T> put(Object modifyCompositeKey, T tuple) {
        unremovedSize++;
        KeyCollection_ indexKeyCollection = modifyKeyRetriever.apply(modifyCompositeKey);
        List<Pair<Key_, ListEntry<T>>> children = new ArrayList<>(indexKeyCollection.size());
        for (Key_ indexKey : indexKeyCollection) {
            // Avoids computeIfAbsent in order to not create lambdas on the hot path.
            Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKey);
            if (downstreamIndexer == null) {
                downstreamIndexer = downstreamIndexerSupplier.get();
                downstreamIndexerMap.put(indexKey, downstreamIndexer);
            }
            // Even though this method puts a tuple in multiple downstreamIndexers, it does not break size() or forEach()
            // because at most one of those downstreamIndexers matches for a particular compositeKey
            ListEntry<T> childListEntry = downstreamIndexer.put(modifyCompositeKey, tuple);
            children.add(new Pair<>(indexKey, childListEntry));
        }
        return new CompositeListEntry<>(tuple, children);
    }

    @Override
    public void remove(Object modifyCompositeKey, ListEntry<T> entry) {
        unremovedSize--;
        KeyCollection_ indexKeyCollection = modifyKeyRetriever.apply(modifyCompositeKey);
        List<Pair<Key_, ListEntry<T>>> children = ((CompositeListEntry<Key_, T>) entry).getChildren();
        if (indexKeyCollection.size() != children.size()) {
            throw new IllegalStateException(
                    ("Impossible state: the tuple (%s) with composite key (%s) has a different number of children (%d)" +
                            " than the index key collection size (%d).")
                            .formatted(entry, modifyCompositeKey, children.size(), indexKeyCollection.size()));
        }
        for (Pair<Key_, ListEntry<T>> child : children) {
            Key_ indexKey = child.key();
            ListEntry<T> childListEntry = child.value();
            // Avoids removeIfAbsent in order to not create lambdas on the hot path.
            Indexer<T> downstreamIndexer = getDownstreamIndexer(modifyCompositeKey, indexKey);
            downstreamIndexer.remove(modifyCompositeKey, childListEntry);
            if (downstreamIndexer.isRemovable()) {
                downstreamIndexerMap.remove(indexKey);
            }
        }
    }

    private Indexer<T> getDownstreamIndexer(Object compositeKey, Key_ indexerKey) {
        Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexerKey);
        if (downstreamIndexer == null) {
            throw new IllegalStateException(
                    "Impossible state: the composite key (%s) doesn't exist in the indexer %s."
                            .formatted(compositeKey, this));
        }
        return downstreamIndexer;
    }

    @Override
    public int size(Object queryCompositeKey) {
        KeyCollection_ indexKeyCollection = queryKeyRetriever.apply(queryCompositeKey);
        return switch (indexKeyCollection.size()) {
            case 0 -> 0;
            case 1 -> {
                Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKeyCollection.iterator().next());
                yield (downstreamIndexer == null) ? 0 : downstreamIndexer.size(queryCompositeKey);
            }
            default -> {
                AtomicInteger size = new AtomicInteger(0);
                forEach(queryCompositeKey, tuple -> size.incrementAndGet());
                yield size.get();
            }
        };
    }

    @Override
    public void forEach(Object queryCompositeKey, Consumer<T> tupleConsumer) {
        KeyCollection_ indexKeyCollection = queryKeyRetriever.apply(queryCompositeKey);
        switch (indexKeyCollection.size()) {
            case 0 -> {
                // Do nothing;
            }
            case 1 -> {
                Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKeyCollection.iterator().next());
                if (downstreamIndexer != null) {
                    downstreamIndexer.forEach(queryCompositeKey, tupleConsumer);
                }
            }
            default -> {
                Set<T> distinctingSet = new HashSet<>(indexKeyCollection.size() * 16);
                for (Key_ indexKey : indexKeyCollection) {
                    Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKey);
                    if (downstreamIndexer != null) {
                        downstreamIndexer.forEach(queryCompositeKey, tuple -> {
                            if (distinctingSet.add(tuple)) {
                                tupleConsumer.accept(tuple);
                            }
                        });
                    }
                }
            }
        }
        ;
    }

    @Override
    public Iterator<T> iterator(Object queryCompositeKey) {
        KeyCollection_ indexKeyCollection = queryKeyRetriever.apply(queryCompositeKey);
        return switch (indexKeyCollection.size()) {
            case 0 -> Collections.emptyIterator();
            case 1 -> {
                Key_ indexKey = indexKeyCollection.iterator().next();
                Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKey);
                if (downstreamIndexer == null) {
                    yield Collections.emptyIterator();
                }
                yield downstreamIndexer.iterator(queryCompositeKey);
            }
            default -> new DefaultIterator(queryCompositeKey);
        };
    }

    @Override
    public ListEntry<T> get(Object queryCompositeKey, int index) {
        KeyCollection_ indexKeyCollection = queryKeyRetriever.apply(queryCompositeKey);
        return switch (indexKeyCollection.size()) {
            case 0 -> throw new IndexOutOfBoundsException("Index: %d".formatted(index));
            case 1 -> {
                Key_ indexKey = indexKeyCollection.iterator().next();
                Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKey);
                if (downstreamIndexer == null) {
                    throw new IndexOutOfBoundsException("Index: %d".formatted(index));
                }
                yield downstreamIndexer.get(queryCompositeKey, index);
            }
            default -> {
                Set<T> distinctingSet = new HashSet<>(indexKeyCollection.size() * 16);
                var seenCount = 0;
                for (Key_ indexKey : indexKeyCollection) {
                    Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKey);
                    if (downstreamIndexer == null) {
                        continue;
                    }
                    var downstreamIndexerIterator = downstreamIndexer.iterator(queryCompositeKey);
                    while (downstreamIndexerIterator.hasNext()) { // To avoid capturing a lambda on the hot path.
                        T tuple = downstreamIndexerIterator.next();
                        if (distinctingSet.add(tuple)) {
                            var size = downstreamIndexer.size(queryCompositeKey);
                            if (index < seenCount + size) {
                                yield downstreamIndexer.get(queryCompositeKey, index - seenCount);
                            } else {
                                seenCount += size;
                            }
                        }
                    }
                }
                throw new IndexOutOfBoundsException("Index: %d".formatted(index));
            }
        };
    }

    @Override
    public boolean isRemovable() {
        return unremovedSize == 0;
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
        private final Set<T> distinctingSet;

        public DefaultIterator(Object queryCompositeKey) {
            this.queryCompositeKey = queryCompositeKey;
            var keyCollection = queryKeyRetriever.apply(queryCompositeKey);
            this.indexerIterator = keyCollection.iterator();
            this.distinctingSet = new HashSet<>(keyCollection.size() * 16);
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
                Indexer<T> downstreamIndexer = downstreamIndexerMap.get(indexKey);
                if (downstreamIndexer == null) {
                    continue;
                }
                downstreamIterator = downstreamIndexer.iterator(queryCompositeKey);
                while (downstreamIterator.hasNext()) {
                    T tuple = downstreamIterator.next();
                    if (distinctingSet.add(tuple)) {
                        next = tuple;
                        return true;
                    }
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
