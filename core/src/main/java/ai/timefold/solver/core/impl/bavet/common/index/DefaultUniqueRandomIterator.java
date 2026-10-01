package ai.timefold.solver.core.impl.bavet.common.index;

import org.jspecify.annotations.NullMarked;

/**
 * Wraps a {@link RetiringRandomIterator}
 * and calls {@link RetiringRandomIterator#retire()} right after every {@link RetiringRandomIterator#next()},
 * so that the client of this iterator does not need to call {@code retire()} itself
 * to get unique elements.
 *
 * @param <T>
 */
@NullMarked
final class DefaultUniqueRandomIterator<T> implements UniqueRandomIterator<T> {

    private final RetiringRandomIterator<T> delegate;

    DefaultUniqueRandomIterator(RetiringRandomIterator<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public T next() {
        var result = delegate.next();
        delegate.retire();
        return result;
    }

}
