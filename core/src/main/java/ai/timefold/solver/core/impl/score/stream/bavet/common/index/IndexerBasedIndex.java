package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.function.Consumer;

import ai.timefold.solver.core.impl.util.ElementAwareListEntry;

record IndexerBasedIndex<T>(Indexer<T> indexer) implements Index<T> {

    @Override
    public ElementAwareListEntry<T> put(Object indexKeys, T tuple) {
        return indexer.put(indexKeys, tuple);
    }

    @Override
    public void remove(Object indexKeys, ElementAwareListEntry<T> entry) {
        indexer.remove(indexKeys, entry);
    }

    @Override
    public int size(Object indexKeys) {
        return indexer.size(indexKeys);
    }

    @Override
    public void forEach(Object indexKeys, Consumer<T> tupleConsumer) {
        indexer.forEach(indexKeys, tupleConsumer);
    }

    @Override
    public boolean isEmpty() {
        return indexer.isEmpty();
    }

    @Override
    public String toString() {
        return indexer.toString();
    }

}
