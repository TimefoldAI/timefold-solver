package ai.timefold.solver.core.impl.domain.valuerange.buildin.collection;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ai.timefold.solver.core.impl.domain.valuerange.AbstractCountableValueRange;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.CachedListRandomIterator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ListValueRange<T> extends AbstractCountableValueRange<T> {

    private static final int LIST_SIZE_LOOKUP_LIMIT = 10;

    private final List<T> list;
    private @Nullable Set<T> lookupSet; // Initialized lazily for large lists.

    public ListValueRange(List<T> list) {
        this.list = list;
    }

    @Override
    public long getSize() {
        return list.size();
    }

    @Override
    public T get(long index) {
        if (index > Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException("The index (" + index + ") must fit in an int.");
        }
        return list.get((int) index);
    }

    @Override
    public boolean contains(@Nullable T value) {
        if (list.size() > LIST_SIZE_LOOKUP_LIMIT) {
            if (lookupSet == null) {
                lookupSet = Set.copyOf(list);
            }
            return lookupSet.contains(value);
        } else { // For small lists, sequential scanning is not a performance issue.
            return list.contains(value);
        }
    }

    @Override
    public @NonNull Iterator<T> createOriginalIterator() {
        return list.iterator();
    }

    @Override
    public @NonNull Iterator<T> createRandomIterator(@NonNull Random workingRandom) {
        return new CachedListRandomIterator<>(list, workingRandom);
    }

    @Override
    public String toString() {
        // Formatting: interval (mathematics) ISO 31-11
        return list.isEmpty() ? "[]" : "[" + list.get(0) + "-" + list.get(list.size() - 1) + "]";
    }

}
