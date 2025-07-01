package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import java.util.IdentityHashMap;
import java.util.Map;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.entity.FromEntityListValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.cache.CacheableValueRange;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;

public class FromListVarEntityPropertyValueRangeDescriptor<Solution_>
        extends FromEntityPropertyValueRangeDescriptor<Solution_> {

    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    // We cache the last working solution and the related value range
    private Solution_ lastCachedSolution;
    private FromEntityListValueRange<?> cachedValueRange;
    private Map<Object, CacheableValueRange<?>> entityCacheValueRange;

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
        computeRange(solution);
        return cachedValueRange.getSize();
    }

    @Override
    public <Value_> CacheableValueRange<Value_> extractValueRange(Solution_ solution, Object entity) {
        if (entity == null) {
            return readValueRangeFromSolution(solution);
        } else {
            // TODO - Evaluate if a real-time change can return a stale entity value range
            var range = (ValueRange<Value_>) entityCacheValueRange.get(entity);
            if (range == null) {
                range = readValueRange(entity);
                if (!(range instanceof CacheableValueRange<Value_> cacheableValueRange)) {
                    throw new IllegalArgumentException(
                            "The value range must be based on %s".formatted(CacheableValueRange.class.getSimpleName()));
                }
                entityCacheValueRange.put(entity, cacheableValueRange);
            }
            return (CacheableValueRange<Value_>) range;
        }
    }

    private <Value_> CacheableValueRange<Value_> readValueRangeFromSolution(Solution_ solution) {
        computeRange(solution);
        return (CacheableValueRange<Value_>) cachedValueRange;
    }

    /**
     * This method generates and stores the value range for the last working solution
     * to prevent recomputing the unique value list.
     * <p>
     * Loading the values in advance is necessary
     * because the ListVariableState needs
     * to know the exact number of values to calculate the count of unassigned values accurately.
     * <p>
     * The calculation occurs only once when the solver starts
     * and whenever the solution is altered due to real-time events.
     * The real-time changing event is identified
     * when {@link #extractValueRange(Object, Object)} is called with a different solution than the last working solution.
     * 
     * @param solution the working solution
     */
    private void computeRange(Solution_ solution) {
        // We verify whether the solution has changed by any real-time events
        if (lastCachedSolution != null && lastCachedSolution == solution) {
            return;
        }
        this.lastCachedSolution = solution;
        var entityList = listVariableDescriptor.getEntityDescriptor().extractEntities(solution);
        this.entityCacheValueRange = new IdentityHashMap<>(entityList.size());
        this.cachedValueRange = new FromEntityListValueRange<>(entityList, this);
    }
}
