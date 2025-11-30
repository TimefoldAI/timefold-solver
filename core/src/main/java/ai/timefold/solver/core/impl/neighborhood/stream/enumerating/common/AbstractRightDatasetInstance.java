package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.bavet.common.index.Indexer;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractLeftDatasetInstance.UnwrappingIterator;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbstractRightDatasetInstance<Solution_, Right_>
        extends AbstractDatasetInstance<Solution_, UniTuple<Right_>> {

    private final IndexerFactory.KeysExtractor<UniTuple<Right_>> compositeKeyExtractor;
    private final int compositeKeyStoreIndex;
    private final Indexer<UniTuple<Right_>> indexer;

    protected AbstractRightDatasetInstance(AbstractDataset<Solution_> parent,
            IndexerFactory.KeysExtractor<UniTuple<Right_>> compositeKeyExtractor, int compositeKeyStoreIndex,
            int rightMostPositionStoreIndex, Indexer<UniTuple<Right_>> indexer) {
        super(parent, rightMostPositionStoreIndex);
        this.compositeKeyExtractor = compositeKeyExtractor;
        this.compositeKeyStoreIndex = compositeKeyStoreIndex;
        this.indexer = indexer;
    }

    @Override
    public void insert(UniTuple<Right_> tuple) {
        var compositeKey = compositeKeyExtractor.apply(tuple);
        tuple.setStore(entryStoreIndex, indexer.put(compositeKey, tuple));
        tuple.setStore(compositeKeyStoreIndex, compositeKey);
    }

    @Override
    public void update(UniTuple<Right_> tuple) {
        var oldCompositeKey = tuple.getStore(compositeKeyStoreIndex);
        var newCompositeKey = compositeKeyExtractor.apply(tuple);
        if (!Objects.equals(oldCompositeKey, newCompositeKey)) {
            indexer.remove(oldCompositeKey, tuple.getStore(entryStoreIndex));
            tuple.setStore(entryStoreIndex, indexer.put(newCompositeKey, tuple));
            tuple.setStore(compositeKeyStoreIndex, newCompositeKey);
        }
    }

    @Override
    public void retract(UniTuple<Right_> tuple) {
        indexer.remove(tuple.removeStore(compositeKeyStoreIndex), tuple.removeStore(entryStoreIndex));
    }

    public Iterator<UniTuple<Right_>> iterator(Object compositeKey) {
        var list = indexer.asList(compositeKey);
        return new UnwrappingIterator<>(list.iterator());
    }

    public DefaultUniqueRandomSequence<UniTuple<Right_>> buildRandomSequence(Object compositeKey) {
        return new DefaultUniqueRandomSequence<>(indexer.asList(compositeKey));
    }

    public FilteredUniqueRandomSequence<UniTuple<Right_>> buildRandomSequence(Object compositeKey,
            Predicate<UniTuple<Right_>> predicate) {
        return new FilteredUniqueRandomSequence<>(indexer.asList(compositeKey), predicate);
    }

    public int size(Object compositeKey) {
        return indexer.size(compositeKey);
    }

}
