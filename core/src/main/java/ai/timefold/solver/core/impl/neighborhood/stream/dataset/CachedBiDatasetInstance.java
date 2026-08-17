package ai.timefold.solver.core.impl.neighborhood.stream.dataset;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi.BiLeftDatasetInstance;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiDatasetInstance;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.BiIterator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A {@link BiDatasetInstance} backed by a bavet-materialized, step-cached dataset instance.
 */
@NullMarked
public final class CachedBiDatasetInstance<Solution_, A, B> implements BiDatasetInstance<A, B> {

    private final BiLeftDatasetInstance<Solution_, A, B> delegate;

    public CachedBiDatasetInstance(BiLeftDatasetInstance<Solution_, A, B> delegate) {
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
        return delegate.size(a);
    }

    @Override
    public Iterator<@Nullable B> iterator(@Nullable A a) {
        return new FactIterator<>(delegate.iterator(a));
    }

    @Override
    public Iterator<@Nullable B> randomIterator(@Nullable A a, RandomGenerator random) {
        return new FactIterator<>(delegate.randomIterator(a, random));
    }

    @Override
    public Iterator<@Nullable B> uniqueRandomIterator(@Nullable A a, RandomGenerator random) {
        return new FactIterator<>(delegate.uniqueRandomIterator(a, random));
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
        public @Nullable A a() {
            return Objects.requireNonNull(current).getA();
        }

        @Override
        public @Nullable B b() {
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
