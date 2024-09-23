package ai.timefold.solver.core.impl.domain.solution.descriptor;

import ai.timefold.solver.core.api.domain.metamodel.BasicVariableMetaModel;
import ai.timefold.solver.core.api.domain.metamodel.EntityMetaModel;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;

public record DefaultBasicVariableMetaModel<Solution_, Entity_, Value_>(EntityMetaModel<Solution_, Entity_> entity,
        BasicVariableDescriptor<Solution_> variableDescriptor)
        implements
            BasicVariableMetaModel<Solution_, Entity_, Value_> {

    @Override
    public boolean allowsUnassigned() {
        return variableDescriptor.allowsUnassigned();
    }

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
