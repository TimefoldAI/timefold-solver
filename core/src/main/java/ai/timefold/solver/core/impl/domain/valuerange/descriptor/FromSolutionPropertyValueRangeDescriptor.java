package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class FromSolutionPropertyValueRangeDescriptor<Solution_>
        extends AbstractFromPropertyValueRangeDescriptor<Solution_>
        implements IterableValueRangeDescriptor<Solution_> {

    public FromSolutionPropertyValueRangeDescriptor(
            GenuineVariableDescriptor<Solution_> variableDescriptor, boolean acceptNullInValueRange,
            MemberAccessor memberAccessor) {
        super(variableDescriptor, acceptNullInValueRange, memberAccessor);
    }

    @Override
    public <Value_> ValueRange<Value_> extractValueRange(Solution_ solution, Object entity) {
        return readValueRange(solution);
    }

    @Override
    public long extractValueRangeSize(Solution_ solution, Object entity) {
        return readValueRangeSize(solution);
    }

    @Override
    public <T> ValueRange<T> extractValueRange(Solution_ solution) {
        return readValueRange(solution);
    }

    @Override
    public long extractValueRangeSize(Solution_ solution) {
        return readValueRangeSize(solution);
    }

    @Override
    public boolean canExtractValueRangeFromSolution() {
        return true;
    }

}
