package ai.timefold.solver.core.impl.domain.solution.descriptor;

import ai.timefold.solver.core.api.domain.metamodel.BasicVariableMetaModel;
import ai.timefold.solver.core.api.domain.metamodel.EntityMetaModel;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;

public record DefaultBasicVariableMetaModel<Solution_, Entity_, Value_>(EntityMetaModel<Solution_, Entity_> entity,
        BasicVariableDescriptor<Solution_> variableDescriptor)
        implements
            BasicVariableMetaModel<Solution_, Entity_, Value_> {

    @SuppressWarnings("unchecked")
    @Override
    public Class<Value_> type() {
        return (Class<Value_>) variableDescriptor.getVariablePropertyType();
    }

    @Override
    public String name() {
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
