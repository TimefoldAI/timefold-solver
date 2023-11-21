package ai.timefold.solver.core.impl.domain.variable.listener.support.violation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.util.Pair;

public class VariableSnapshot<Solution_> {
    private final VariableDescriptor<Solution_> variableDescriptor;
    private final Object entity;
    private final Object value;
    private final Pair<VariableDescriptor<Solution_>, Object> variableId;

    public VariableSnapshot(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        this.variableDescriptor = variableDescriptor;
        this.entity = entity;
        this.variableId = new Pair<>(variableDescriptor, entity);

        // If it is a genuine list variable, we need to create a copy
        // of the value in order to create a snapshot (since the contents
        // of the list change instead of the list itself).
        this.value = variableDescriptor.isGenuineListVariable() ? new ArrayList<>((List<?>) variableDescriptor.getValue(entity))
                : variableDescriptor.getValue(entity);
    }

    public VariableDescriptor<Solution_> getVariableDescriptor() {
        return variableDescriptor;
    }

    public Object getEntity() {
        return entity;
    }

    public Object getValue() {
        return value;
    }

    public Pair<VariableDescriptor<Solution_>, Object> getVariableId() {
        return variableId;
    }

    public boolean isDifferentFrom(VariableSnapshot<Solution_> other) {
        return !Objects.equals(value, other.value);
    }
}
