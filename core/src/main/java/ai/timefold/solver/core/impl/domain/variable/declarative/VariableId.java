package ai.timefold.solver.core.impl.domain.variable.declarative;

import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.Nullable;

public record VariableId(Class<?> entityClass, String variableName, @Nullable VariableId parentVariableId) {
    public VariableId(Class<?> entityClass, String variableName) {
        this(entityClass, variableName, null);
    }

    public VariableId(VariableMetaModel<?, ?, ?> metaModel) {
        this(metaModel.entity().type(), metaModel.name(), null);
    }

    @Override
    public String toString() {
        return entityClass.getSimpleName() + "." + variableName;
    }
}
