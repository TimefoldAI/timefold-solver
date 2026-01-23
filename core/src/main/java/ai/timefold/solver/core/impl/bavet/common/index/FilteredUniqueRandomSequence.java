package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

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
final class FilteredUniqueRandomSequence<T> implements UniqueRandomSequence<T> {

    FilteredUniqueRandomSequence(ElementAwareArrayList<T> source, Random workingRandom, Predicate<T> filter) {
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public T next() {
        return null;
    }

}
