package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.entity.FromEntityListValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.cache.CacheableValueRange;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;

public class FromListVarEntityPropertyValueRangeDescriptor<Solution_>
        extends FromEntityPropertyValueRangeDescriptor<Solution_> implements EntityIndependentValueRangeDescriptor<Solution_> {

    private final ListVariableDescriptor<Solution_> listVariableDescriptor;

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
    public boolean isEntityIndependent() {
        return true;
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
    public <Value_> ValueRange<Value_> extractValueRange(Solution_ solution, Object entity) {
        if (entity == null) {
            var entityList = listVariableDescriptor.getEntityDescriptor().extractEntities(solution);
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
}
