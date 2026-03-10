package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.util.CompositeListEntry;
import ai.timefold.solver.core.impl.util.ListEntry;
import ai.timefold.solver.core.impl.util.Pair;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * As defined by {@link Joiners#containingAnyOf(Function, Function)}
 */
@NullMarked
final class ContainingAnyOfIndexer<T, Key_, KeyCollection_ extends SequencedCollection<Key_>> implements Indexer<T> {

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
        if (downstreamIndexerMap.isEmpty()) {
            return 0;
        }
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
        if (downstreamIndexerMap.isEmpty()) {
            return;
        }
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
        if (downstreamIndexerMap.isEmpty()) {
            return Collections.emptyIterator();
        }
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
    public Iterator<T> randomIterator(Object queryCompositeKey, RandomGenerator workingRandom) {
        return randomIterator(queryCompositeKey, workingRandom, null);
    }

    @Override
    public Iterator<T> randomIterator(Object queryCompositeKey, RandomGenerator workingRandom, @Nullable Predicate<T> filter) {
        var indexKeyCollection = queryKeyUnpacker.apply(queryCompositeKey);
        if (indexKeyCollection.isEmpty()) {
            return Collections.emptyIterator();
        }
        if (filter == null) {
            return new RandomIterator(indexKeyCollection, workingRandom,
                    downstreamIndexer -> downstreamIndexer.randomIterator(queryCompositeKey, workingRandom));
        } else {
            return new RandomIterator(indexKeyCollection, workingRandom,
                    downstreamIndexer -> downstreamIndexer.randomIterator(queryCompositeKey, workingRandom, filter));
        }
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
            if (downstreamIterator != null) {
                while (downstreamIterator.hasNext()) {
                    var tuple = downstreamIterator.next();
                    if (distinctingSet.add(tuple)) {
                        next = tuple;
                        return true;
                    }
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

    private final class RandomIterator implements Iterator<T> {

        private final List<DownstreamIteratorSupplier> downstreamIteratorSupplierList;
        private final RandomGenerator workingRandom;
        private final Function<Indexer<T>, Iterator<T>> downstreamIndexerIteratorFunction;
        private @Nullable Set<T> removedSet;
        private @Nullable T next = null;
        private @Nullable T current = null;
        private @Nullable DownstreamIteratorSupplier currentIteratorSupplier = null;
        private @Nullable Iterator<T> currentIterator = null;

        private class DownstreamIteratorSupplier {
            private final Key_ key;
            @Nullable
            private Iterator<T> cachedDownstreamIterator;

            public DownstreamIteratorSupplier(Key_ key) {
                this.key = key;
                this.cachedDownstreamIterator = null;
            }

            Iterator<T> iterator() {
                if (cachedDownstreamIterator != null) {
                    return cachedDownstreamIterator;
                }
                cachedDownstreamIterator = downstreamIndexerIteratorFunction.apply(downstreamIndexerMap.get(key));
                return cachedDownstreamIterator;
            }
        }

        public RandomIterator(KeyCollection_ indexKeyCollection, RandomGenerator workingRandom,
                Function<Indexer<T>, Iterator<T>> downstreamIndexerIteratorFunction) {
            this.downstreamIteratorSupplierList = new ArrayList<>(indexKeyCollection.size());
            this.workingRandom = workingRandom;
            this.downstreamIndexerIteratorFunction = downstreamIndexerIteratorFunction;
            for (var indexKey : indexKeyCollection) {
                this.downstreamIteratorSupplierList.add(new DownstreamIteratorSupplier(indexKey));
            }
        }

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            if (currentIterator != null) {
                while (currentIterator.hasNext()) {
                    next = currentIterator.next();
                    if (removedSet == null || !removedSet.contains(next)) {
                        return true;
                    } else {
                        currentIterator.remove();
                    }
                }
                downstreamIteratorSupplierList.remove(currentIteratorSupplier);
            }
            while (!downstreamIteratorSupplierList.isEmpty()) {
                var remainingIteratorCount = downstreamIteratorSupplierList.size();
                var selectedIndex = workingRandom.nextInt(remainingIteratorCount);
                currentIteratorSupplier = downstreamIteratorSupplierList.get(selectedIndex);
                currentIterator = currentIteratorSupplier.iterator();
                if (!currentIterator.hasNext()) {
                    downstreamIteratorSupplierList.remove(selectedIndex);
                    continue;
                }

                next = currentIterator.next();
                if (removedSet == null || !removedSet.contains(next)) {
                    return true;
                } else {
                    currentIterator.remove();
                    if (!currentIterator.hasNext()) {
                        downstreamIteratorSupplierList.remove(selectedIndex);
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
            current = next;
            next = null;
            return current;
        }

        @Override
        public void remove() {
            // Note: since we have multiple downstream iterators, and they may have
            // duplicates, we need to keep track of removed elements ourselves
            if (current == null) {
                throw new IllegalStateException("next() must be called before remove().");
            }
            if (removedSet == null) {
                removedSet = new HashSet<>();
            }
            removedSet.add(current);
            currentIterator.remove();
            if (!currentIterator.hasNext()) {
                downstreamIteratorSupplierList.remove(currentIteratorSupplier);
                currentIteratorSupplier = null;
                currentIterator = null;
            }
            current = null;
        }
    }

}
