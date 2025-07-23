package ai.timefold.solver.core.impl.domain.valuerange.buildin.collection;

import ai.timefold.solver.core.impl.domain.valuerange.cache.IdentityValueRangeCache;
import ai.timefold.solver.core.impl.domain.valuerange.cache.ValueRangeCacheStrategy;

import org.jspecify.annotations.NullMarked;

/**
 * Same as {@link SetValueRange}, but it employs a different caching strategy and remains immutable.
 *
 * @param <T> the value type
 */
@NullMarked
public final class IdentitySetValueRange<T> extends AbstractIdentityValueRange<T, SetValueRange<T>> {

    public IdentitySetValueRange(SetValueRange<T> valueRange) {
        super(valueRange);
    }

    @Override
    public ValueRangeCacheStrategy<T> generateCache() {
        return new IdentityValueRangeCache<>(valueRange.set);
    }

}
