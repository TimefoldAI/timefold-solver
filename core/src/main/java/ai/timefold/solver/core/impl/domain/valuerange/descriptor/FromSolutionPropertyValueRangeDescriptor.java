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
        return readValueRangeForSolution(solution);
    }

    @Override
    public <T> ValueRange<T> extractValuesFromEntity(Solution_ solution, Object entity) {
        return readValueRangeForSolution(solution); // Needed for composite ranges on solution and on entity.
    }

    @Override
    public long getValueRangeSize(Solution_ solution, Object entity) {
        return getValueRangeSize(solution);
    }

    /**
     * Counts the values of the solution's value range, without the null value,
     * with the same limitations as {@link #getValueRangeSize(Object, Object)}.
     * Unlike value ranges from entities, the solution's value range isn't deduplicated when built,
     * so this is also the size of the built value range.
     */
    public long getValueRangeSize(Solution_ solution) {
        return countValues(memberAccessor.executeGetter(solution), solution);
    }

    @Override
    public boolean canExtractValueRangeFromSolution() {
        return true;
    }

}
