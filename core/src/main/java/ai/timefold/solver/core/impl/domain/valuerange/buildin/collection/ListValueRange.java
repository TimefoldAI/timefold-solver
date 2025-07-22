package ai.timefold.solver.core.impl.domain.valuerange.buildin.collection;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.impl.domain.valuerange.AbstractCountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.cache.HashSetValueRangeCache;
import ai.timefold.solver.core.impl.domain.valuerange.cache.ValueRangeCacheStrategy;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.CachedListRandomIterator;

import org.jspecify.annotations.NonNull;

public final class ListValueRange<T> extends AbstractCountableValueRange<T> {

    final List<T> list;
    private ValueRangeCacheStrategy<T> cacheStrategy;

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
    public boolean contains(T value) {
        if (cacheStrategy == null) {
            cacheStrategy = generateCache();
        }
        return cacheStrategy.contains(value);
    }

    @Override
    public @NonNull ValueRangeCacheStrategy<T> generateCache() {
        return new HashSetValueRangeCache<>(list);
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
