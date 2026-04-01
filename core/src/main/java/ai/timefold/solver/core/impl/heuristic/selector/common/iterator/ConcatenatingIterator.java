package ai.timefold.solver.core.impl.heuristic.selector.common.iterator;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.jspecify.annotations.Nullable;

public final class ConcatenatingIterator<T> implements Iterator<T> {

    private final Iterator<Iterator<? extends T>> iterators;
    private Iterator<? extends T> current;
    private boolean hasNext = false; // Exists to support null values.
    private @Nullable T next;

    @SafeVarargs
    public ConcatenatingIterator(Iterator<? extends T>... iterators) {
        this.iterators = Arrays.asList(iterators).iterator();
        this.current = Collections.emptyIterator();
    }

    @Override
    public boolean hasNext() {
        if (hasNext) {
            return true;
        }
        while (!current.hasNext()) {
            if (!iterators.hasNext()) {
                return false;
            }
            current = iterators.next();
        }
        hasNext = true;
        next = current.next();
        return true;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        var result = next;
        hasNext = false;
        next = null;
        return result;
    }

}
