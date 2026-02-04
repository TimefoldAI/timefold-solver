package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.Iterator;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.bavet.common.index.Indexer;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbstractRightDatasetInstance<Solution_, Right_>
        extends AbstractDatasetInstance<Solution_, UniTuple<Right_>> {

    private final IndexerFactory.KeysExtractor<UniTuple<Right_>> compositeKeyExtractor;
    private final int compositeKeyStoreIndex;
    private final Indexer<UniTuple<Right_>> indexer;

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
        var compositeKey = tuple.getStore(compositeKeyStoreIndex);
        if (compositeKey == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }

        indexer.remove(compositeKey, tuple.removeStore(entryStoreIndex));
    }

    public Iterator<UniTuple<Right_>> iterator(Object compositeKey) {
        return indexer.iterator(compositeKey);
    }

    public Iterator<UniTuple<Right_>> randomIterator(Object compositeKey, Random workingRandom) {
        return indexer.randomIterator(compositeKey, workingRandom);
    }

    public Iterator<UniTuple<Right_>> randomIterator(Object compositeKey, Random workingRandom,
            Predicate<UniTuple<Right_>> predicate) {
        return indexer.randomIterator(compositeKey, workingRandom, predicate);
    }

}
