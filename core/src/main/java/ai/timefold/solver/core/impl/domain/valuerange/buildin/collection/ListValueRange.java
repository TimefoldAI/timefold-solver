package ai.timefold.solver.core.impl.domain.valuerange.buildin.collection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.AbstractCountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.ValueRangeCache;
import ai.timefold.solver.core.impl.domain.valuerange.sort.ValueRangeSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.CachedListRandomIterator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ListValueRange<T> extends AbstractCountableValueRange<T> {

    private final boolean isValueImmutable;
    private final List<T> list;
    private @Nullable ValueRangeCache<T> cache;

    public ListValueRange(List<T> list) {
        this(list, false);
    }

    public ListValueRange(List<T> list, boolean isValueImmutable) {
        this.isValueImmutable = isValueImmutable;
        this.list = list;
    }

    @Override
    public boolean isValueImmutable() {
        return isValueImmutable;
    }

    @Override
    public long getSize() {
        return list.size();
    }

    @Override
    public @Nullable T get(long index) {
        if (index > Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException("The index (" + index + ") must fit in an int.");
        }
        return list.get((int) index);
    }

    @Override
    public boolean contains(@Nullable T value) {
        if (cache == null) {
            var cacheBuilder = isValueImmutable ? ValueRangeCache.Builder.FOR_TRUSTED_VALUES
                    : ValueRangeCache.Builder.FOR_USER_VALUES;
            cache = cacheBuilder.buildCache(list);
        }
        return cache.contains(value);
    }

    @Override
    public ValueRange<T> sort(ValueRangeSorter<T> sorter) {
        // The list may be immutable and need to be copied
        var sortableList = new ArrayList<>(list);
        sorter.sort(sortableList);
        return new ListValueRange<>(sortableList, isValueImmutable);
    }

    @Override
    public Iterator<T> createOriginalIterator() {
        return list.iterator();
    }

    @Override
    public Iterator<T> createRandomIterator(Random workingRandom) {
        return new CachedListRandomIterator<>(list, workingRandom);
    }

    @Override
    public boolean equals(Object o) {
        // We do not use Objects.equals(...) due to https://bugs.openjdk.org/browse/JDK-8015417.
        if (this == o) {
            return true;
        }
        if (!(o instanceof ListValueRange<?> that)) {
            return false;
        }
        return isValueImmutable == that.isValueImmutable &&
                list.equals(that.list);
    }

    @Override
    public int hashCode() {
        // We do not use Objects.hash(...) because it creates an array each time.
        // We do not use Objects.hashCode() due to https://bugs.openjdk.org/browse/JDK-8015417.
        var hash = 1;
        hash = 31 * hash + Boolean.hashCode(isValueImmutable);
        return 31 * hash + list.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        // Formatting: interval (mathematics) ISO 31-11
        return list.isEmpty() ? "[]" : "[" + list.get(0) + "-" + list.get(list.size() - 1) + "]";
    }

}
