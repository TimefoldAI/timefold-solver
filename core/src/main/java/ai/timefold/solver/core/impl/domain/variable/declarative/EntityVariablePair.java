package ai.timefold.solver.core.impl.domain.variable.declarative;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record EntityVariablePair<Solution_>(Object entity, VariableUpdaterInfo<Solution_> variableReference, int graphNodeId) {
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof EntityVariablePair<?> that))
            return false;
        return graphNodeId == that.graphNodeId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(graphNodeId);
    }

    @Override
    public String toString() {
        return entity + ":" + variableReference.id();
    }
}
