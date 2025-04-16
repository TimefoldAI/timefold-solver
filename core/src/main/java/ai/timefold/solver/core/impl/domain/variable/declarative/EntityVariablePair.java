package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Objects;

public record EntityVariablePair(Object entity, VariableId variableId,
        VariableUpdaterInfo variableReference, int graphNodeId) {
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof EntityVariablePair that))
            return false;
        return graphNodeId == that.graphNodeId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(graphNodeId);
    }

    @Override
    public String toString() {
        return entity + ":" + variableId;
    }
}
