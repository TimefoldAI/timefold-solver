package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.IdentityHashMap;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class IntermediateGraphNavigator<Entity_, Value_> implements GraphNavigator<Entity_, Value_> {
    final Class<? extends Entity_> entityType;
    final Class<? extends Value_> valueType;
    final String intermediateName;
    final VariableId variableId;
    final IdentityHashMap<Entity_, Value_> intermediateValueMap;

    public IntermediateGraphNavigator(VariableId parentVariableId,
            Class<? extends Entity_> entityType,
            Class<? extends Value_> valueType,
            String intermediateName,
            IdentityHashMap<Entity_, Value_> intermediateValueMap) {
        this.variableId = parentVariableId.child(
                DefaultShadowVariableFactory.getIntermediateVariableName(intermediateName));
        this.entityType = entityType;
        this.valueType = valueType;
        this.intermediateName = intermediateName;
        this.intermediateValueMap = intermediateValueMap;
    }

    @Override
    public @Nullable Value_ getValueEdge(Entity_ entity) {
        return intermediateValueMap.get(entity);
    }

    @Override
    public VariableId getVariableId() {
        return variableId;
    }

    public VariableId getRootVariableId() {
        return new VariableId(entityType, DefaultShadowVariableFactory.getIntermediateVariableName(intermediateName));
    }

    public Class<? extends Entity_> getEntityType() {
        return entityType;
    }

    public Class<? extends Value_> getValueType() {
        return valueType;
    }

    public String getIntermediateName() {
        return intermediateName;
    }
}
