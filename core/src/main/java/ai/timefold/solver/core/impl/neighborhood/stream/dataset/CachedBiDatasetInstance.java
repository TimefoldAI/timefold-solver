package ai.timefold.solver.core.impl.neighborhood.stream.dataset;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
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
        return new TupleBiIterator<>(delegate.iterator(), false);
    }

    @Override
    public BiIterator<A, B> randomIterator(RandomGenerator random) {
        return new TupleBiIterator<>(delegate.randomIterator(random), true);
    }

    @Override
    public int size(@Nullable A a) {
        // ponytail: O(n) scan over the whole dataset; index by A if profiling demands it.
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
        return new FilteringFactIterator<>(delegate.iterator(), a, false);
    }

    @Override
    public Iterator<@Nullable B> randomIterator(@Nullable A a, RandomGenerator random) {
        return new FilteringFactIterator<>(delegate.randomIterator(random), a, true);
    }

    private static final class TupleBiIterator<A, B> implements BiIterator<A, B> {

        private final Iterator<BiTuple<A, B>> tupleIterator;
        private final boolean removeAfterNext;
        private @Nullable BiTuple<A, B> current;

        private TupleBiIterator(Iterator<BiTuple<A, B>> tupleIterator, boolean removeAfterNext) {
            this.tupleIterator = tupleIterator;
            this.removeAfterNext = removeAfterNext;
        }

        @Override
        public boolean hasNext() {
            return tupleIterator.hasNext();
        }

        @Override
        public void next() {
            current = tupleIterator.next();
            if (removeAfterNext) {
                tupleIterator.remove();
            }
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

    private static final class FilteringFactIterator<A, B> implements Iterator<@Nullable B> {

        private final Iterator<BiTuple<A, B>> tupleIterator;
        private final @Nullable A a;
        private final boolean removeAfterNext;
        private @Nullable BiTuple<A, B> nextTuple;

        private FilteringFactIterator(Iterator<BiTuple<A, B>> tupleIterator, @Nullable A a, boolean removeAfterNext) {
            this.tupleIterator = tupleIterator;
            this.a = a;
            this.removeAfterNext = removeAfterNext;
        }

        @Override
        public boolean hasNext() {
            while (nextTuple == null && tupleIterator.hasNext()) {
                var candidate = tupleIterator.next();
                if (Objects.equals(candidate.getA(), a)) {
                    nextTuple = candidate;
                } else if (removeAfterNext) {
                    tupleIterator.remove();
                }
            }
            return nextTuple != null;
        }

        @Override
        public @Nullable B next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var result = Objects.requireNonNull(nextTuple).getB();
            nextTuple = null;
            if (removeAfterNext) {
                tupleIterator.remove();
            }
            return result;
        }

    }

}
