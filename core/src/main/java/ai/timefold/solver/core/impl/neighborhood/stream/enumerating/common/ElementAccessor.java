package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

public interface ElementAccessor<T> {

    T get(int index);

    int size();

}
