package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.function.Consumer;

public final class NoneIndexer<T> implements Indexer<T> {

    private final IndexedSet<T> store;

    public NoneIndexer(ElementPositionTracker<T> elementPositionTracker) {
        this.store = new IndexedSet<>(elementPositionTracker);
    }

    @Override
    public void put(Object indexKeys, T element) {
        store.add(element);
    }

    @Override
    public void remove(Object indexKeys, T element) {
        store.remove(element);
    }

    @Override
    public int size(Object indexKeys) {
        return store.size();
    }

    @Override
    public void forEach(Object indexKeys, Consumer<T> tupleConsumer) {
        store.forEach(tupleConsumer);
    }

    @Override
    public boolean isEmpty() {
        return store.isEmpty();
    }

    @Override
    public String toString() {
        return "size = " + store.size();
    }

}
