package ai.timefold.solver.core.impl.domain.variable.provided;

import java.util.function.Function;

import org.jspecify.annotations.NonNull;

public class FactGraphNavigator<Entity_, Value_> implements GraphNavigator<Entity_, Value_> {
    @NonNull
    final VariableId variableId;

    @NonNull
    final Class<?> entityClass;

    @NonNull
    final Function<Entity_, Value_> factMapper;

    public FactGraphNavigator(VariableId parentVariableId,
            @NonNull Class<?> entityClass,
            @NonNull Function<Entity_, Value_> factMapper) {
        this.variableId = parentVariableId.child(entityClass,
                DefaultShadowVariableFactory.FACT);
        this.entityClass = entityClass;
        this.factMapper = factMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Value_ getValueEdge(Entity_ entity) {
        return factMapper.apply(entity);
    }

    @Override
    public VariableId getVariableId() {
        return variableId;
    }
}
