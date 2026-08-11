package ai.timefold.solver.core.impl.neighborhood.stream.dataset;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.FilteringIterator;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractLeftDatasetInstance;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDatasetInstance;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiIterator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A {@link BiDatasetInstance} backed by a bavet-materialized, step-cached dataset instance.
 */
@NullMarked
public final class CachedBiDatasetInstance<Solution_, A, B> implements BiDatasetInstance<A, B> {

    private final AbstractLeftDatasetInstance<Solution_, BiTuple<A, B>> delegate;

    public CachedBiDatasetInstance(AbstractLeftDatasetInstance<Solution_, BiTuple<A, B>> delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public BiIterator<A, B> iterator() {
        return new TupleBiIterator<>(delegate.iterator());
    }

    @Override
    public BiIterator<A, B> randomIterator(RandomGenerator random) {
        return new TupleBiIterator<>(delegate.randomIterator(random));
    }

    @Override
    public BiIterator<A, B> uniqueRandomIterator(RandomGenerator random) {
        return new TupleBiIterator<>(delegate.uniqueRandomIterator(random));
    }

    @Override
    public int size(@Nullable A a) {
        var count = 0;
        var tupleIterator = delegate.iterator();
        while (tupleIterator.hasNext()) {
            if (Objects.equals(tupleIterator.next().getA(), a)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public Iterator<@Nullable B> iterator(@Nullable A a) {
        // The delegate is finite and deterministic, so there is nothing to bail out of.
        return new FactIterator<>(new FilteringIterator<>(delegate.iterator(), matchingA(a)));
    }

    @Override
    public Iterator<@Nullable B> randomIterator(@Nullable A a, RandomGenerator random) {
        // There is no index on A here (unlike a join's right side);
        // this is a linear scan over every pair, drawn with replacement.
        // Draws can never prove that no matching A exists,
        // so bail out after many consecutive rejections,
        // same multiple as FilteringEntitySelector.
        var bailOutSize = size(a) * 10L;
        return new FactIterator<>(new FilteringIterator<>(delegate.randomIterator(random), matchingA(a), bailOutSize));
    }

    @Override
    public Iterator<@Nullable B> uniqueRandomIterator(@Nullable A a, RandomGenerator random) {
        // The delegate already removes every element it returns, matching or not,
        // so it is finite regardless of how selective the filter is;
        // nothing to bail out of.
        return new FactIterator<>(new FilteringIterator<>(delegate.uniqueRandomIterator(random), matchingA(a)));
    }

    private static <A, B> Predicate<BiTuple<A, B>> matchingA(@Nullable A a) {
        return candidate -> Objects.equals(candidate.getA(), a);
    }

    private static final class TupleBiIterator<A, B> implements BiIterator<A, B> {

        private final Iterator<BiTuple<A, B>> tupleIterator;
        private @Nullable BiTuple<A, B> current;

        private TupleBiIterator(Iterator<BiTuple<A, B>> tupleIterator) {
            this.tupleIterator = tupleIterator;
        }

        @Override
        public boolean hasNext() {
            return tupleIterator.hasNext();
        }

        @Override
        public void next() {
            current = tupleIterator.next();
        }

        @Override
        public @Nullable A getA() {
            return Objects.requireNonNull(current).getA();
        }

        @Override
        public @Nullable B getB() {
            return Objects.requireNonNull(current).getB();
        }

    }

    private record FactIterator<A, B>(Iterator<BiTuple<A, B>> tupleIterator)
            implements
                Iterator<@Nullable B> {

        @Override
        public boolean hasNext() {
            return tupleIterator.hasNext();
        }

        @Override
        public @Nullable B next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return tupleIterator.next().getB();
        }

        @Override
        public void forEachRemaining(Consumer<? super @Nullable B> action) {
            tupleIterator.forEachRemaining(tuple -> action.accept(tuple.getB()));
        }

    }

}
