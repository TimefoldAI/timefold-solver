package ai.timefold.solver.core.impl.domain.variable.provided;

import java.util.List;

public interface GraphNavigator<Entity_, Value_> {
    Value_ getValueEdge(Entity_ entity);

    VariableId getVariableId();

    default boolean hasParentEdge() {
        return false;
    }

    default List<Entity_> getParentEdges(Value_ entity) {
        throw new UnsupportedOperationException("%s instances do not support getParent."
                .formatted(getClass().getSimpleName()));
    }

    default VariableId getParentVariableId() {
        throw new UnsupportedOperationException("%s instances do not support getParentVariableId."
                .formatted(getClass().getSimpleName()));
    }
}
