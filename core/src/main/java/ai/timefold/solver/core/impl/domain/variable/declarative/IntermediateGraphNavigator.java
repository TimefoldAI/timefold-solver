package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.IdentityHashMap;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class IntermediateGraphNavigator<Entity_, Value_> implements GraphNavigator<Entity_, Value_> {
    final Class<? extends Value_> type;
    final String intermediateName;
    final VariableId variableId;
    final IdentityHashMap<Entity_, Value_> intermediateValueMap;

    public IntermediateGraphNavigator(VariableId parentVariableId,
            Class<? extends Value_> type,
            String intermediateName,
            IdentityHashMap<Entity_, Value_> intermediateValueMap) {
        this.variableId = parentVariableId.child(
                DefaultShadowVariableFactory.getIntermediateVariableName(intermediateName));
        this.type = type;
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

    public Class<? extends Value_> getType() {
        return type;
    }

    public String getIntermediateName() {
        return intermediateName;
    }
}
