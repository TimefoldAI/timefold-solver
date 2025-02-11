package ai.timefold.solver.core.impl.domain.variable.provided;

import java.util.Objects;

public record EntityVariableOrFactReference(VariableId variableId, Object entity,
        AbstractVariableReference<?, ?> variableReference, int id) {
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof EntityVariableOrFactReference that))
            return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return entity + ":" + variableId;
    }
}
