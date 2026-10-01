package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.score.stream.Joiners;
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
        return multiIndexerRepeatingIterator(queryCompositeKey, indexKeyCollection, workingRandom);
    }

    /**
     * Every {@code next()} independently picks a key by its downstream size, with replacement,
     * so this never ends: there is no shrinking distribution and no removed set to maintain.
     */
    private RepeatingRandomIterator<T> multiIndexerRepeatingIterator(Object queryCompositeKey,
            SequencedCollection<Key_> indexKeyCollection, RandomGenerator workingRandom) {
        return ContainingAnyOfIndexer.multiIndexerRepeatingIterator(queryCompositeKey, downstreamIndexerMap, indexKeyCollection,
                workingRandom);
    }

    @Override
    public UniqueRandomIterator<T> uniqueRandomIterator(Object queryCompositeKey, RandomGenerator workingRandom) {
        var indexKeyCollection = unpackDistinctQueryKeys(queryCompositeKey);
        return switch (indexKeyCollection.size()) {
            case 0 -> UniqueRandomIterator.empty();
            // Single matching key: no bucket weighting needed,
            // and this keeps random consumption byte-identical to before for the common single-key case.
            case 1 -> uniqueRandomIteratorSingleKey(indexKeyCollection, queryCompositeKey, workingRandom);
            default -> uniqueRandomIteratorManyKeys(indexKeyCollection, queryCompositeKey, workingRandom);
        };
    }

    private UniqueRandomIterator<T> uniqueRandomIteratorSingleKey(SequencedCollection<Key_> indexKeyCollection,
            Object queryCompositeKey, RandomGenerator workingRandom) {
        var downstreamIndexer = downstreamIndexerMap.get(indexKeyCollection.iterator().next());
        if (downstreamIndexer == null) {
            return UniqueRandomIterator.empty();
        }
        return downstreamIndexer.uniqueRandomIterator(queryCompositeKey, workingRandom);
    }

    /**
     * Buckets here are disjoint by construction
     * (one key per tuple, see {@link #modifyKeyUnpacker}),
     * so {@link MultiBucketUniqueRandomIterator} applies directly:
     * every matching, non-empty bucket is walked upfront to learn its size,
     * and weighted, uniform-without-replacement sampling takes care of the rest.
     */
    private UniqueRandomIterator<T> uniqueRandomIteratorManyKeys(SequencedCollection<Key_> indexKeyCollection,
            Object queryCompositeKey, RandomGenerator workingRandom) {
        var bucketIteratorList = new ArrayList<UniqueRandomIterator<T>>(indexKeyCollection.size());
        // Upper-bounded by indexKeyCollection.size(); a trailing 0 in distribution is never sampled
        // (sampleWithDistribution() only ever walks its non-zero prefix),
        // so no trim to the real count is needed.
        var distribution = new int[indexKeyCollection.size()];
        var bucketCount = 0;
        var distributionSum = 0;
        for (var indexKey : indexKeyCollection) {
            var downstreamIndexer = downstreamIndexerMap.get(indexKey);
            if (downstreamIndexer == null) {
                continue;
            }
            var size = downstreamIndexer.size(queryCompositeKey);
            if (size == 0) {
                continue;
            }
            distribution[bucketCount++] = size;
            distributionSum += size;
            bucketIteratorList.add(downstreamIndexer.uniqueRandomIterator(queryCompositeKey, workingRandom));
        }
        if (distributionSum == 0) {
            return UniqueRandomIterator.empty();
        }
        return new MultiBucketUniqueRandomIterator<>(bucketIteratorList, distribution, distributionSum, workingRandom);
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
        @Nullable
        private Iterator<T> downstreamIterator = null;
        private @Nullable T next = null;

        public DefaultIterator(Object queryCompositeKey, SequencedCollection<Key_> indexKeyCollection) {
            this.queryCompositeKey = queryCompositeKey;
            this.indexerIterator = indexKeyCollection.iterator();
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
                var downstreamIndexer = downstreamIndexerMap.get(indexKey);
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
