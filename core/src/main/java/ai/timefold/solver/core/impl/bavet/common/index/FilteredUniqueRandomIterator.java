package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Unlike {@link DefaultUniqueRandomIterator}, this class only returns elements that match the given filter.
 * Because we can't predict how many elements will be filtered out,
 * and because we don't want to pre-filter the entire list,
 * this class may need to try multiple times to find a matching element.
 *
 * @param <T>
 */
@NullMarked
public final class FilteredUniqueRandomIterator<T> implements UniqueRandomIterator<T> {

    private final Predicate<T> filter;
    private final DefaultUniqueRandomIterator<T> delegate;

    private boolean hasNext = false;
    private @Nullable T next = null;

    FilteredUniqueRandomIterator(ElementAwareArrayList<T> source, Random workingRandom, Predicate<T> filter) {
        this.filter = Objects.requireNonNull(filter);
        this.delegate = new DefaultUniqueRandomIterator<>(source, workingRandom);
    }

    @Override
    public boolean hasNext() {
        if (hasNext) {
            return true;
        }
        while (delegate.hasNext()) {
            var retrieved = delegate.next();
            if (filter.test(retrieved)) {
                next = retrieved;
                hasNext = true;
                return true;
            } else {
                delegate.remove();
            }
        }
        hasNext = false;
        next = null;
        return false;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements available that match the filter.");
        }
        var result = next;
        hasNext = false;
        next = null;
        return result;
    }

    @Override
    public void remove() {
        delegate.remove();
    }

}
