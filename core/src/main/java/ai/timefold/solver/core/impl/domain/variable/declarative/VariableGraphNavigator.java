package ai.timefold.solver.core.impl.domain.variable.declarative;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class VariableGraphNavigator<Entity_, Value_> implements GraphNavigator<Entity_, Value_> {
    final VariableId variableId;
    VariableDescriptor<?> variableDescriptor;

    public VariableGraphNavigator(@Nullable final VariableId parentVariableId,
            VariableDescriptor<?> variableDescriptor) {
        if (parentVariableId != null) {
            variableId = parentVariableId.child(
                    variableDescriptor.getVariableName());
        } else {
            variableId = new VariableId(variableDescriptor.getEntityDescriptor().getEntityClass(),
                    variableDescriptor.getVariableName());
        }
        this.variableDescriptor = variableDescriptor;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable Value_ getValueEdge(Entity_ entity) {
        return (Value_) variableDescriptor.getValue(entity);
    }

    @Override
    public VariableId getVariableId() {
        return variableId;
    }
}
