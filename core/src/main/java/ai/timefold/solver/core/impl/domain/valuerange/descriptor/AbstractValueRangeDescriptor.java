package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;

import org.jspecify.annotations.NullMarked;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public abstract sealed class AbstractValueRangeDescriptor<Solution_>
        implements ValueRangeDescriptor<Solution_>
        permits AbstractFromPropertyValueRangeDescriptor, CompositeValueRangeDescriptor {

    private final int ordinalId;
    protected final GenuineVariableDescriptor<Solution_> variableDescriptor;

    protected AbstractValueRangeDescriptor(int ordinalId, GenuineVariableDescriptor<Solution_> variableDescriptor) {
        this.ordinalId = ordinalId;
        this.variableDescriptor = variableDescriptor;
    }

    @Override
    public int getOrdinalId() {
        return ordinalId;
    }

    @Override
    public GenuineVariableDescriptor<Solution_> getVariableDescriptor() {
        return variableDescriptor;
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
