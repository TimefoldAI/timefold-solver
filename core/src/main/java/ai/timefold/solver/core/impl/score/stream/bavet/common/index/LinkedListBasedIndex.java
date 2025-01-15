package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.function.Consumer;

import ai.timefold.solver.core.impl.util.ElementAwareList;
import ai.timefold.solver.core.impl.util.ElementAwareListEntry;

record LinkedListBasedIndex<T>(ElementAwareList<T> tupleList) implements Index<T> {

    static <T> LinkedListBasedIndex<T> build() {
        return new LinkedListBasedIndex<>(new ElementAwareList<>());
    }

    @Override
    public ElementAwareListEntry<T> put(Object indexKeys, T tuple) {
        return tupleList.add(tuple);
    }

    @Override
    public void remove(Object indexKeys, ElementAwareListEntry<T> entry) {
        entry.remove();
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
