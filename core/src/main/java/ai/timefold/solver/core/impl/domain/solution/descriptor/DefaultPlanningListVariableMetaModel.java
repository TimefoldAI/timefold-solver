package ai.timefold.solver.core.impl.domain.solution.descriptor;

import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record DefaultPlanningListVariableMetaModel<Solution_, Entity_, Value_>(
        PlanningEntityMetaModel<Solution_, Entity_> entity,
        ListVariableDescriptor<Solution_> variableDescriptor)
        implements
            PlanningListVariableMetaModel<Solution_, Entity_, Value_>,
            InnerVariableMetaModel<Solution_> {

    @SuppressWarnings("unchecked")
    @Override
    public Class<Value_> type() {
        return (Class<Value_>) variableDescriptor.getElementType();
    }

    @Override
    public String name() {
        return variableDescriptor.getVariableName();
    }

    @Override
    public boolean allowsUnassignedValues() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        // Do not use entity in equality checks;
        // If an entity is subclassed, that subclass will have it
        // own distinct VariableMetaModel
        if (o instanceof DefaultPlanningListVariableMetaModel<?, ?, ?> that) {
            return Objects.equals(variableDescriptor, that.variableDescriptor);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableDescriptor);
    }

    @Override
    public String toString() {
        return "Genuine List Variable '%s %s.%s' (allowsUnassignedValues: %b)"
                .formatted(type(), entity.getClass().getSimpleName(), name(), allowsUnassignedValues());
    }

}
