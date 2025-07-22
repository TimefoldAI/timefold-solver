package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.entity.AllEntitiesListValueRange;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;

import org.jspecify.annotations.NullMarked;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public final class FromEntityPropertyValueRangeDescriptor<Solution_>
        extends AbstractFromPropertyValueRangeDescriptor<Solution_>
        implements ValueRangeDescriptor<Solution_> {

    public FromEntityPropertyValueRangeDescriptor(GenuineVariableDescriptor<Solution_> variableDescriptor,
            boolean acceptNullInValueRange, MemberAccessor memberAccessor) {
        super(variableDescriptor, acceptNullInValueRange, memberAccessor);
    }

    @Override
    public <T> ValueRange<T> extractAllValues(Solution_ solution) {
        var entityList = variableDescriptor.getEntityDescriptor().extractEntities(solution);
        return new AllEntitiesListValueRange<>(solution, entityList, this);
    }

    @Override
    public <T> ValueRange<T> extractValuesFromEntity(Solution_ solution, Object entity) {
        return readValueRange(entity);
    }

    @Override
    public boolean canExtractValueRangeFromSolution() {
        return false;
    }
}
