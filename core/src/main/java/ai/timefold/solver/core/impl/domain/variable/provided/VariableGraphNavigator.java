package ai.timefold.solver.core.impl.domain.variable.provided;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class VariableGraphNavigator<Entity_, Value_> implements GraphNavigator<Entity_, Value_> {
    @NonNull
    final VariableId variableId;

    @NonNull
    VariableDescriptor<?> variableDescriptor;

    public VariableGraphNavigator(@Nullable final VariableId parentVariableId,
            @NonNull VariableDescriptor<?> variableDescriptor) {
        if (parentVariableId != null) {
            variableId = parentVariableId.child(variableDescriptor.getEntityDescriptor().getEntityClass(),
                    variableDescriptor.getVariableName());
        } else {
            variableId = new VariableId(variableDescriptor.getEntityDescriptor().getEntityClass(),
                    variableDescriptor.getVariableName());
        }
        this.variableDescriptor = variableDescriptor;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Value_ getValueEdge(Entity_ entity) {
        return (Value_) variableDescriptor.getValue(entity);
    }

    @Override
    public VariableId getVariableId() {
        return variableId;
    }
}
