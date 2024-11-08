package ai.timefold.solver.core.impl.domain.solution.descriptor;

import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;

import org.jspecify.annotations.NonNull;

public record DefaultPlanningVariableMetaModel<Solution_, Entity_, Value_>(
        PlanningEntityMetaModel<Solution_, Entity_> entity,
        BasicVariableDescriptor<Solution_> variableDescriptor)
        implements
            PlanningVariableMetaModel<Solution_, Entity_, Value_>,
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
    public boolean allowsUnassigned() {
        return variableDescriptor.allowsUnassigned();
    }

    @Override
    public boolean isChained() {
        return variableDescriptor.isChained();
    }

    @Override
    public String toString() {
        return "Genuine Variable '%s %s.%s' (allowsUnassigned: %b, isChained: %b)"
                .formatted(type(), entity.getClass().getSimpleName(), name(), allowsUnassigned(), isChained());
    }
}
