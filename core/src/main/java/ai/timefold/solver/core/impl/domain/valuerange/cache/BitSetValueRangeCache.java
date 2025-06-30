package ai.timefold.solver.core.impl.domain.valuerange.cache;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.impl.domain.valuerange.buildin.primint.IntValueRange;

/**
 * This caching strategy employs a {@link BitSet} to keep track of the values that have been added so far.
 * It is designed for use in situations where the POJO class has a planning ID of an integer type
 * or with the built-in value range of {@link IntValueRange}.
 *
 * @param <Value_> the value type
 */
public class BitSetValueRangeCache<Value_> implements ValueRangeCacheStrategy<Value_> {

    private final int maxSize;
    private final BitSet cache;
    private final List<Value_> values;
    private final Function<Value_, Integer> extractIdFunction;

    public BitSetValueRangeCache(Function<Value_, Integer> extractIdFunction) {
        // Initial value of 1K items
        this(1_000, extractIdFunction);
    }

    public BitSetValueRangeCache(int size, Function<Value_, Integer> extractIdFunction) {
        this.maxSize = size;
        this.extractIdFunction = extractIdFunction;
        cache = new BitSet(size);
        values = new ArrayList<>(size);
    }

    private BitSetValueRangeCache(int maxSize, Function<Value_, Integer> extractIdFunction, BitSet cache, List<Value_> values) {
        this.maxSize = maxSize;
        this.extractIdFunction = extractIdFunction;
        this.cache = cache;
        this.values = values;
    }

    @Override
    public void add(Value_ value) {
        var id = extractIdFunction.apply(value);
        if (!cache.get(id)) {
            values.add(value);
            cache.set(id);
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
        return cache.get(extractIdFunction.apply(value));
    }

    @Override
    public long getSize() {
        return values.size();
    }

    @Override
    public ValueRangeCacheStrategy<Value_> copy() {
        var cacheCopy = (BitSet) cache.clone();
        var valuesCopy = new ArrayList<Value_>(maxSize);
        valuesCopy.addAll(values);
        return new BitSetValueRangeCache<>(maxSize, extractIdFunction, cacheCopy, valuesCopy);
    }
}
