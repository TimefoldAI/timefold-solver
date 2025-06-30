package ai.timefold.solver.core.impl.domain.valuerange.buildin.collection;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import ai.timefold.solver.core.impl.domain.valuerange.AbstractCountableValueRange;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.CachedListRandomIterator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class SetValueRange<T> extends AbstractCountableValueRange<T> {

    private static final int VALUES_TO_LIST_IN_TO_STRING = 3;
    private static final String VALUE_DELIMITER = ", ";

    private final Set<T> set;
    private @Nullable List<T> list; // To allow for random access; null until first access.

    public SetValueRange(Set<T> set) {
        this.set = set;
    }

    @Override
    public long getSize() {
        return set.size();
    }

    @Override
    public T get(long index) {
        if (index > Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException("The index (%d) must fit in an int."
                    .formatted(index));
        }
        return getList().get((int) index);
    }

    private List<T> getList() {
        if (list == null) {
            list = List.copyOf(set);
        }
        return list;
    }

    @Override
    public boolean contains(@Nullable T value) {
        return set.contains(value);
    }

    @Override
    public @NonNull Iterator<T> createOriginalIterator() {
        return set.iterator();
    }

    @Override
    public @NonNull Iterator<T> createRandomIterator(@NonNull Random workingRandom) {
        return new CachedListRandomIterator<>(getList(), workingRandom);
    }

    @Override
    public String toString() { // Formatting: interval (mathematics) ISO 31-11, shorten long sets
        var suffix = set.size() > VALUES_TO_LIST_IN_TO_STRING ? VALUE_DELIMITER + "...}" : "}";
        return set.isEmpty() ? "{}"
                : set.stream()
                        .limit(VALUES_TO_LIST_IN_TO_STRING)
                        .map(Object::toString)
                        .collect(Collectors.joining(VALUE_DELIMITER, "{", suffix));
    }

}
