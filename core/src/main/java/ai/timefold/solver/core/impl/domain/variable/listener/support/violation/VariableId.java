package ai.timefold.solver.core.impl.domain.variable.listener.support.violation;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

/**
 * A {@link VariableId} is an entity/variable of a given solution.
 * {@link VariableId} cannot be compared across different solution instances,
 * since variableDescriptor and entity are compared by reference equality.
 * <p>
 * Note: The entity is compared using {@link Object#equals(Object)} and {@link Object#hashCode()},
 * so it's important that the planning entity class properly overrides both methods,
 * typically based on a unique identifier (e.g. a UId).
 * <p>
 * @param variableDescriptor The variable this {@link VariableId} refers to.
 * @param entity The entity this {@link VariableId} refers to.
 */
public record VariableId<Solution_>(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
    @Override
    public boolean equals(Object other) {
        if (other instanceof VariableId<?> variableId) {
            return variableDescriptor.equals(variableId.variableDescriptor) &&
                    entity.equals(variableId.entity);
        }
        return false;
    }
}
