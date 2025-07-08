package ai.timefold.solver.core.impl.domain.valuerange.buildin.entity;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.AbstractCountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.cache.CacheableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.cache.ValueRangeCacheStrategy;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.FromEntityPropertyValueRangeDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.CachedListRandomIterator;

import org.jspecify.annotations.NonNull;

/**
 * The value range is built based on the individual value ranges of each entity.
 * 
 * @param <Value_> the value type
 */
public final class FromEntityListValueRange<Value_> extends AbstractCountableValueRange<Value_> {

    private final ValueRangeCacheStrategy<Value_> cacheStrategy;

    public <Solution_> FromEntityListValueRange(List<?> entityList,
            FromEntityPropertyValueRangeDescriptor<Solution_> valueRangeDescriptor) {
        if (entityList.isEmpty()) {
            throw new IllegalArgumentException("Impossible state: the entity list (%s) cannot be empty."
                    .formatted(valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getEntityClass()
                            .getSimpleName()));
        }
        // Some components of the solver,
        // such as the iterators and the list variable state,
        // expect the value list to have the correct size.
        // Therefore, we create a unique values list in advance to return consistent information to the outer tiers.
        var firstValueRange =
                (CacheableValueRange<Value_>) valueRangeDescriptor.<Value_> extractValueRange(null, entityList.get(0));
        this.cacheStrategy = firstValueRange.generateCache();
        for (var i = 1; i < entityList.size(); ++i) {
            var entity = entityList.get(i);
            var otherValueRange = (CountableValueRange<Value_>) valueRangeDescriptor.<Value_> extractValueRange(null, entity);
            this.cacheStrategy.merge(otherValueRange);
        }
    }

    @Override
    public long getSize() {
        return cacheStrategy.getSize();
    }

    @Override
    public Value_ get(long index) {
        return cacheStrategy.get((int) index);
    }

    @Override
    public boolean contains(Value_ value) {
        return cacheStrategy.contains(value);
    }

    @Override
    public @NonNull Iterator<Value_> createOriginalIterator() {
        return cacheStrategy.getAll().iterator();
    }

    @Override
    public @NonNull Iterator<Value_> createRandomIterator(@NonNull Random workingRandom) {
        return new CachedListRandomIterator<>(cacheStrategy.getAll(), workingRandom);
    }

    @Override
    public String toString() {
        return cacheStrategy.getAll().isEmpty() ? "[]"
                : "[" + cacheStrategy.getAll().get(0) + "-" + cacheStrategy.getAll().get(cacheStrategy.getAll().size() - 1)
                        + "]";
    }

}
