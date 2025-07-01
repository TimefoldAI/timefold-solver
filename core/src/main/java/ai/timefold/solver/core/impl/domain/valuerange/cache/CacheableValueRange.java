package ai.timefold.solver.core.impl.domain.valuerange.cache;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;

import org.jspecify.annotations.NonNull;

/**
 * Cacheable value ranges enable the extraction of a caching representation for the related value range,
 * facilitating a quick way to check if a specified element exists within that range.
 */
public interface CacheableValueRange<Value_> extends CountableValueRange<Value_> {

    /**
     * Generates a cache strategy compatible with the value range.
     * The method always recomputes the cache for the value range and returns a new instance.
     */
    @NonNull
    ValueRangeCacheStrategy<Value_> generateCache();
}
