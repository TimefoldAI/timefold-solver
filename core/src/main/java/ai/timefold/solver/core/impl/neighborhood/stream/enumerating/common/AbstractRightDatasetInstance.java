package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.bavet.common.index.Indexer;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.index.UniqueRandomIterator;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.FilteringIterator;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDatasetInstance;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbstractRightDatasetInstance<Solution_, Right_>
        extends AbstractDatasetInstance<Solution_, UniTuple<Right_>> {

    private final IndexerFactory.KeysExtractor<UniTuple<Right_>> compositeKeyExtractor;
    private final int compositeKeyStoreIndex;
    private final Indexer<UniTuple<Right_>> indexer;

    /**
     * The number of tuples currently live in this dataset, across every composite key.
     * Maintained incrementally in {@link #insert}/{@link #retract} so {@link #totalSize()} is O(1);
     * {@link Indexer} has no key-independent size query to recompute it from.
     */
    private int totalSize;

    protected AbstractRightDatasetInstance(AbstractDataset<Solution_> parent,
            IndexerFactory.KeysExtractor<UniTuple<Right_>> compositeKeyExtractor, int compositeKeyStoreIndex,
            int entryStoreIndex, Indexer<UniTuple<Right_>> indexer) {
        super(parent, entryStoreIndex);
        this.compositeKeyExtractor = compositeKeyExtractor;
        this.compositeKeyStoreIndex = compositeKeyStoreIndex;
        this.indexer = indexer;
    }

    @Override
    public void insert(UniTuple<Right_> tuple) {
        if (tuple.getStore(compositeKeyStoreIndex) != null) {
            throw new IllegalStateException(
                    "Impossible state: the input for the tuple (%s) was already added in the tupleStore."
                            .formatted(tuple));
        }

        var compositeKey = compositeKeyExtractor.apply(tuple);
        tuple.setStore(entryStoreIndex, indexer.put(compositeKey, tuple));
        tuple.setStore(compositeKeyStoreIndex, compositeKey);
        totalSize++;
    }

    @Override
    public void update(UniTuple<Right_> tuple) {
        var oldCompositeKey = tuple.getStore(compositeKeyStoreIndex);
        if (oldCompositeKey == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            insert(tuple);
            return;
        }

        var newCompositeKey = compositeKeyExtractor.apply(tuple);
        if (!Objects.equals(oldCompositeKey, newCompositeKey)) {
            indexer.remove(oldCompositeKey, tuple.getStore(entryStoreIndex));
            tuple.setStore(entryStoreIndex, indexer.put(newCompositeKey, tuple));
            tuple.setStore(compositeKeyStoreIndex, newCompositeKey);
        }
    }

    @Override
    public void retract(UniTuple<Right_> tuple) {
        var compositeKey = tuple.removeStore(compositeKeyStoreIndex);
        if (compositeKey == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s).
            // Never inserted (filtered out), so totalSize must not be decremented either.
            return;
        }

        indexer.remove(compositeKey, tuple.removeStore(entryStoreIndex));
        totalSize--;
    }

    /**
     * As defined by {@link UniDatasetInstance#size()},
     * only accepts a key for joins.
     */
    public int size(Object compositeKey) {
        return indexer.size(compositeKey);
    }

    /**
     * The number of tuples currently live in this dataset, across every composite key.
     * Used as the denominator that makes {@code BiRandomMoveIterator}'s per-pair probability uniform:
     * an indexed key's {@link #size(Object)} is always a subset of this total.
     */
    public int totalSize() {
        return totalSize;
    }

    /**
     * As defined by {@link UniDatasetInstance#iterator(RandomGenerator)},
     * only accepts a key for joins.
     */
    public Iterator<UniTuple<Right_>> randomIterator(Object compositeKey, RandomGenerator workingRandom) {
        return indexer.randomIterator(compositeKey, workingRandom);
    }

    /**
     * As defined by {@link UniDatasetInstance#exhaustiveIterator(RandomGenerator)},
     * only accepts a key for joins.
     */
    public UniqueRandomIterator<UniTuple<Right_>> uniqueRandomIterator(Object compositeKey, RandomGenerator workingRandom) {
        return indexer.uniqueRandomIterator(compositeKey, workingRandom);
    }

    /**
     * As defined by {@link #uniqueRandomIterator(Object, RandomGenerator)},
     * but only returning elements matching the given predicate.
     */
    public Iterator<UniTuple<Right_>> uniqueRandomIterator(Object compositeKey, RandomGenerator workingRandom,
            Predicate<UniTuple<Right_>> predicate) {
        return new FilteringIterator<>(indexer.uniqueRandomIterator(compositeKey, workingRandom), predicate);
    }

}
