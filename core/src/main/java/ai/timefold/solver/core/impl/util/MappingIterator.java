package ai.timefold.solver.core.impl.util;

import java.util.Iterator;
import java.util.function.Function;

public record MappingIterator<T, R>(Iterator<T> source, Function<T, R> mapper)
        implements
            Iterator<R> {

    @Override
    public boolean hasNext() {
        return source.hasNext();
    }

    @Override
    public R next() {
        return mapper.apply(source.next());
    }

}
