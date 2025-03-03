package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface GraphNavigator<Entity_, Value_> {
    @Nullable
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
