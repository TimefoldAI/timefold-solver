package ai.timefold.solver.core.impl.domain.valuerange.cache;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * This caching strategy employs a {@link HashSet} to track the values that have been added so far.
 * This approach is more general,
 * as it does not solely depend on object references and can be used for any special built-in value ranges.
 *
 * @param <Value_> the value type
 */
@NullMarked
public final class HashSetValueRangeCache<Value_> implements ValueRangeCacheStrategy<Value_> {

    private final Set<Value_> cache;
    private final List<Value_> values;

    public HashSetValueRangeCache(int size) {
        cache = new HashSet<>(size);
        values = new ArrayList<>(size);
    }

    public HashSetValueRangeCache(List<Value_> list) {
        values = new ArrayList<>(list);
        cache = new HashSet<>(list);
    }

    public HashSetValueRangeCache(Set<Value_> set) {
        values = new ArrayList<>(set);
        cache = new HashSet<>(set);
    }

    @Override
    public void add(@Nullable Value_ value) {
        if (cache.add(value)) {
            values.add(value);
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
    public boolean contains(@Nullable Value_ value) {
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
