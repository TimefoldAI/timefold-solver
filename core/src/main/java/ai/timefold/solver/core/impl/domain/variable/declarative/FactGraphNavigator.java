package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.function.Function;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class FactGraphNavigator<Entity_, Value_> implements GraphNavigator<Entity_, Value_> {
    final VariableId variableId;
    final Class<?> entityClass;
    final Function<Entity_, Value_> factMapper;

    public FactGraphNavigator(VariableId parentVariableId,
            Class<?> entityClass,
            Function<Entity_, Value_> factMapper) {
        this.variableId = parentVariableId.child(entityClass,
                DefaultShadowVariableFactory.FACT);
        this.entityClass = entityClass;
        this.factMapper = factMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable Value_ getValueEdge(Entity_ entity) {
        return factMapper.apply(entity);
    }

    @Override
    public VariableId getVariableId() {
        return variableId;
    }
}
