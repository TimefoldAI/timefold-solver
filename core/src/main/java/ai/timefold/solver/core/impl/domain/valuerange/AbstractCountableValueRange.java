package ai.timefold.solver.core.impl.domain.valuerange;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;
import ai.timefold.solver.core.impl.domain.valuerange.cache.CacheableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.cache.HashSetValueRangeCache;
import ai.timefold.solver.core.impl.domain.valuerange.cache.ValueRangeCacheStrategy;

import org.jspecify.annotations.NonNull;

/**
 * Abstract superclass for {@link CountableValueRange} and {@link CacheableValueRange} (and therefore {@link ValueRange}).
 *
 * @see CountableValueRange
 * @see CacheableValueRange
 * @see ValueRange
 * @see ValueRangeFactory
 */
public abstract class AbstractCountableValueRange<T> implements CacheableValueRange<T> {

    @Override
    public boolean isEmpty() {
        return getSize() == 0L;
    }

    /**
     * By default, we use the {@link HashSetValueRangeCache} strategy as it is applicable in more general cases.
     */
    @Override
    public @NonNull ValueRangeCacheStrategy<T> generateCache() {
        var cacheStrategy = new HashSetValueRangeCache<T>((int) getSize());
        createOriginalIterator().forEachRemaining(cacheStrategy::add);
        return cacheStrategy;
    }

}
