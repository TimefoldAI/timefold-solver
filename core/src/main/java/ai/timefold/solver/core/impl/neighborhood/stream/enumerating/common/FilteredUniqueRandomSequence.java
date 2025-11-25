package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.util.ElementAwareArrayList;
import ai.timefold.solver.core.impl.util.ListEntry;

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

    private final List<ListEntry<T>> originalList;
    private final Predicate<T> filter;
    private final DefaultUniqueRandomSequence<T> delegate;

    public FilteredUniqueRandomSequence(List<ElementAwareArrayList.Entry<T>> listOfUniqueItems, Predicate<T> filter) {
        this.originalList = Collections.unmodifiableList(listOfUniqueItems);
        this.filter = Objects.requireNonNull(filter);
        this.delegate = new DefaultUniqueRandomSequence<>(listOfUniqueItems);
    }

    @Override
    public SequenceElement<T> pick(Random workingRandom) {
        var index = pickIndex(workingRandom);
        return new SequenceElement<>(originalList.get(index).getElement(), index);
    }

    public int pickIndex(Random workingRandom) {
        if (delegate.isEmpty()) {
            throw new NoSuchElementException("No more elements to pick from.");
        }
        var nonRemovedElement = delegate.pick(workingRandom);
        var originalRandomIndex = nonRemovedElement.index();

        var actualValueIndex = originalRandomIndex;
        var value = nonRemovedElement.value();
        while (!filter.test(value)) {
            delegate.remove(actualValueIndex);
            // We try the same random index again; the underlying sequence will find the next best non-removed element.
            actualValueIndex = delegate.pickIndex(workingRandom, originalRandomIndex);
            value = originalList.get(actualValueIndex).getElement();
        }
        return actualValueIndex;
    }

    @Override
    public T remove(Random workingRandom) {
        return remove(pickIndex(workingRandom));
    }

    @Override
    public T remove(int index) {
        var element = delegate.remove(index);
        if (!filter.test(originalList.get(index).getElement())) {
            throw new NoSuchElementException();
        }
        return element;
    }

}
