package ai.timefold.solver.core.impl.domain.valuerange.buildin.collection;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import ai.timefold.solver.core.impl.domain.valuerange.AbstractCountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.cache.HashSetValueRangeCache;
import ai.timefold.solver.core.impl.domain.valuerange.cache.ValueRangeCacheStrategy;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.CachedListRandomIterator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class SetValueRange<T> extends AbstractCountableValueRange<T> {

    private static final int VALUES_TO_LIST_IN_TO_STRING = 3;
    private static final String VALUE_DELIMITER = ", ";

    final Set<T> set;
    private @Nullable ValueRangeCacheStrategy<T> cacheStrategy;

    public SetValueRange(Set<T> set) {
        this.set = set;
    }

    @Override
    public long getSize() {
        return set.size();
    }

    @Override
    public @Nullable T get(long index) {
        if (cacheStrategy == null) {
            cacheStrategy = generateCache();
        }
        return cacheStrategy.get((int) index);
    }

    @Override
    public boolean contains(@Nullable T value) {
        return set.contains(value);
    }

    @Override
    public ValueRangeCacheStrategy<T> generateCache() {
        return new HashSetValueRangeCache<>(set);
    }

    @Override
    public Iterator<T> createOriginalIterator() {
        return set.iterator();
    }

    @Override
    public Iterator<T> createRandomIterator(Random workingRandom) {
        if (cacheStrategy == null) {
            cacheStrategy = generateCache();
        }
        return new CachedListRandomIterator<>(cacheStrategy.getAll(), workingRandom);
    }

    @Override
    public String toString() { // Formatting: interval (mathematics) ISO 31-11
        var suffix = set.size() > VALUES_TO_LIST_IN_TO_STRING ? VALUE_DELIMITER + "...}" : "}";
        return set.isEmpty() ? "{}"
                : set.stream()
                        .limit(VALUES_TO_LIST_IN_TO_STRING)
                        .map(Object::toString)
                        .collect(Collectors.joining(VALUE_DELIMITER, "{", suffix));
    }

}
