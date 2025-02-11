package ai.timefold.solver.core.impl.domain.variable.provided;

import org.jspecify.annotations.Nullable;

public record VariableId(Class<?> entityClass, String variableName, @Nullable VariableId parentVariableId) {
    public VariableId(Class<?> entityClass, String variableName) {
        this(entityClass, variableName, null);
    }

    public static VariableId entity(Class<?> entityClass) {
        return new VariableId(entityClass, DefaultShadowVariableFactory.IDENTITY, null);
    }

    public VariableId rootId() {
        return entity(entityClass).child(entityClass, variableName.substring(variableName.lastIndexOf('.') + 1));
    }

    public VariableId child(Class<?> entityClass, String childVariableName) {
        return new VariableId(entityClass, variableName + "." + childVariableName, this);
    }

    @Override
    public String toString() {
        return entityClass.getSimpleName() + "." + variableName;
    }
}
