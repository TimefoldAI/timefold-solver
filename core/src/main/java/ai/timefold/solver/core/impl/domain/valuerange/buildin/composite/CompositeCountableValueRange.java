package ai.timefold.solver.core.impl.domain.valuerange.buildin.composite;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.AbstractCountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.ValueRangeCache;
import ai.timefold.solver.core.impl.domain.valuerange.sort.ValueRangeSorter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class CompositeCountableValueRange<T> extends AbstractCountableValueRange<T> {

    private final boolean isValueImmutable;
    private final List<? extends AbstractCountableValueRange<T>> valueRangeList;
    private final ValueRangeCache<T> cache;

    public CompositeCountableValueRange(List<? extends AbstractCountableValueRange<T>> childValueRangeList) {
        var maximumSize = 0L;
        var isImmutable = true;
        for (AbstractCountableValueRange<T> childValueRange : childValueRangeList) {
            isImmutable &= childValueRange.isValueImmutable();
            // We choose to select the larger size instead of summing all sizes, as they may not be distinct.
            // This approach opts for the cost of resizing instead of allocating larger chunks of memory.
            var size = childValueRange.getSize();
            if (size > maximumSize) {
                maximumSize = size;
            }
        }
        // To eliminate duplicates, we immediately expand the child value ranges into a cache.
        var cacheBuilder = isImmutable ? ValueRangeCache.Builder.FOR_TRUSTED_VALUES
                : ValueRangeCache.Builder.FOR_USER_VALUES;
        this.cache = cacheBuilder.buildCache((int) maximumSize);
        for (var childValueRange : childValueRangeList) {
            // If the child value range includes nulls, we will ignore them.
            // They will be added later by the wrapper, if necessary.
            if (childValueRange instanceof NullAllowingCountableValueRange<T> nullAllowingCountableValueRange) {
                childValueRange = nullAllowingCountableValueRange.getChildValueRange();
            }
            childValueRange.createOriginalIterator().forEachRemaining(cache::add);
        }
        this.isValueImmutable = isImmutable;
        this.valueRangeList = childValueRangeList;
    }

    private CompositeCountableValueRange(boolean isValueImmutable,
            List<? extends AbstractCountableValueRange<T>> valueRangeList, ValueRangeCache<T> cache) {
        this.isValueImmutable = isValueImmutable;
        this.valueRangeList = valueRangeList;
        this.cache = cache;
    }

    @Override
    public boolean isValueImmutable() {
        return isValueImmutable;
    }

    @Override
    public long getSize() {
        return cache.getSize();
    }

    @Override
    public T get(long index) {
        return cache.get((int) index);
    }

    @Override
    public ValueRange<T> sort(ValueRangeSorter<T> sorter) {
        var sortedCache = this.cache.sort(sorter);
        return new CompositeCountableValueRange<>(isValueImmutable, valueRangeList, sortedCache);
    }

    @Override
    public boolean contains(@Nullable T value) {
        return cache.contains(value);
    }

    @Override
    public Iterator<T> createOriginalIterator() {
        return cache.iterator();
    }

    @Override
    public Iterator<T> createRandomIterator(Random workingRandom) {
        return cache.iterator(workingRandom);
    }

    @Override
    public boolean equals(Object o) {
        // We do not use Objects.equals(...) due to https://bugs.openjdk.org/browse/JDK-8015417.
        if (this == o) {
            return true;
        }
        return o instanceof CompositeCountableValueRange<?> that &&
                isValueImmutable == that.isValueImmutable &&
                valueRangeList.equals(that.valueRangeList);
    }

    @Override
    public int hashCode() {
        // We do not use Objects.hash(...) because it creates an array each time.
        // We do not use Objects.hashCode() due to https://bugs.openjdk.org/browse/JDK-8015417.
        var hash = 1;
        hash = 31 * hash + Boolean.hashCode(isValueImmutable);
        return 31 * hash + valueRangeList.hashCode();
    }

}
