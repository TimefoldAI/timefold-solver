package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record EntityVariablePair<Solution_>(Object entity, List<VariableUpdaterInfo<Solution_>> variableReferences,
        int graphNodeId, int entityId, @Nullable int[] groupEntityIds) {
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
        return entity + ":" + variableReferences.stream().map(VariableUpdaterInfo::id).toList();
    }
}
