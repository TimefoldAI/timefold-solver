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
public final class HashSetValueRangeCache<Value_> implements ValueRangeCacheStrategy<Value_> {

    private final Set<Value_> cache;
    private final List<Value_> values;

    public HashSetValueRangeCache(int size) {
        cache = new HashSet<>(size);
        values = new ArrayList<>(size);
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
    public List<Value_> getAll() {
        return values;
    }
}
