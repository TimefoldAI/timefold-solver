package ai.timefold.solver.core.impl.domain.valuerange.cache;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This caching strategy employs a {@link HashSet} to track the values that have been added so far.
 * This approach is more general,
 * as it does not solely depend on object references and can be used for any special built-in value ranges.
 *
 * @param <Value_> the value type
 */
public class HashSetValueRangeCache<Value_> implements ValueRangeCacheStrategy<Value_> {

    private final int maxSize;
    private final Set<Value_> cache;
    private final List<Value_> values;

    public HashSetValueRangeCache() {
        // Initial value of 1K items
        this(1_000);
    }

    public HashSetValueRangeCache(int size) {
        this.maxSize = size;
        cache = new HashSet<>(size);
        values = new ArrayList<>(size);
    }

    private HashSetValueRangeCache(int maxSize, Set<Value_> cache, List<Value_> values) {
        this.maxSize = maxSize;
        this.cache = cache;
        this.values = values;
    }

    @Override
    public void add(Value_ value) {
        if (!cache.contains(value)) {
            values.add(value);
            cache.add(value);
        }
    }

    @Override
    public Value_ get(int index) {
        if (index < 0 || index >= values.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + values.size());
        }
        return values.get(index);
    }

    @Override
    public boolean contains(Value_ value) {
        return cache.contains(value);
    }

    @Override
    public long getSize() {
        return values.size();
    }

    @Override
    public ValueRangeCacheStrategy<Value_> copy() {
        var cacheCopy = new HashSet<Value_>(maxSize);
        cacheCopy.addAll(cache);
        var valuesCopy = new ArrayList<Value_>(maxSize);
        valuesCopy.addAll(values);
        return new HashSetValueRangeCache<>(maxSize, cacheCopy, valuesCopy);
    }
}
