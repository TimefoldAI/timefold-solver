package ai.timefold.solver.core.impl.domain.solution.descriptor;

import ai.timefold.solver.core.api.domain.metamodel.EntityMetaModel;
import ai.timefold.solver.core.api.domain.metamodel.ListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;

record DefaultListVariableMetaModel<Solution_, Entity_, Value_>(EntityMetaModel<Solution_, Entity_> entity,
        ListVariableDescriptor<Solution_> variableDescriptor)
        implements
            ListVariableMetaModel<Solution_, Entity_, Value_> {

    @SuppressWarnings("unchecked")
    @Override
    public Class<Value_> type() {
        return (Class<Value_>) variableDescriptor.getElementType();
    }

    @Override
    public String name() {
        return variableDescriptor.getVariableName();
    }

    @Override
    public boolean allowsUnassignedValues() {
        return false;
    }

}
