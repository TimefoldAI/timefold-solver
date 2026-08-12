package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps an iterator with a filter, skipping elements which do not match.
 * <p>
 * If the wrapped iterator never ends,
 * a rejecting filter can make {@link #hasNext()} loop forever,
 * as no amount of skipped elements can prove that no matching element exists.
 * A non-negative {@code bailOutSize} avoids this:
 * after that many consecutive rejections,
 * {@link #hasNext()} gives up and returns false,
 * even though a matching element might still exist.
 * A negative {@code bailOutSize} disables the bail-out,
 * for use with a wrapped iterator which is already known to end by itself.
 * <p>
 * The bail-out counter ({@code attemptsBeforeBailOut}, below) is a local,
 * reset to {@code bailOutSize} on every {@link #hasNext()} call
 * (the cached-{@code hasNext} fast path aside).
 * A bail-out is therefore a per-call false negative, not proof of emptiness:
 * calling {@link #hasNext()} again gives the delegate a fresh, independent budget to find a match in.
 * {@code RetiringBiWalk} depends on this property.
 */
@NullMarked
public final class FilteringIterator<T> implements Iterator<T> {

    /**
     * Multiplied by the candidate population's size to size a bail-out budget,
     * wherever this iterator is built with one.
     */
    public static final long BAIL_OUT_SAFETY_MULTIPLIER = 10L;

    private static final Logger LOGGER = LoggerFactory.getLogger(FilteringIterator.class);

    private final Iterator<T> delegate;
    private final Predicate<T> filter;
    private final long bailOutSize;

    private boolean hasNext = false;
    private @Nullable T next;

    public FilteringIterator(Iterator<T> delegate, Predicate<T> filter) {
        this(delegate, filter, -1);
    }

    public FilteringIterator(Iterator<T> delegate, Predicate<T> filter, long bailOutSize) {
        this.delegate = Objects.requireNonNull(delegate);
        this.filter = Objects.requireNonNull(filter);
        this.bailOutSize = bailOutSize;
    }

    @Override
    public boolean hasNext() {
        if (hasNext) {
            return true;
        }
        var attemptsBeforeBailOut = bailOutSize; // Fresh, independent budget every call; see the class javadoc.
        while (delegate.hasNext()) {
            if (bailOutSize >= 0 && attemptsBeforeBailOut <= 0) {
                LOGGER.trace("Bailing out of filtering iterator ({}) after ({}) attempts to avoid an infinite loop.", this,
                        bailOutSize);
                return false;
            }
            var candidate = delegate.next();
            if (filter.test(candidate)) {
                hasNext = true;
                next = candidate;
                return true;
            }
            attemptsBeforeBailOut--;
        }
        return false;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        var result = Objects.requireNonNull(next);
        hasNext = false;
        next = null;
        return result;
    }

}
