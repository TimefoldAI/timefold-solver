package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import ai.timefold.solver.core.impl.bavet.common.index.Indexer;

public record IndexerBasedElementAccessor<T>(Indexer<T> indexer, Object compositeKey)
        implements
            ElementAccessor<T> {

    @Override
    public T get(int index) {
        return indexer.get(compositeKey, index).element();
    }

    @Override
    public int size() {
        return indexer.size(compositeKey);
    }

}
