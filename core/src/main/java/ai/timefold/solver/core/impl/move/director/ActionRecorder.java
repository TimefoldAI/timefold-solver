package ai.timefold.solver.core.impl.move.director;

import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

public sealed interface ActionRecorder<Solution_> permits DefaultActionRecorder {

    void recordVariableChangeAction(VariableDescriptor<Solution_> variableDescriptor, Object entity, Object value);

    void recordListVariableBeforeChangeAction(ListVariableDescriptor<Solution_> variableDescriptor, Object entity,
            List<Object> values, int fromIndex, int toIndex);

    void recordListVariableAfterChangeAction(ListVariableDescriptor<Solution_> variableDescriptor, Object entity, int fromIndex,
            int toIndex);

    void recordListVariableBeforeAssignmentAction(ListVariableDescriptor<Solution_> variableDescriptor, Object element);

    void recordListVariableAfterAssignmentAction(ListVariableDescriptor<Solution_> variableDescriptor, Object element);

    void recordListVariableBeforeUnassignmentAction(ListVariableDescriptor<Solution_> variableDescriptor, Object element);

    void recordListVariableAfterUnassignmentAction(ListVariableDescriptor<Solution_> variableDescriptor, Object element);
}
