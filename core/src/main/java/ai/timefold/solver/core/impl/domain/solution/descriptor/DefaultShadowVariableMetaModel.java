package ai.timefold.solver.core.impl.domain.solution.descriptor;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.ShadowVariableMetaModel;

import org.jspecify.annotations.NonNull;

public record DefaultShadowVariableMetaModel<Solution_, Entity_, Value_>(
        PlanningEntityMetaModel<Solution_, Entity_> entity,
        ShadowVariableDescriptor<Solution_> variableDescriptor)
        implements
            ShadowVariableMetaModel<Solution_, Entity_, Value_>,
            InnerVariableMetaModel<Solution_> {

    @SuppressWarnings("unchecked")
    @Override
    public @NonNull Class<Value_> type() {
        return (Class<Value_>) variableDescriptor.getVariablePropertyType();
    }

    @Override
    public @NonNull String name() {
        return variableDescriptor.getVariableName();
    }

    @Override
    public ShadowVariableDescriptor<Solution_> variableDescriptor() {
        return variableDescriptor;
    }

    @Override
    public String toString() {
        return "Shadow Variable '%s %s.%s'"
                .formatted(type(), entity.getClass().getSimpleName(), name());
    }

}
