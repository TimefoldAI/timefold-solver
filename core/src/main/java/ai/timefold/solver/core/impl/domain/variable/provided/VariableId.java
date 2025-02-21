package ai.timefold.solver.core.impl.domain.variable.provided;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

import org.jspecify.annotations.Nullable;

public record VariableId(Class<?> entityClass, String variableName, @Nullable VariableId parentVariableId) {
    public VariableId(Class<?> entityClass, String variableName) {
        this(entityClass, variableName, null);
    }

    public static VariableId entity(Class<?> entityClass) {
        return new VariableId(entityClass, DefaultShadowVariableFactory.IDENTITY, null);
    }

    public static <Solution_> VariableId of(VariableDescriptor<Solution_> variableDescriptor) {
        var entityClass = variableDescriptor.getEntityDescriptor().getEntityClass();
        return entity(entityClass).child(entityClass, variableDescriptor.getVariableName());
    }

    public VariableId rootId() {
        return entity(entityClass).child(entityClass, variableName.substring(variableName.lastIndexOf('.') + 1));
    }

    public VariableId child(Class<?> entityClass, String childVariableName) {
        return new VariableId(entityClass, variableName + "." + childVariableName, this);
    }

    public VariableId group(Class<?> elementClass, int group) {
        return new VariableId(elementClass, variableName + ".group(%d)".formatted(group), this);
    }

    public VariableId previous() {
        return child(entityClass, DefaultShadowVariableFactory.PREVIOUS);
    }

    public VariableId next() {
        return child(entityClass, DefaultShadowVariableFactory.NEXT);
    }

    public VariableId inverse() {
        return child(entityClass, DefaultShadowVariableFactory.INVERSE);
    }

    @Override
    public String toString() {
        return entityClass.getSimpleName() + "." + variableName;
    }
}
