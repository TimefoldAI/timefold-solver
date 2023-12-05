package ai.timefold.solver.core.impl.domain.variable.listener.support.violation;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

/**
 * A {@link VariableId} is an entity/variable of a given solution.
 * {@link VariableId} cannot be compared across different solution instances,
 * since variableDescriptor and entity are compared by reference equality.
 *
 * @param variableDescriptor The variable this {@link VariableId} refers to.
 * @param entity The entity this {@link VariableId} refers to.
 */
public record VariableId<Solution_>(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
    @Override
    public boolean equals(Object other) {
        if (other instanceof VariableId<?> variableId) {
            return variableDescriptor == variableId.variableDescriptor &&
                    entity == variableId.entity;
        }
        return false;
    }
}
