package ai.timefold.solver.core.impl.domain.solution.descriptor;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;

import org.jspecify.annotations.NonNull;

public record DefaultPlanningListVariableMetaModel<Solution_, Entity_, Value_>(
        PlanningEntityMetaModel<Solution_, Entity_> entity,
        ListVariableDescriptor<Solution_> variableDescriptor)
        implements
            PlanningListVariableMetaModel<Solution_, Entity_, Value_>,
            InnerVariableMetaModel<Solution_> {

    @SuppressWarnings("unchecked")
    @Override
    public @NonNull Class<Value_> type() {
        return (Class<Value_>) variableDescriptor.getElementType();
    }

    @Override
    public @NonNull String name() {
        return variableDescriptor.getVariableName();
    }

    @Override
    public boolean allowsUnassignedValues() {
        return false;
    }

    @Override
    public String toString() {
        return "Genuine List Variable '%s %s.%s' (allowsUnassignedValues: %b)"
                .formatted(type(), entity.getClass().getSimpleName(), name(), allowsUnassignedValues());
    }

}
