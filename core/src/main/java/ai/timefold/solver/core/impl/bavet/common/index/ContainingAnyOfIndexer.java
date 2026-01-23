package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.UnfinishedJoiners;
import ai.timefold.solver.core.impl.util.CompositeListEntry;
import ai.timefold.solver.core.impl.util.ListEntry;
import ai.timefold.solver.core.impl.util.Pair;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * As defined by {@link UnfinishedJoiners#containingAnyOf(Function, Function)}
 */
@NullMarked
final class ContainingAnyOfIndexer<T, Key_, KeyCollection_ extends Collection<Key_>> implements Indexer<T> {

    private final KeyUnpacker<KeyCollection_> modifyKeyUnpacker;
    private final KeyUnpacker<KeyCollection_> queryKeyUnpacker;
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
    public ContainingAnyOfIndexer(KeyUnpacker<Key_> keyUnpacker, Supplier<Indexer<T>> downstreamIndexerSupplier) {
        this.modifyKeyUnpacker = Objects.requireNonNull((KeyUnpacker<KeyCollection_>) keyUnpacker);
        this.queryKeyUnpacker = Objects.requireNonNull((KeyUnpacker<KeyCollection_>) keyUnpacker);
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
            // because even though those downstreamIndexers match for a particular compositeKey,
            // the distinctingSet in those methods ensures that each tuple is only counted/consumed once.
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
        for (var i = 0; i < children.size(); i++) { // Avoids creating an iterator on the hot path.
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
                    "Impossible state: the composite key (%s) doesn't exist in the indexer %s.".formatted(compositeKey, this));
        }
        return downstreamIndexer;
    }

    @Override
    public int size(Object queryCompositeKey) {
        var indexKeyCollection = queryKeyUnpacker.apply(queryCompositeKey);
        return switch (indexKeyCollection.size()) {
            case 0 -> 0;
            case 1 -> sizeSingleKey(queryCompositeKey, indexKeyCollection);
            default -> sizeManyKeys(queryCompositeKey, indexKeyCollection);
        };
    }

    private int sizeSingleKey(Object queryCompositeKey, KeyCollection_ indexKeyCollection) {
        var downstreamIndexer = downstreamIndexerMap.get(indexKeyCollection.iterator().next());
        return (downstreamIndexer == null) ? 0 : downstreamIndexer.size(queryCompositeKey);
    }

    private int sizeManyKeys(Object queryCompositeKey, KeyCollection_ indexKeyCollection) {
        var size = 0;
        var iterator = new DefaultIterator(queryCompositeKey, indexKeyCollection); // Avoid duplicating iteration logic
        while (iterator.hasNext()) {
            iterator.next();
            size++;
        }
        return size;
    }

    @Override
    public void forEach(Object queryCompositeKey, Consumer<T> tupleConsumer) {
        var indexKeyCollection = queryKeyUnpacker.apply(queryCompositeKey);
        switch (indexKeyCollection.size()) {
            case 0 -> {
                // Do nothing;
            }
            case 1 -> forEachSingleKey(queryCompositeKey, indexKeyCollection, tupleConsumer);
            default -> forEachManyKeys(queryCompositeKey, indexKeyCollection, tupleConsumer);
        }
    }

    private void forEachSingleKey(Object queryCompositeKey, KeyCollection_ indexKeyCollection, Consumer<T> tupleConsumer) {
        var indexKey = indexKeyCollection.iterator().next();
        var downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer != null) {
            downstreamIndexer.forEach(queryCompositeKey, tupleConsumer);
        }
    }

    private void forEachManyKeys(Object queryCompositeKey, KeyCollection_ indexKeyCollection, Consumer<T> tupleConsumer) {
        var iterator = new DefaultIterator(queryCompositeKey, indexKeyCollection); // Avoid duplicating iteration logic
        while (iterator.hasNext()) {
            var tuple = iterator.next();
            tupleConsumer.accept(tuple);
        }
    }

    @Override
    public Iterator<T> iterator(Object queryCompositeKey) {
        var indexKeyCollection = queryKeyUnpacker.apply(queryCompositeKey);
        return switch (indexKeyCollection.size()) {
            case 0 -> Collections.emptyIterator();
            case 1 -> iteratorSingleKey(queryCompositeKey, indexKeyCollection);
            default -> new DefaultIterator(queryCompositeKey, indexKeyCollection);
        };
    }

    private Iterator<T> iteratorSingleKey(Object queryCompositeKey, KeyCollection_ indexKeyCollection) {
        var indexKey = indexKeyCollection.iterator().next();
        var downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            return Collections.emptyIterator();
        }
        return downstreamIndexer.iterator(queryCompositeKey);
    }

    @Override
    public Iterator<T> randomIterator(Object queryCompositeKey, Random workingRandom) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> randomIterator(Object queryCompositeKey, Random workingRandom, Predicate<T> filter) {
        throw new UnsupportedOperationException();
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
        private final Set<T> distinctingSet;
        private @Nullable Iterator<T> downstreamIterator = null;
        private @Nullable T next = null;

        public DefaultIterator(Object queryCompositeKey, KeyCollection_ indexKeyCollection) {
            this.queryCompositeKey = queryCompositeKey;
            this.indexerIterator = indexKeyCollection.iterator();
            this.distinctingSet = new HashSet<>(indexKeyCollection.size() * 16);
        }

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            if (downstreamIterator != null && downstreamIterator.hasNext()) {
                var tuple = downstreamIterator.next();
                if (distinctingSet.add(tuple)) {
                    next = tuple;
                    return true;
                }
            }
            while (indexerIterator.hasNext()) {
                var indexKey = indexerIterator.next();
                var downstreamIndexer = downstreamIndexerMap.get(indexKey);
                if (downstreamIndexer == null) {
                    continue;
                }
                downstreamIterator = downstreamIndexer.iterator(queryCompositeKey);
                while (downstreamIterator.hasNext()) {
                    var tuple = downstreamIterator.next();
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
