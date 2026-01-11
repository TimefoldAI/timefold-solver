package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

import org.jspecify.annotations.NullMarked;

/**
 * Unlike {@link DefaultUniqueRandomSequence}, this class only returns elements that match the given filter.
 * Because we can't predict how many elements will be filtered out,
 * and because we don't want to pre-filter the entire list,
 * this class may need to try multiple times to find a matching element.
 * It also can't provide an emptiness check
 * and has to rely on {@link NoSuchElementException} to be thrown when everything's been finally filtered out.
 * Other than that, it relies on {@link DefaultUniqueRandomSequence} to keep track of removed elements.
 * 
 * @param <T>
 */
@NullMarked
public final class FilteredUniqueRandomSequence<T> implements UniqueRandomSequence<T> {

    private final ElementAccessor<T> accessor;
    private final Predicate<T> filter;
    private final DefaultUniqueRandomSequence<T> delegate;

    FilteredUniqueRandomSequence(ElementAccessor<T> accessor, Predicate<T> filter) {
        this.accessor = accessor;
        this.filter = Objects.requireNonNull(filter);
        this.delegate = new DefaultUniqueRandomSequence<>(accessor);
    }

    @Override
    public SequenceElement<T> pick(Random workingRandom) {
        if (delegate.isEmpty()) {
            throw new NoSuchElementException("No more elements to pick from.");
        }
        var nonRemovedElement = delegate.pick(workingRandom);
        var originalRandomIndex = nonRemovedElement.index();

        var actualValueIndex = originalRandomIndex;
        var value = nonRemovedElement.value();
        while (!filter.test(value)) {
            if (delegate.isEmpty()) {
                throw new NoSuchElementException("No more elements to pick from.");
            }
            delegate.removeWithoutReturn(actualValueIndex);
            // We try the same random index again; the underlying sequence will find the next best non-removed element.
            actualValueIndex = delegate.pickIndex(workingRandom, originalRandomIndex);
            value = accessor.get(actualValueIndex);
        }
        return new SequenceElement<>(value, actualValueIndex);
    }

    @Override
    public T remove(Random workingRandom) {
        var element = pick(workingRandom);
        delegate.removeWithoutReturn(element.index());
        return element.value();
    }

    /**
     * Returns whether there are no more elements to pick from.
     *
     * @return true if the underlying sequence has no more elements;
     *         otherwise true, even if all of those elements may later be filtered out.
     */
    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

}
