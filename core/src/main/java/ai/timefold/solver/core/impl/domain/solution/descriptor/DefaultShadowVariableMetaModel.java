package ai.timefold.solver.core.impl.domain.solution.descriptor;

import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.ShadowVariableMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record DefaultShadowVariableMetaModel<Solution_, Entity_, Value_>(
        PlanningEntityMetaModel<Solution_, Entity_> entity,
        ShadowVariableDescriptor<Solution_> variableDescriptor)
        implements
            ShadowVariableMetaModel<Solution_, Entity_, Value_>,
            InnerVariableMetaModel<Solution_> {

    @SuppressWarnings("unchecked")
    @Override
    public Class<Value_> type() {
        return (Class<Value_>) variableDescriptor.getVariablePropertyType();
    }

    @Override
    public String name() {
        return variableDescriptor.getVariableName();
    }

    @Override
    public ShadowVariableDescriptor<Solution_> variableDescriptor() {
        return variableDescriptor;
    }

    @Override
    public boolean equals(Object o) {
        // Do not use entity in equality checks;
        // If an entity is subclassed, that subclass will have it
        // own distinct VariableMetaModel
        if (o instanceof DefaultShadowVariableMetaModel<?, ?, ?> that) {
            return Objects.equals(variableDescriptor, that.variableDescriptor);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(variableDescriptor);
    }

    @Override
    public String toString() {
        return "Shadow Variable '%s %s.%s'"
                .formatted(type(), entity.type().getSimpleName(), name());
    }

}
