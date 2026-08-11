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
import ai.timefold.solver.core.impl.util.ElementAwareArrayList;
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
        if (downstreamIndexerMap.isEmpty()) {
            return UniqueRandomIterator.empty();
        }
        var indexKeyCollection = queryKeyUnpacker.apply(queryCompositeKey);
        return switch (indexKeyCollection.size()) {
            case 0 -> UniqueRandomIterator.empty();
            case 1 -> uniqueRandomIteratorSingleKey(queryCompositeKey, indexKeyCollection, workingRandom);
            default -> uniqueRandomIteratorManyKeys(queryCompositeKey, indexKeyCollection, workingRandom);
        };
    }

    private UniqueRandomIterator<T> uniqueRandomIteratorSingleKey(Object queryCompositeKey, KeyCollection_ indexKeyCollection,
            RandomGenerator workingRandom) {
        var downstreamIndexer = downstreamIndexerMap.get(indexKeyCollection.iterator().next());
        if (downstreamIndexer == null) {
            return UniqueRandomIterator.empty();
        }
        return downstreamIndexer.uniqueRandomIterator(queryCompositeKey, workingRandom);
    }

    /**
     * Unlike {@link ComparisonIndexer} or {@code ContainedInIndexer},
     * whose buckets for different query keys are disjoint,
     * a single tuple here can be reachable under more than one query key
     * (that is the whole point of {@link Joiners#containingAnyOf(Function, Function)}).
     * A bucket-then-uniform-inside-bucket sampler cannot be made uniform over the distinct tuples when buckets overlap:
     * for buckets {@code X = {t1, t2}}, {@code Y = {t2, t3}},
     * whatever weight is put on each bucket,
     * {@code P(t1) + P(t3) = 1/2} always,
     * since each bucket contributes half of its own probability mass to its non-shared element;
     * but uniformity over the 3 distinct tuples needs {@code P(t1) + P(t3) = 2/3}.
     * No weight assignment closes that gap.
     * <p>
     * So instead of weighting buckets,
     * this drains the already tuple-deduplicating {@link DefaultIterator} into a plain list
     * and delegates to {@link UniqueRandomIterator#of(ElementAwareArrayList, RandomGenerator)},
     * which is exact over any list.
     */
    private UniqueRandomIterator<T> uniqueRandomIteratorManyKeys(Object queryCompositeKey, KeyCollection_ indexKeyCollection,
            RandomGenerator workingRandom) {
        var iterator = new DefaultIterator(queryCompositeKey, indexKeyCollection);
        var distinctTupleList = new ElementAwareArrayList<T>();
        while (iterator.hasNext()) {
            distinctTupleList.add(iterator.next());
        }
        return UniqueRandomIterator.of(distinctTupleList, workingRandom);
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

    /**
     * Picks a key by its downstream bucket size,
     * then a random element from within that bucket,
     * with replacement;
     * never ends and never needs to track duplicates,
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
