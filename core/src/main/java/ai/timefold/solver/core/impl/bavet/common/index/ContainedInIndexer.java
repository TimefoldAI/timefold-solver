package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.solver.random.RandomUtils;
import ai.timefold.solver.core.impl.util.ListEntry;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * As defined by {@link Joiners#containedIn(Function, Function)}
 */
@NullMarked
final class ContainedInIndexer<T, Key_, KeyCollection_ extends SequencedCollection<Key_>> implements Indexer<T> {

    private final KeyUnpacker<Key_> modifyKeyUnpacker;
    private final KeyUnpacker<KeyCollection_> queryKeyUnpacker;
    private final Supplier<Indexer<T>> downstreamIndexerSupplier;
    /**
     * See {@link EqualIndexer} for explanation of the parameters.
     */
    private final Map<Key_, Indexer<T>> downstreamIndexerMap = new HashMap<>(16, 0.5f);

    /**
     * @param keyUnpacker determines if it immediately goes to a {@link LeafIndexer} or if it uses a {@link CompositeKey}.
     * @param downstreamIndexerSupplier the supplier of the downstream indexer
     */
    @SuppressWarnings("unchecked")
    public ContainedInIndexer(KeyUnpacker<Key_> keyUnpacker, Supplier<Indexer<T>> downstreamIndexerSupplier) {
        this.modifyKeyUnpacker = Objects.requireNonNull(keyUnpacker);
        this.queryKeyUnpacker = Objects.requireNonNull((KeyUnpacker<KeyCollection_>) keyUnpacker);
        this.downstreamIndexerSupplier = Objects.requireNonNull(downstreamIndexerSupplier);
    }

    @Override
    public ListEntry<T> put(Object modifyCompositeKey, T tuple) {
        var indexKey = modifyKeyUnpacker.apply(modifyCompositeKey);
        // Avoids computeIfAbsent in order to not create lambdas on the hot path.
        var downstreamIndexer = downstreamIndexerMap.get(indexKey);
        if (downstreamIndexer == null) {
            downstreamIndexer = downstreamIndexerSupplier.get();
            downstreamIndexerMap.put(indexKey, downstreamIndexer);
        }
        return downstreamIndexer.put(modifyCompositeKey, tuple);
    }

    @Override
    public void remove(Object modifyCompositeKey, ListEntry<T> entry) {
        var indexKey = modifyKeyUnpacker.apply(modifyCompositeKey);
        var downstreamIndexer = getDownstreamIndexer(modifyCompositeKey, indexKey, entry);
        downstreamIndexer.remove(modifyCompositeKey, entry);
        if (downstreamIndexer.isRemovable()) {
            downstreamIndexerMap.remove(indexKey);
        }
    }

    private Indexer<T> getDownstreamIndexer(Object compositeKey, Key_ indexerKey, ListEntry<T> entry) {
        var downstreamIndexer = downstreamIndexerMap.get(indexerKey);
        if (downstreamIndexer == null) {
            throw new IllegalStateException(
                    "Impossible state: the tuple (%s) with composite key (%s) doesn't exist in the indexer %s."
                            .formatted(entry, compositeKey, this));
        }
        return downstreamIndexer;
    }

    /**
     * {@link #queryKeyUnpacker}'s output can contain a duplicated key;
     * nothing upstream guarantees uniqueness.
     * Buckets in this indexer are disjoint by construction
     * (one key per tuple, see {@link #modifyKeyUnpacker}),
     * so a duplicated query key would otherwise double-count in {@link #size(Object)}
     * and double-visit its bucket in {@link DefaultIterator},
     * returning the same tuples twice from a {@link #uniqueRandomIterator(Object, RandomGenerator)} drain
     * and breaking its documented "never returned again" contract.
     */
    private SequencedCollection<Key_> unpackDistinctQueryKeys(Object queryCompositeKey) {
        var indexKeyCollection = queryKeyUnpacker.apply(queryCompositeKey);
        if (indexKeyCollection.size() < 2 || indexKeyCollection instanceof Set<?>) {
            return indexKeyCollection; // Nothing can be duplicated; skip the allocation.
        }
        // LinkedHashSet, not Set.copyOf(): DefaultIterator's bucket walk depends on encounter order.
        return new LinkedHashSet<>(indexKeyCollection);
    }

    @Override
    public int size(Object queryCompositeKey) {
        if (downstreamIndexerMap.isEmpty()) {
            return 0;
        }
        var indexKeyCollection = unpackDistinctQueryKeys(queryCompositeKey);
        if (indexKeyCollection.isEmpty()) {
            return 0;
        }
        var size = 0;
        for (var indexKey : indexKeyCollection) {
            var downstreamIndexer = downstreamIndexerMap.get(indexKey);
            if (downstreamIndexer != null) {
                size += downstreamIndexer.size(queryCompositeKey);
            }
        }
        return size;
    }

    @Override
    public Iterator<T> iterator(Object queryCompositeKey) {
        var indexKeyCollection = unpackDistinctQueryKeys(queryCompositeKey);
        if (indexKeyCollection.isEmpty()) {
            return Collections.emptyIterator();
        }
        return new DefaultIterator(queryCompositeKey, indexKeyCollection);
    }

    @Override
    public RepeatingRandomIterator<T> randomIterator(Object queryCompositeKey, RandomGenerator workingRandom) {
        var indexKeyCollection = unpackDistinctQueryKeys(queryCompositeKey);
        if (indexKeyCollection.isEmpty()) {
            return RepeatingRandomIterator.empty();
        }
        return new RepeatingIterator(queryCompositeKey, indexKeyCollection, workingRandom);
    }

    @Override
    public UniqueRandomIterator<T> uniqueRandomIterator(Object queryCompositeKey, RandomGenerator workingRandom) {
        var indexKeyCollection = unpackDistinctQueryKeys(queryCompositeKey);
        if (indexKeyCollection.isEmpty()) {
            return UniqueRandomIterator.empty();
        }
        return new RandomIterator(indexKeyCollection,
                downstreamIndexer -> downstreamIndexer.uniqueRandomIterator(queryCompositeKey, workingRandom));
    }

    @Override
    public boolean isRemovable() {
        return downstreamIndexerMap.isEmpty();
    }

    @Override
    public String toString() {
        return "size = " + downstreamIndexerMap.size();
    }

    private class DefaultIterator implements Iterator<T> {

        private final Iterator<Key_> indexerIterator;
        private final Function<Indexer<T>, Iterator<T>> downstreamIteratorFunction;
        protected @Nullable Iterator<T> downstreamIterator = null;
        private @Nullable T next = null;

        public DefaultIterator(Object queryCompositeKey, SequencedCollection<Key_> indexKeyCollection) {
            this(indexKeyCollection,
                    downstreamIndexer -> downstreamIndexer.iterator(queryCompositeKey));
        }

        protected DefaultIterator(SequencedCollection<Key_> indexKeyCollection,
                Function<Indexer<T>, Iterator<T>> downstreamIteratorFunction) {
            this.indexerIterator = indexKeyCollection.iterator();
            this.downstreamIteratorFunction = downstreamIteratorFunction;
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
                var downstreamIndexer = downstreamIndexerMap.get(indexKey);
                if (downstreamIndexer == null) {
                    continue;
                }
                downstreamIterator = downstreamIteratorFunction.apply(downstreamIndexer);
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

    final class RandomIterator extends DefaultIterator implements UniqueRandomIterator<T> {

        public RandomIterator(SequencedCollection<Key_> indexKeyCollection,
                Function<Indexer<T>, Iterator<T>> downstreamIteratorFunction) {
            super(indexKeyCollection, downstreamIteratorFunction);
        }

        // No remove() override: the downstream is already a self-retiring UniqueRandomIterator,
        // so there is nothing left to forward; UniqueRandomIterator's default already throws.

    }

    /**
     * Unlike {@link RandomIterator}, this class never ends:
     * every {@link #next()} independently picks a key by its downstream size, with replacement,
     * so there is no shrinking distribution and no removed set to maintain.
     */
    final class RepeatingIterator implements RepeatingRandomIterator<T> {

        private final List<Iterator<T>> downstreamIteratorList;
        private final int[] distribution;
        private final int distributionSum;
        private final RandomGenerator workingRandom;

        RepeatingIterator(Object queryCompositeKey, SequencedCollection<Key_> indexKeyCollection,
                RandomGenerator workingRandom) {
            this.workingRandom = workingRandom;
            this.downstreamIteratorList = new ArrayList<>(indexKeyCollection.size());
            this.distribution = new int[indexKeyCollection.size()];
            var index = 0;
            var sum = 0;
            for (var indexKey : indexKeyCollection) {
                var downstreamIndexer = downstreamIndexerMap.get(indexKey);
                if (downstreamIndexer == null) {
                    downstreamIteratorList.add(Collections.emptyIterator());
                } else {
                    distribution[index] = downstreamIndexer.size(queryCompositeKey);
                    downstreamIteratorList.add(downstreamIndexer.randomIterator(queryCompositeKey, workingRandom));
                    sum += distribution[index];
                }
                index++;
            }
            this.distributionSum = sum;
        }

        @Override
        public boolean hasNext() {
            return distributionSum > 0;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var selectedIndex = RandomUtils.sampleWithDistribution(workingRandom, distributionSum, distribution);
            return downstreamIteratorList.get(selectedIndex).next();
        }

        // No remove()/forEachRemaining() overrides: RepeatingRandomIterator's defaults already throw.

    }

}
