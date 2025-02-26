package ai.timefold.solver.core.impl.domain.variable.declarative;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class IdGraphNavigator<Entity_> implements GraphNavigator<Entity_, Entity_> {
    final VariableId variableId;

    public IdGraphNavigator(Class<? extends Entity_> entityClass) {
        this.variableId = VariableId.entity(entityClass);
    }

    @Override
    public @NonNull Entity_ getValueEdge(Entity_ entity) {
        return entity;
    }

    @Override
    public VariableId getVariableId() {
        return variableId;
    }
}
