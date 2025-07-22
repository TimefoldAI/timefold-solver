package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.entity.AllEntitiesListValueRange;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class FromEntityPropertyValueRangeDescriptor<Solution_>
        extends AbstractFromPropertyValueRangeDescriptor<Solution_>
        implements IterableValueRangeDescriptor<Solution_> {

    public FromEntityPropertyValueRangeDescriptor(GenuineVariableDescriptor<Solution_> variableDescriptor,
            boolean acceptNullInValueRange, MemberAccessor memberAccessor) {
        super(variableDescriptor, acceptNullInValueRange, memberAccessor);
    }

    @Override
    public <T> ValueRange<T> extractValueRange(Solution_ solution, Object entity) {
        if (entity == null) {
            var entityList = variableDescriptor.getEntityDescriptor().extractEntities(solution);
            return new AllEntitiesListValueRange<>(entityList, this);
        } else {
            return readValueRange(entity);
        }
    }

    @Override
    public <T> ValueRange<T> extractValueRange(Solution_ solution) {
        return extractValueRange(solution, null);
    }

    @Override
    public boolean canExtractValueRangeFromSolution() {
        return false;
    }
}
