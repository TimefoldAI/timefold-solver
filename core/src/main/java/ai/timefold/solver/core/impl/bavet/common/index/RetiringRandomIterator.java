package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Exists to support random selection with replacement over a shrinking live pool.
 * It accepts a list of items on input, and does not copy or modify it.
 * <p>
 * Unlike {@link UniqueRandomIterator},
 * this iterator never ends by itself (unless the underlying list is empty)
 * and may return the same element more than once.
 * Unlike {@link RepeatingRandomIterator},
 * an element may be permanently dropped from the pool by calling {@link #retire()}
 * right after the {@link #next()} call which returned it;
 * a retired element is never returned again.
 * {@link #retire()} does not touch the underlying list or the index it came from;
 * it only affects what this iterator itself may return.
 * {@link #remove()} is not supported; use {@link #retire()} instead.
 * {@link #forEachRemaining(Consumer)} is not supported, as this iterator may never end.
 *
 * @param <T>
 */
@NullMarked
public sealed interface RetiringRandomIterator<T extends @Nullable Object>
        extends Iterator<T>
        permits DefaultRetiringRandomIterator, RetiringRandomIterator.MappingRetiringRandomIterator {

    static <T extends @Nullable Object> RetiringRandomIterator<T> of(ElementAwareArrayList<T> list, RandomGenerator random) {
        return new DefaultRetiringRandomIterator<>(list, random);
    }

    /**
     * Adapts an iterator of one type to another, without changing which element retirement targets:
     * {@link #retire()} on the result still retires whatever the delegate itself last handed out.
     */
    static <S extends @Nullable Object, T extends @Nullable Object> RetiringRandomIterator<T> mapping(
            RetiringRandomIterator<S> delegate, Function<S, T> mapper) {
        return new MappingRetiringRandomIterator<>(delegate, mapper);
    }

    /**
     * Returns whether there are any elements left to pick from.
     * Only turns {@code false} once every element has been retired,
     * or the source was empty to begin with.
     */
    @Override
    boolean hasNext();

    /**
     * Picks a random element from the pool of elements not yet retired.
     * The same element may be returned again by a later call,
     * unless {@link #retire()} is called for it.
     */
    @Override
    T next();

    /**
     * Permanently drops the element returned by the last {@link #next()} call from this iterator's pool;
     * it will never be returned again by this iterator.
     * Does not modify the underlying list or index.
     *
     * @throws IllegalStateException if {@link #next()} has not been called yet,
     *         or this method was already called since the last {@link #next()} call
     */
    void retire();

    /**
     * Not supported: this iterator never ends by itself,
     * so {@code forEachRemaining()} cannot terminate.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    default void forEachRemaining(Consumer<? super T> action) {
        throw new UnsupportedOperationException("""
                The retiring random iterator (%s) does not end by itself, so forEachRemaining() cannot terminate.
                Maybe use hasNext() and next() with your own stop condition instead."""
                .formatted(this));
    }

    /**
     * Adapts a {@link RetiringRandomIterator} of one type to another,
     * by mapping each element through a function,
     * without changing which element is retired:
     * {@link #retire()} still retires whatever the delegate last handed out,
     * keyed by the delegate's own identity, not by the mapped value.
     */
    record MappingRetiringRandomIterator<S extends @Nullable Object, T extends @Nullable Object>(
            RetiringRandomIterator<S> delegate,
            Function<S, T> mapper) implements RetiringRandomIterator<T> {

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public T next() {
            return mapper.apply(delegate.next());
        }

        @Override
        public void retire() {
            delegate.retire();
        }

    }

}
