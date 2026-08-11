package ai.timefold.solver.core.impl.neighborhood.stream.dataset;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
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
        return new FactIterator<>(delegate.iterator());
    }

    @Override
    public Iterator<@Nullable A> randomIterator(RandomGenerator random) {
        return new FactIterator<>(delegate.randomIterator(random));
    }

    @Override
    public Iterator<@Nullable A> uniqueRandomIterator(RandomGenerator random) {
        return new FactIterator<>(delegate.uniqueRandomIterator(random));
    }

    /**
     * Maps a tuple iterator to its fact.
     * Uniqueness or endlessness are entirely a property of the wrapped {@code tupleIterator};
     * this class neither removes nor limits anything itself.
     */
    private record FactIterator<A>(Iterator<UniTuple<A>> tupleIterator)
            implements
                Iterator<@Nullable A> {

        @Override
        public boolean hasNext() {
            return tupleIterator.hasNext();
        }

        @Override
        public @Nullable A next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return tupleIterator.next().getA();
        }

        @Override
        public void forEachRemaining(Consumer<? super @Nullable A> action) {
            tupleIterator.forEachRemaining(tuple -> action.accept(tuple.getA()));
        }

    }

}
