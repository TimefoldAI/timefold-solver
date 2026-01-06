package ai.timefold.solver.core.impl.domain.valuerange.buildin.collection;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.AbstractCountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.ValueRangeCache;
import ai.timefold.solver.core.impl.domain.valuerange.sort.ValueRangeSorter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class SetValueRange<T> extends AbstractCountableValueRange<T> {

    private static final int VALUES_TO_LIST_IN_TO_STRING = 3;
    private static final String VALUE_DELIMITER = ", ";

    private final boolean isValueImmutable;
    private final Set<T> set;
    private @Nullable ValueRangeCache<T> cache;

    public SetValueRange(Set<T> set) {
        this(set, false);
    }

    public SetValueRange(Set<T> set, boolean isValueImmutable) {
        this.isValueImmutable = isValueImmutable;
        this.set = set;
    }

    @Override
    public boolean isValueImmutable() {
        return isValueImmutable;
    }

    @Override
    public long getSize() {
        return set.size();
    }

    @Override
    public T get(long index) {
        return getCache().get((int) index);
    }

    private ValueRangeCache<T> getCache() {
        if (cache == null) {
            var cacheBuilder = isValueImmutable ? ValueRangeCache.Builder.FOR_TRUSTED_VALUES
                    : ValueRangeCache.Builder.FOR_USER_VALUES;
            cache = cacheBuilder.buildCache(set);
        }
        return cache;
    }

    @Override
    public boolean contains(@Nullable T value) {
        return set.contains(value);
    }

    @Override
    public ValueRange<T> sort(ValueRangeSorter<T> sorter) {
        var sortedSet = sorter.sort(set);
        return new SetValueRange<>(sortedSet);
    }

    @Override
    public Iterator<T> createOriginalIterator() {
        return set.iterator();
    }

    @Override
    public Iterator<T> createRandomIterator(Random workingRandom) {
        return getCache().iterator(workingRandom);
    }

    @Override
    public boolean equals(Object o) {
        // We do not use Objects.equals(...) due to https://bugs.openjdk.org/browse/JDK-8015417.
        if (this == o) {
            return true;
        }
        if (!(o instanceof SetValueRange<?> that)) {
            return false;
        }
        return isValueImmutable == that.isValueImmutable &&
                set.equals(that.set);
    }

    @Override
    public int hashCode() {
        // We do not use Objects.hash(...) because it creates an array each time.
        // We do not use Objects.hashCode() due to https://bugs.openjdk.org/browse/JDK-8015417.
        var hash = 1;
        hash = 31 * hash + Boolean.hashCode(isValueImmutable);
        return 31 * hash + set.hashCode();
        return hash;
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
