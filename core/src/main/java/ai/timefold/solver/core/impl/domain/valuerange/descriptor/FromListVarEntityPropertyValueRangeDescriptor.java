package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.entity.FromEntityListValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.cache.CacheableValueRange;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;

public class FromListVarEntityPropertyValueRangeDescriptor<Solution_>
        extends FromEntityPropertyValueRangeDescriptor<Solution_> {

    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    // We store the most recent cached solution to analyze if the statistics must be refreshed
    private Solution_ lastCachedSolution;
    private long cachedRangeSize;

    public FromListVarEntityPropertyValueRangeDescriptor(GenuineVariableDescriptor<Solution_> variableDescriptor,
            boolean addNullInValueRange, MemberAccessor memberAccessor) {
        super(variableDescriptor, addNullInValueRange, memberAccessor);
        if (!(variableDescriptor instanceof ListVariableDescriptor<Solution_> descriptor)) {
            throw new IllegalArgumentException(
                    "The source of the value range must be instance of %s."
                            .formatted(ListVariableDescriptor.class.getSimpleName()));
        }
        this.listVariableDescriptor = descriptor;
    }

    @Override
    public long extractValueRangeSize(Solution_ solution, Object entity) {
        if (entity == null) {
            return readValueRangeSizeFromSolution(solution);
        } else {
            return readValueRangeSize(entity);
        }
    }

    private long readValueRangeSizeFromSolution(Solution_ solution) {
        computeSize(solution);
        return cachedRangeSize;
    }

    @Override
    public <Value_> CacheableValueRange<Value_> extractValueRange(Solution_ solution, Object entity) {
        if (entity == null) {
            return readValueRangeFromSolution(solution);
        } else {
            ValueRange<Value_> range = readValueRange(entity);
            if (!(range instanceof CacheableValueRange<Value_> cacheableValueRange)) {
                throw new IllegalArgumentException(
                        "The value range must be based on %s".formatted(CacheableValueRange.class.getSimpleName()));
            }
            return cacheableValueRange;
        }
    }

    private <Value_> CacheableValueRange<Value_> readValueRangeFromSolution(Solution_ solution) {
        computeSize(solution);
        return new FromEntityListValueRange<>(listVariableDescriptor.getEntityDescriptor().extractEntities(solution),
                (int) cachedRangeSize, this);
    }

    /**
     * The statistics are calculated
     * to determine the total size of the value range and the maximum size of entity value ranges.
     * The ranges are not stored to avoid consuming memory for large datasets.
     * 
     * @param solution the working solution
     */
    private void computeSize(Solution_ solution) {
        // We verify whether the solution has changed by any real-time events
        if (lastCachedSolution != null && lastCachedSolution == solution) {
            return;
        }
        var entityList = listVariableDescriptor.getEntityDescriptor().extractEntities(solution);
        if (entityList.isEmpty()) {
            throw new IllegalArgumentException("Impossible state: the entity list (%s) cannot be empty."
                    .formatted(listVariableDescriptor.getEntityDescriptor().getEntityClass().getSimpleName()));
        }
        var cacheStrategy = extractValueRange(null, entityList.get(0)).generateCache();
        var maxEntitySize = cacheStrategy.getSize();
        for (var i = 1; i < entityList.size(); i++) {
            var otherCacheStrategy = extractValueRange(null, entityList.get(i)).generateCache();
            if (otherCacheStrategy.getSize() > maxEntitySize) {
                maxEntitySize = otherCacheStrategy.getSize();
            }
            cacheStrategy.merge(otherCacheStrategy);
        }
        this.lastCachedSolution = solution;
        this.cachedRangeSize = cacheStrategy.getSize();
    }
}
