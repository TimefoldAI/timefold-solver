package ai.timefold.solver.core.impl.domain.valuerange.buildin.collection;

import java.util.Iterator;
import java.util.Objects;
import java.util.Random;

import ai.timefold.solver.core.impl.domain.valuerange.AbstractCountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.cache.IdentityValueRangeCache;
import ai.timefold.solver.core.impl.domain.valuerange.cache.ValueRangeCacheStrategy;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Same as {@link ListValueRange}, but it employs a different caching strategy and remains immutable.
 *
 * @param <T> the value type
 */
public final class IdentityListValueRange<T> extends AbstractCountableValueRange<T> {
    private final ListValueRange<T> valueRange;

    public IdentityListValueRange(ListValueRange<T> valueRange) {
        this.valueRange = Objects.requireNonNull(valueRange);
    }

    @Override
    public long getSize() {
        return valueRange.getSize();
    }

    @Override
    public @Nullable T get(long index) {
        return valueRange.get(index);
    }

    @Override
    public @NonNull Iterator<T> createOriginalIterator() {
        return valueRange.createOriginalIterator();
    }

    @Override
    public boolean contains(@Nullable T value) {
        return valueRange.contains(value);
    }

    @Override
    public @NonNull Iterator<T> createRandomIterator(@NonNull Random workingRandom) {
        return valueRange.createRandomIterator(workingRandom);
    }

    @Override
    public @NonNull ValueRangeCacheStrategy<T> generateCache() {
        return new IdentityValueRangeCache<>(valueRange.list);
    }
}
