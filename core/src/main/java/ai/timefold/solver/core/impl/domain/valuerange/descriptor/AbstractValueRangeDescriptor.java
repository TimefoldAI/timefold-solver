package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class AbstractValueRangeDescriptor<Solution_> implements ValueRangeDescriptor<Solution_> {

    protected final GenuineVariableDescriptor<Solution_> variableDescriptor;
    protected final boolean acceptNullInValueRange;

    protected AbstractValueRangeDescriptor(GenuineVariableDescriptor<Solution_> variableDescriptor,
            boolean acceptNullInValueRange) {
        this.variableDescriptor = variableDescriptor;
        this.acceptNullInValueRange = acceptNullInValueRange;
    }

    @Override
    public GenuineVariableDescriptor<Solution_> getVariableDescriptor() {
        return variableDescriptor;
    }

    @Override
    public boolean acceptNullInValueRange() {
        return acceptNullInValueRange;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean mightContainEntity() {
        SolutionDescriptor<Solution_> solutionDescriptor = variableDescriptor.getEntityDescriptor().getSolutionDescriptor();
        Class<?> variablePropertyType = variableDescriptor.getVariablePropertyType();
        for (Class<?> entityClass : solutionDescriptor.getEntityClassSet()) {
            if (variablePropertyType.isAssignableFrom(entityClass)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + variableDescriptor.getVariableName() + ")";
    }

}
