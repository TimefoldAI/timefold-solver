package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.function.Consumer;

import ai.timefold.solver.core.impl.util.ElementAwareLinkedList;
import ai.timefold.solver.core.impl.util.ListEntry;

import org.jspecify.annotations.NullMarked;

/**
 * Super-fast, but doesn't support random access.
 *
 * @param <T>
 */
@NullMarked
public final class LinkedListIndexerBackend<T> implements IndexerBackend<T> {

    private final ElementAwareLinkedList<T> tupleList = new ElementAwareLinkedList<>();

    @Override
    public ListEntry<T> put(Object indexKeys, T tuple) {
        return tupleList.add(tuple);
    }

    @Override
    public void remove(Object indexKeys, ListEntry<T> entry) {
        tupleList.remove((ElementAwareLinkedList.Entry<T>) entry);
    }

    @Override
    public int size(Object indexKeys) {
        return tupleList.size();
    }

    @Override
    public void forEach(Object indexKeys, Consumer<T> tupleConsumer) {
        tupleList.forEach(tupleConsumer);
    }

    @Override
    public boolean isEmpty() {
        return tupleList.size() == 0;
    }

    @Override
    public String toString() {
        return "size = " + tupleList.size();
    }

}
