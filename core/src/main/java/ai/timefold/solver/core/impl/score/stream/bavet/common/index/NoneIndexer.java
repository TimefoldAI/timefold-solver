package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.function.Consumer;

import ai.timefold.solver.core.impl.util.ElementAwareList;

final class NoneIndexer<T> implements Indexer<T> {

    private final ElementAwareList<T> tupleList = new ElementAwareList<>();

    @Override
    public ElementAwareList<T>.Entry put(Object indexProperties, T tuple) {
        return tupleList.add(tuple);
    }

    @Override
    public void remove(Object indexProperties, ElementAwareList<T>.Entry entry) {
        entry.remove();
    }

    @Override
    public int size(Object indexProperties) {
        return tupleList.size();
    }

    @Override
    public void forEach(Object indexProperties, Consumer<T> tupleConsumer) {
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
