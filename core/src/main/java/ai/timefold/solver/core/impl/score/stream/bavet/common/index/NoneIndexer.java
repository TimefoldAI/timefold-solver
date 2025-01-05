package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.function.Consumer;

import ai.timefold.solver.core.impl.util.ElementAwareList;

final class NoneIndexer<T> implements Indexer<T> {

    private final ElementAwareList<T> tupleList = new ElementAwareList<>();

    @Override
    public ElementAwareList<T>.Entry put(IndexProperties indexProperties, T tuple) {
        return tupleList.add(tuple);
    }

    @Override
    public void remove(IndexProperties indexProperties, ElementAwareList<T>.Entry entry) {
        entry.remove();
    }

    @Override
    public int size(IndexProperties indexProperties) {
        return tupleList.size();
    }

    @Override
    public void forEach(IndexProperties indexProperties, Consumer<T> tupleConsumer) {
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
