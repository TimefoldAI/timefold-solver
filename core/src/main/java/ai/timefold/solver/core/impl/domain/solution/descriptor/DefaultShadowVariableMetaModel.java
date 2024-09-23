package ai.timefold.solver.core.impl.domain.solution.descriptor;

import ai.timefold.solver.core.api.domain.metamodel.EntityMetaModel;
import ai.timefold.solver.core.api.domain.metamodel.ShadowVariableMetaModel;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;

public record DefaultShadowVariableMetaModel<Solution_, Entity_, Value_>(EntityMetaModel<Solution_, Entity_> entity,
        ShadowVariableDescriptor<Solution_> variableDescriptor)
        implements
            ShadowVariableMetaModel<Solution_, Entity_, Value_> {

    @SuppressWarnings("unchecked")
    @Override
    public Class<Value_> type() {
        return (Class<Value_>) variableDescriptor.getVariablePropertyType();
    }

    @Override
    public String name() {
        return variableDescriptor.getVariableName();
    }
}
