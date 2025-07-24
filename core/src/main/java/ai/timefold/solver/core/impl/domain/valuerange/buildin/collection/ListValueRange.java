package ai.timefold.solver.core.impl.domain.valuerange.buildin.collection;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.impl.domain.valuerange.AbstractCountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.ValueRangeCache;
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
    public Iterator<T> createOriginalIterator() {
        return list.iterator();
    }

    @Override
    public Iterator<T> createRandomIterator(Random workingRandom) {
        return new CachedListRandomIterator<>(list, workingRandom);
    }

    @Override
    public String toString() {
        // Formatting: interval (mathematics) ISO 31-11
        return list.isEmpty() ? "[]" : "[" + list.get(0) + "-" + list.get(list.size() - 1) + "]";
    }

}
