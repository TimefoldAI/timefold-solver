package ai.timefold.solver.core.impl.domain.variable.listener.support.violation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

/**
 * A {@link VariableSnapshot} is a snapshot of the value of a variable for a given entity.
 * Only {@link VariableSnapshot} from the same solution instance can be compared.
 *
 * @param variableId The entity/variable pair that is recorded.
 * @param value The recorded value of the variable for the given entity.
 * @param <Solution_>
 */
public record VariableSnapshot<Solution_>(VariableId<Solution_> variableId, Object value) {

    public VariableSnapshot(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        this(new VariableId<>(variableDescriptor, entity),
                variableDescriptor.isListVariable() ? new ArrayList<>((List<?>) variableDescriptor.getValue(entity))
                        : variableDescriptor.getValue(entity));
    }

    public VariableDescriptor<Solution_> getVariableDescriptor() {
        return variableId.variableDescriptor();
    }

    public Object getEntity() {
        return variableId.entity();
    }

    public Object getValue() {
        return value;
    }

    public VariableId<Solution_> getVariableId() {
        return variableId;
    }

    public void restore() {
        variableId.variableDescriptor().setValue(variableId.entity(), value);
    }

    public boolean isDifferentFrom(VariableSnapshot<Solution_> other) {
        if (!Objects.equals(variableId, other.variableId)) {
            throw new IllegalArgumentException(
                    "The variable/entity pair for the other snapshot (%s) differs from the variable/entity pair for this snapshot (%s)."
                            .formatted(other.variableId, this.variableId));
        }
        return !Objects.equals(value, other.value);
    }
}
