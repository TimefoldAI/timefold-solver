package ai.timefold.solver.core.impl.domain.variable.declarative;

import org.jspecify.annotations.Nullable;

public record VariableId(Class<?> entityClass, String variableName, @Nullable VariableId parentVariableId) {
    public VariableId(Class<?> entityClass, String variableName) {
        this(entityClass, variableName, null);
    }

    @Override
    public String toString() {
        return entityClass.getSimpleName() + "." + variableName;
    }
}
