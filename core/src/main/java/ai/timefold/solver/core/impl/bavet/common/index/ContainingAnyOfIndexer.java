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
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.solver.random.RandomUtils;
import ai.timefold.solver.core.impl.util.ListEntry;
import ai.timefold.solver.core.impl.util.Triple;

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
     * @param keyUnpacker determines if it immediately goes to a {@link LeafIndexer} or if it uses a {@link CompositeKey}.
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
        var children = new ArrayList<Triple<Key_, Indexer<T>, ListEntry<T>>>(indexKeyCollection.size());
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
        for (var i = 0; i < children.size(); i++) { // Avoids creating an iterator on the hot path.
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
    public RepeatingRandomIterator<T> randomIterator(Object queryCompositeKey, RandomGenerator workingRandom) {
        var indexKeyCollection = queryKeyUnpacker.apply(queryCompositeKey);
        if (indexKeyCollection.isEmpty()) {
            return RepeatingRandomIterator.empty();
        }
        return new RepeatingIterator(queryCompositeKey, indexKeyCollection, workingRandom);
    }

    @Override
    public UniqueRandomIterator<T> uniqueRandomIterator(Object queryCompositeKey, RandomGenerator workingRandom) {
        var indexKeyCollection = queryKeyUnpacker.apply(queryCompositeKey);
        if (indexKeyCollection.isEmpty()) {
            return UniqueRandomIterator.empty();
        }
        return new RandomIterator(queryCompositeKey, indexKeyCollection, workingRandom,
                downstreamIndexer -> downstreamIndexer.uniqueRandomIterator(queryCompositeKey, workingRandom));
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

    final class RandomIterator implements UniqueRandomIterator<T> {

        private final List<DownstreamIterator> downstreamIteratorList;
        private final RandomGenerator workingRandom;
        /**
         * How many elements are remaining for each {@link Iterator} in {@link #downstreamIteratorList}.
         * Used with {@link RandomUtils#sampleWithDistribution(RandomGenerator, int, int[])}
         * to fairly select an iterator
         * (so selecting an iterator with more elements is more likely).
         */
        private final int[] distribution;
        /**
         * Sum of all values in {@link #distribution}.
         * Used with {@link RandomUtils#sampleWithDistribution(RandomGenerator, int, int[])}
         * to fairly select an iterator
         * (so selecting an iterator with more elements is more likely).
         */
        private int distributionSum;
        private @Nullable T next = null;

        /**
         * The element returned by the last {@link #next()} call,
         * not yet recorded into {@link #removedSet}.
         * Recording is deferred to the start of the next {@link #hasNext()} call
         * (see {@link #flushUnrecorded()}),
         * so a caller which draws exactly once never allocates {@link #removedSet} at all.
         */
        private @Nullable T unrecorded;
        private @Nullable Set<T> removedSet;
        private @Nullable DownstreamIterator currentIterator = null;

        private class DownstreamIterator implements Iterator<T> {
            private final int index;
            private final Iterator<T> cachedDownstreamIterator;

            public DownstreamIterator(Object queryCompositeKey,
                    Function<Indexer<T>, Iterator<T>> downstreamIndexerIteratorFunction, int index, Key_ key) {
                this.index = index;
                var indexer = downstreamIndexerMap.get(key);
                if (indexer == null) {
                    // Mirrors RepeatingIterator: an unmatched query key is a dead bucket of weight 0.
                    this.cachedDownstreamIterator = Collections.emptyIterator();
                    return;
                }
                this.cachedDownstreamIterator = downstreamIndexerIteratorFunction.apply(indexer);
                distribution[index] = indexer.size(queryCompositeKey);
                distributionSum += distribution[index];
            }

            @Override
            public boolean hasNext() {
                return cachedDownstreamIterator.hasNext();
            }

            @Override
            public T next() {
                // The downstream already self-retires on every call,
                // permanently consuming one element of this bucket;
                // keep the weighted-sampling bookkeeping in sync here,
                // since there is no separate remove() step to do it anymore.
                var result = cachedDownstreamIterator.next();
                distribution[index]--;
                distributionSum--;
                return result;
            }

            // No remove() override: the downstream already retires on its own next().
        }

        public RandomIterator(Object queryCompositeKey, KeyCollection_ indexKeyCollection, RandomGenerator workingRandom,
                Function<Indexer<T>, Iterator<T>> downstreamIndexerIteratorFunction) {
            this.downstreamIteratorList = new ArrayList<>(indexKeyCollection.size());
            this.workingRandom = workingRandom;
            this.distribution = new int[indexKeyCollection.size()];
            var index = 0;
            for (var indexKey : indexKeyCollection) {
                this.downstreamIteratorList
                        .add(new DownstreamIterator(queryCompositeKey, downstreamIndexerIteratorFunction, index, indexKey));
                index++;
            }
        }

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            flushUnrecorded();
            if (currentIterator != null) {
                while (currentIterator.hasNext()) {
                    var candidate = currentIterator.next();
                    if (removedSet == null || !removedSet.contains(candidate)) {
                        next = candidate;
                        return true;
                    }
                    // A duplicate of a value already returned from a different bucket;
                    // already retired from this bucket by the currentIterator.next() call above.
                    // We do not remove the current iterator from the list
                    // if the current iterator has no more elements,
                    // since then we would need to resize the distribution array.
                    // The current iterator will never be picked if it has no more elements,
                    // since it would have a weight of 0 in the sample.
                }
            }
            while (distributionSum > 0) {
                var selectedIndex = RandomUtils.sampleWithDistribution(workingRandom, distributionSum, distribution);
                currentIterator = downstreamIteratorList.get(selectedIndex);
                if (!currentIterator.hasNext()) {
                    // Weight should never overstate a bucket's actual contents, but if it ever does,
                    // zero the weight instead of just skipping, so the loop cannot spin forever.
                    distributionSum -= distribution[selectedIndex];
                    distribution[selectedIndex] = 0;
                    continue;
                }

                var candidate = currentIterator.next();
                if (removedSet == null || !removedSet.contains(candidate)) {
                    next = candidate;
                    return true;
                }
                // Same as above: a cross-bucket duplicate, already retired; keep searching.
            }
            return false;
        }

        /**
         * Records the element returned by the previous {@link #next()} call into {@link #removedSet},
         * now that a further draw was actually requested.
         * Called at the start of every {@link #hasNext()}.
         */
        private void flushUnrecorded() {
            if (unrecorded != null) {
                if (removedSet == null) {
                    removedSet = new HashSet<>();
                }
                removedSet.add(unrecorded);
                unrecorded = null;
            }
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var result = next;
            next = null;
            unrecorded = result;
            return result;
        }

        // No remove() override: this iterator already guarantees uniqueness by itself;
        // UniqueRandomIterator's default remove() already throws.
    }

    /**
     * Unlike {@link RandomIterator}, this class never ends and never needs to track duplicates:
     * every {@link #next()} independently picks a key by its downstream size, with replacement,
     * so there is no shrinking distribution and no removed set to maintain.
     */
    final class RepeatingIterator implements RepeatingRandomIterator<T> {

        private final List<Iterator<T>> downstreamIteratorList;
        private final int[] distribution;
        private final int distributionSum;
        private final RandomGenerator workingRandom;

        RepeatingIterator(Object queryCompositeKey, KeyCollection_ indexKeyCollection, RandomGenerator workingRandom) {
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
