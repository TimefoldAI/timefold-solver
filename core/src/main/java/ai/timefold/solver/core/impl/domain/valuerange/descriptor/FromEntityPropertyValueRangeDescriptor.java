package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.entity.FromEntityListValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.cache.CacheableValueRange;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class FromEntityPropertyValueRangeDescriptor<Solution_>
        extends AbstractFromPropertyValueRangeDescriptor<Solution_>
        implements IterableValueRangeDescriptor<Solution_> {

    public FromEntityPropertyValueRangeDescriptor(GenuineVariableDescriptor<Solution_> variableDescriptor,
            boolean addNullInValueRange, MemberAccessor memberAccessor) {
        super(variableDescriptor, addNullInValueRange, memberAccessor);
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
        var valueRange = extractValueRange(solution, null);
        if (valueRange instanceof CacheableValueRange<?> cacheableValueRange) {
            return cacheableValueRange.getSize();
        }
        throw new IllegalArgumentException(
                "The value range must be based on %s".formatted(CacheableValueRange.class.getSimpleName()));
    }

    @Override
    public <T> ValueRange<T> extractValueRange(Solution_ solution, Object entity) {
        if (entity == null) {
            var entityList = variableDescriptor.getEntityDescriptor().extractEntities(solution);
            return new FromEntityListValueRange<>(entityList, this);
        } else {
            return readValueRange(entity);
        }
    }

    @Override
    public <T> ValueRange<T> extractValueRange(Solution_ solution) {
        return extractValueRange(solution, null);
    }

    @Override
    public long extractValueRangeSize(Solution_ solution) {
        return extractValueRangeSize(solution, null);
    }

    @Override
    public boolean canExtractValueRangeFromSolution() {
        return false;
    }
}
