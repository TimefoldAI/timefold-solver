package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.util.CompositeListEntry;
import ai.timefold.solver.core.impl.util.ListEntry;
import ai.timefold.solver.core.impl.util.Pair;

import org.jspecify.annotations.NullMarked;

/**
 * As defined by {@link Joiners#contain(Function, Function)}
 */
@NullMarked
final class ContainIndexer<T, Key_, KeyCollection_ extends Collection<Key_>> implements Indexer<T> {

    private final KeyUnpacker<KeyCollection_> modifyKeyUnpacker;
    private final KeyUnpacker<Key_> queryKeyUnpacker;
    private final Supplier<Indexer<T>> downstreamIndexerSupplier;
    /**
     * See {@link EqualIndexer} for explanation of the parameters.
     */
    private final Map<Key_, Indexer<T>> downstreamIndexerMap = new HashMap<>(16, 0.5f);
    private long unremovedSize = 0;

    /**
     * @param keyUnpacker determines if it immediately goes to a {@link IndexerBackend} or if it uses a {@link CompositeKey}.
     * @param downstreamIndexerSupplier the supplier of the downstream indexer
     */
    @SuppressWarnings("unchecked")
    public ContainIndexer(KeyUnpacker<Key_> keyUnpacker, Supplier<Indexer<T>> downstreamIndexerSupplier) {
        this.modifyKeyUnpacker = Objects.requireNonNull((KeyUnpacker<KeyCollection_>) keyUnpacker);
        this.queryKeyUnpacker = Objects.requireNonNull(keyUnpacker);
        this.downstreamIndexerSupplier = Objects.requireNonNull(downstreamIndexerSupplier);
    }

    @Override
    public ListEntry<T> put(Object modifyCompositeKey, T tuple) {
        unremovedSize++;
        var indexKeyCollection = modifyKeyUnpacker.apply(modifyCompositeKey);
        var children = new ArrayList<Pair<Key_, ListEntry<T>>>(indexKeyCollection.size());
        for (var indexKey : indexKeyCollection) {
            // Avoids computeIfAbsent in order to not create lambdas on the hot path.
            var downstreamIndexer = downstreamIndexerMap.get(indexKey);
            if (downstreamIndexer == null) {
                downstreamIndexer = downstreamIndexerSupplier.get();
                downstreamIndexerMap.put(indexKey, downstreamIndexer);
            }
            // Even though this method puts a tuple in multiple downstreamIndexers, it does not break size() or forEach()
            // because at most one of those downstreamIndexers matches for a particular compositeKey
            var childListEntry = downstreamIndexer.put(modifyCompositeKey, tuple);
            children.add(new Pair<>(indexKey, childListEntry));
        }
        return new CompositeListEntry<>(tuple, children);
    }

    @Override
    public void remove(Object modifyCompositeKey, ListEntry<T> entry) {
        unremovedSize--;
        var indexKeyCollection = modifyKeyUnpacker.apply(modifyCompositeKey);
        var children = ((CompositeListEntry<Key_, T>) entry).children();
        if (indexKeyCollection.size() != children.size()) {
            throw new IllegalStateException("""
                    Impossible state: the tuple (%s) with composite key (%s) has a different number of children (%d) \
                    than the index key collection size (%d)."""
                    .formatted(entry, modifyCompositeKey, children.size(), indexKeyCollection.size()));
        }
        for (var i = 0; i < indexKeyCollection.size(); i++) { // Avoid creating an iterator on the hot path
            var child = children.get(i);
            var indexKey = child.key();
            var childListEntry = child.value();
            // Avoids removeIfAbsent in order to not create lambdas on the hot path.
            var downstreamIndexer = getDownstreamIndexer(modifyCompositeKey, indexKey);
            downstreamIndexer.remove(modifyCompositeKey, childListEntry);
            if (downstreamIndexer.isRemovable()) {
                downstreamIndexerMap.remove(indexKey);
            }
        }
    }

    private Indexer<T> getDownstreamIndexer(Object compositeKey, Key_ indexerKey) {
        var downstreamIndexer = downstreamIndexerMap.get(indexerKey);
        if (downstreamIndexer == null) {
            throw new IllegalStateException(
                    "Impossible state: the composite key (%s) doesn't exist in the indexer %s."
                            .formatted(compositeKey, this));
        }
        return downstreamIndexer;
    }

    @Override
    public int size(Object queryCompositeKey) {
        var indexKey = queryKeyUnpacker.apply(queryCompositeKey);
        var downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            return 0;
        }
        return downstreamIndexer.size(queryCompositeKey);
    }

    @Override
    public void forEach(Object queryCompositeKey, Consumer<T> tupleConsumer) {
        var indexKey = queryKeyUnpacker.apply(queryCompositeKey);
        var downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            return;
        }
        downstreamIndexer.forEach(queryCompositeKey, tupleConsumer);
    }

    @Override
    public Iterator<T> iterator(Object queryCompositeKey) {
        var indexKey = queryKeyUnpacker.apply(queryCompositeKey);
        var downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            return Collections.emptyIterator();
        }
        return downstreamIndexer.iterator(queryCompositeKey);
    }

    @Override
    public ListEntry<T> get(Object queryCompositeKey, int index) {
        var indexKey = queryKeyUnpacker.apply(queryCompositeKey);
        var downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            throw new IndexOutOfBoundsException("Index: %d"
                    .formatted(index));
        }
        return downstreamIndexer.get(queryCompositeKey, index);
    }

    @Override
    public boolean isRemovable() {
        return unremovedSize == 0;
    }

    @Override
    public String toString() {
        return "size = " + downstreamIndexerMap.size();
    }

}
