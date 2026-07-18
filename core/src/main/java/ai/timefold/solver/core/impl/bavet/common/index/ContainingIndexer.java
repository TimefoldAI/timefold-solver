package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.util.ListEntry;
import ai.timefold.solver.core.impl.util.Triple;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * As defined by {@link Joiners#containing(Function, Function)}
 */
@NullMarked
final class ContainingIndexer<T, Key_, KeyCollection_ extends SequencedCollection<Key_>> implements Indexer<T> {

    private final KeyUnpacker<KeyCollection_> modifyKeyUnpacker;
    private final KeyUnpacker<Key_> queryKeyUnpacker;
    private final Supplier<Indexer<T>> downstreamIndexerSupplier;
    /**
     * See {@link EqualIndexer} for explanation of the parameters.
     */
    private final Map<Key_, Indexer<T>> downstreamIndexerMap = new HashMap<>(16, 0.5f);
    private long unremovedSize = 0;

    /**
     * @param keyUnpacker determines if it immediately goes to a {@link LeafIndexer} or if it uses a {@link CompositeKey}.
     * @param downstreamIndexerSupplier the supplier of the downstream indexer
     */
    @SuppressWarnings("unchecked")
    public ContainingIndexer(KeyUnpacker<Key_> keyUnpacker, Supplier<Indexer<T>> downstreamIndexerSupplier) {
        this.modifyKeyUnpacker = Objects.requireNonNull((KeyUnpacker<KeyCollection_>) keyUnpacker);
        this.queryKeyUnpacker = Objects.requireNonNull(keyUnpacker);
        this.downstreamIndexerSupplier = Objects.requireNonNull(downstreamIndexerSupplier);
    }

    @Override
    public ListEntry<T> put(Object modifyCompositeKey, T tuple) {
        unremovedSize++;
        var indexKeyCollection = modifyKeyUnpacker.apply(modifyCompositeKey);
        var children = new ArrayList<Triple<Key_, Indexer<T>, ListEntry<T>>>(indexKeyCollection.size());
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
            // The downstream indexer rides along so that remove() doesn't need to look it up again.
            children.add(new Triple<>(indexKey, downstreamIndexer, childListEntry));
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
            var downstreamIndexer = child.b();
            downstreamIndexer.remove(modifyCompositeKey, child.c());
            if (downstreamIndexer.isRemovable()) {
                downstreamIndexerMap.remove(child.a());
            }
        }
    }

    @Override
    public int size(Object queryCompositeKey) {
        if (downstreamIndexerMap.isEmpty()) {
            return 0;
        }
        var indexKey = queryKeyUnpacker.apply(queryCompositeKey);
        var downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            return 0;
        }
        return downstreamIndexer.size(queryCompositeKey);
    }

    @Override
    public void forEach(Object queryCompositeKey, Consumer<T> tupleConsumer) {
        if (downstreamIndexerMap.isEmpty()) {
            return;
        }
        var indexKey = queryKeyUnpacker.apply(queryCompositeKey);
        var downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            return;
        }
        downstreamIndexer.forEach(queryCompositeKey, tupleConsumer);
    }

    @Override
    public Iterator<T> iterator(Object queryCompositeKey) {
        if (downstreamIndexerMap.isEmpty()) {
            return Collections.emptyIterator();
        }
        var indexKey = queryKeyUnpacker.apply(queryCompositeKey);
        var downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            return Collections.emptyIterator();
        }
        return downstreamIndexer.iterator(queryCompositeKey);
    }

    @Override
    public Iterator<T> randomIterator(Object queryCompositeKey, RandomGenerator workingRandom) {
        return createRandomIterator(queryCompositeKey, workingRandom, null);
    }

    private Iterator<T> createRandomIterator(Object queryCompositeKey, RandomGenerator workingRandom,
            @Nullable Predicate<T> filter) {
        if (downstreamIndexerMap.isEmpty()) {
            return Collections.emptyIterator();
        }

        var indexKey = queryKeyUnpacker.apply(queryCompositeKey);
        var downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            return Collections.emptyIterator();
        }

        if (filter == null) {
            return downstreamIndexer.randomIterator(queryCompositeKey, workingRandom);
        } else {
            return downstreamIndexer.randomIterator(queryCompositeKey, workingRandom, filter);
        }
    }

    @Override
    public Iterator<T> randomIterator(Object queryCompositeKey, RandomGenerator workingRandom, Predicate<T> filter) {
        return createRandomIterator(queryCompositeKey, workingRandom, filter);
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
