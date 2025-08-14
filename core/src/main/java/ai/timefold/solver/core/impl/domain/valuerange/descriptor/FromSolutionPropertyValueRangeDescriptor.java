package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;

import org.jspecify.annotations.NullMarked;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public final class FromSolutionPropertyValueRangeDescriptor<Solution_>
        extends AbstractFromPropertyValueRangeDescriptor<Solution_> {

    public FromSolutionPropertyValueRangeDescriptor(int ordinal, GenuineVariableDescriptor<Solution_> variableDescriptor,
            MemberAccessor memberAccessor) {
        super(ordinal, variableDescriptor, memberAccessor);
    }

    @Override
    public <T> ValueRange<T> extractAllValues(Solution_ solution) {
        return readValueRange(solution);
    }

    @Override
    public <T> ValueRange<T> extractValuesFromEntity(Solution_ solution, Object entity) {
        return readValueRange(solution); // Needed for composite ranges on solution and on entity.
    }

    @Override
    public boolean canExtractValueRangeFromSolution() {
        return true;
    }

}
