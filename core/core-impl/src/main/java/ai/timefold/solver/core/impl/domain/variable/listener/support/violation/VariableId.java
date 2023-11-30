package ai.timefold.solver.core.impl.domain.variable.listener.support.violation;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

public record VariableId<Solution_>(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
}
