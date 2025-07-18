package ai.timefold.solver.core.impl.domain.valuerange.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.impl.domain.valuerange.buildin.bigdecimal.BigDecimalValueRange;

import org.jspecify.annotations.NonNull;

/**
 * This caching strategy employs an {@link IdentityHashMap} to keep track of the values that have been added so far.
 * It is important to understand that this strategy should not be applied to special built-in value ranges,
 * such as {@link BigDecimalValueRange}.
 * Instead, it is intended for use with value ranges of POJO classes that do not have a numeric planning ID.
 * 
 * @param <Value_> the value type
 */
public final class IdentityValueRangeCache<Value_> implements ValueRangeCacheStrategy<Value_> {

    // The value and its index
    private final Set<Value_> cache;
    private final List<Value_> values;

    public IdentityValueRangeCache(List<Value_> list) {
        values = new ArrayList<>(list);
        cache = Collections.newSetFromMap(new IdentityHashMap<>(list.size()));
        cache.addAll(list);
    }

    @Override
    public void add(@NonNull Value_ value) {
        if (!cache.contains(value)) {
            values.add(value);
            cache.add(value);
        }
    }

    @Override
    public Value_ get(int index) {
        if (index < 0 || index >= values.size()) {
            throw new IndexOutOfBoundsException("Index: %d, Size: %d".formatted(index, values.size()));
        }
        return values.get(index);
    }

    @Override
    public boolean contains(@NonNull Value_ value) {
        return cache.contains(value);
    }

    @Override
    public long getSize() {
        return values.size();
    }

    @Override
    public @NonNull List<Value_> getAll() {
        return values;
    }
}
