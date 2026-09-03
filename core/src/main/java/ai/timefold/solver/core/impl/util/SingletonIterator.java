package ai.timefold.solver.core.impl.util;

import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class SingletonIterator<T extends @Nullable Object>
        implements ListIterator<T> {

    private final @Nullable T singleton;

    private boolean hasNext;
    private boolean hasPrevious;

    public SingletonIterator(@Nullable T singleton) {
        this.singleton = singleton;
        hasNext = true;
        hasPrevious = true;
    }

    public SingletonIterator(@Nullable T singleton, int index) {
        this.singleton = singleton;
        if (index < 0 || index > 1) {
            throw new IllegalArgumentException("The index (" + index + ") is invalid.");
        }
        hasNext = (index == 0);
        hasPrevious = !hasNext;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public @Nullable T next() {
        if (!hasNext) {
            throw new NoSuchElementException();
        }
        hasNext = false;
        hasPrevious = true;
        return singleton;
    }

    @Override
    public boolean hasPrevious() {
        return hasPrevious;
    }

    @Override
    public T previous() {
        if (!hasPrevious) {
            throw new NoSuchElementException();
        }
        hasNext = true;
        hasPrevious = false;
        return singleton;
    }

    @Override
    public int nextIndex() {
        return hasNext ? 0 : 1;
    }

    @Override
    public int previousIndex() {
        return hasPrevious ? 0 : -1;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(@Nullable T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(@Nullable T t) {
        throw new UnsupportedOperationException();
    }

}
