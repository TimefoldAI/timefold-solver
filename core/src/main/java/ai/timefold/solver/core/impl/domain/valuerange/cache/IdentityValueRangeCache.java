package ai.timefold.solver.core.impl.domain.valuerange.cache;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.impl.domain.valuerange.buildin.bigdecimal.BigDecimalValueRange;

/**
 * This caching strategy employs an {@link IdentityHashMap} to keep track of the values that have been added so far.
 * It is important to understand that this strategy should not be applied to special built-in value ranges,
 * such as {@link BigDecimalValueRange}.
 * Instead, it is intended for use with value ranges of POJO classes that do not have a numeric planning ID.
 * 
 * @param <Value_> the value type
 */
public class IdentityValueRangeCache<Value_> implements ValueRangeCacheStrategy<Value_> {

    private final int maxSize;
    // The value and its index
    private final Map<Value_, Integer> cache;
    private final List<Value_> values;

    public IdentityValueRangeCache() {
        // Initial value of 1K items
        this(1_000);
    }

    public IdentityValueRangeCache(int size) {
        this.maxSize = size;
        cache = new IdentityHashMap<>(size);
        values = new ArrayList<>(size);
    }

    private IdentityValueRangeCache(int maxSize, Map<Value_, Integer> cache, List<Value_> values) {
        this.maxSize = maxSize;
        this.cache = cache;
        this.values = values;
    }

    @Override
    public void add(Value_ value) {
        if (!cache.containsKey(value)) {
            values.add(value);
            cache.put(value, values.size() - 1);
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
        return cache.containsKey(value);
    }

    @Override
    public long getSize() {
        return values.size();
    }

    @Override
    public ValueRangeCacheStrategy<Value_> copy() {
        var cacheCopy = new IdentityHashMap<Value_, Integer>(maxSize);
        cacheCopy.putAll(cache);
        var valuesCopy = new ArrayList<Value_>(maxSize);
        valuesCopy.addAll(values);
        return new IdentityValueRangeCache<>(maxSize, cacheCopy, valuesCopy);
    }
}
