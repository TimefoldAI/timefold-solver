package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import ai.timefold.solver.core.impl.bavet.common.index.Indexer;

public record IndexerBasedElementAccessor<T>(Indexer<T> indexer, Object compositeKey)
        implements
            ElementAccessor<T> {

    @Override
    public T get(int index) {
        var entry = indexer.get(compositeKey, index);
        if (entry == null) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        }
        return entry.getElement();
    }

    @Override
    public int size() {
        return indexer.size(compositeKey);
    }

}
