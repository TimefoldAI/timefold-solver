package ai.timefold.solver.core.impl.heuristic.selector.common.iterator;

import java.util.Iterator;
import java.util.function.Function;

public final class MappingIterator<T, R> implements Iterator<R> {

    private final Iterator<T> source;
    private final Function<T, R> mapper;

    public MappingIterator(Iterator<T> source, Function<T, R> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public boolean hasNext() {
        return source.hasNext();
    }

    @Override
    public R next() {
        return mapper.apply(source.next());
    }

}
