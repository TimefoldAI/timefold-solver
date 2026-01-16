package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

public record ListBasedElementAccessor<T>(ElementAwareArrayList<T> list)
        implements
            ElementAccessor<T> {

    @Override
    public T get(int index) {
        return list.get(index).element();
    }

    @Override
    public int size() {
        return list.size();
    }

}
