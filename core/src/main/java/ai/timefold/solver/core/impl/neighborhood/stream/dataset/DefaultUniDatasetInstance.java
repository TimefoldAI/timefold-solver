package ai.timefold.solver.core.impl.neighborhood.stream.dataset;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractLeftDatasetInstance;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.UniDatasetInstance;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class DefaultUniDatasetInstance<Solution_, A> implements UniDatasetInstance<A> {

    private final AbstractLeftDatasetInstance<Solution_, UniTuple<A>> delegate;

    public DefaultUniDatasetInstance(AbstractLeftDatasetInstance<Solution_, UniTuple<A>> delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public Iterator<@Nullable A> iterator() {
        return new FactIterator<>(delegate.iterator(), false);
    }

    @Override
    public Iterator<@Nullable A> randomIterator(RandomGenerator random) {
        return new FactIterator<>(delegate.randomIterator(random), true);
    }

    private static final class FactIterator<A> implements Iterator<@Nullable A> {

        private final Iterator<UniTuple<A>> tupleIterator;
        private final boolean removeAfterNext;

        private FactIterator(Iterator<UniTuple<A>> tupleIterator, boolean removeAfterNext) {
            this.tupleIterator = tupleIterator;
            this.removeAfterNext = removeAfterNext;
        }

        @Override
        public boolean hasNext() {
            return tupleIterator.hasNext();
        }

        @Override
        public @Nullable A next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var result = tupleIterator.next().getA();
            if (removeAfterNext) {
                tupleIterator.remove();
            }
            return result;
        }

    }

}
