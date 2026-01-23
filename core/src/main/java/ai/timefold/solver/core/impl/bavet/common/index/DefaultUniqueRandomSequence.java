package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.Random;

import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

import org.jspecify.annotations.NullMarked;

/**
 * Keeps an exact track of which items were already removed from the sequence,
 * so that no item is ever returned twice.
 * It accepts a list of unique items on input, and does not copy or modify it.
 *
 * @param <T>
 */
@NullMarked
final class DefaultUniqueRandomSequence<T> implements UniqueRandomSequence<T> {

    DefaultUniqueRandomSequence(ElementAwareArrayList<T> source, Random workingRandom) {
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
