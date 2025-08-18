package ai.timefold.solver.core.impl.domain.valuerange.buildin.composite;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.impl.domain.valuerange.AbstractCountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.ValueRangeCache;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class CompositeCountableValueRange<T> extends AbstractCountableValueRange<T> {

    private final boolean isValueImmutable;
    private final ValueRangeCache<T> cache;

    public CompositeCountableValueRange(List<? extends AbstractCountableValueRange<T>> childValueRangeList) {
        var maximumSize = 0L;
        var isValueImmutable = true;
        for (AbstractCountableValueRange<T> childValueRange : childValueRangeList) {
            isValueImmutable &= childValueRange.isValueImmutable();
            maximumSize += childValueRange.getSize();
        }
        // To eliminate duplicates, we immediately expand the child value ranges into a cache.
        var cacheBuilder = isValueImmutable ? ValueRangeCache.Builder.FOR_TRUSTED_VALUES
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
        this.isValueImmutable = isValueImmutable;
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

}
