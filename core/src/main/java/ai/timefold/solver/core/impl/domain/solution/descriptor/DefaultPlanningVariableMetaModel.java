package ai.timefold.solver.core.impl.domain.solution.descriptor;

import java.util.Comparator;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record DefaultPlanningVariableMetaModel<Solution_, Entity_, Value_>(
        GenuineEntityMetaModel<Solution_, Entity_> entity,
        BasicVariableDescriptor<Solution_> variableDescriptor)
        implements
            PlanningVariableMetaModel<Solution_, Entity_, Value_>,
            InnerGenuineVariableMetaModel<Solution_> {

    static final Comparator<VariableMetaModel<?, ?, ?>> VARIABLE_META_MODEL_COMPARATOR =
            Comparator.comparing((VariableMetaModel<?, ?, ?> variableMetaModel) -> variableMetaModel.entity())
                    .thenComparingInt(
                            (VariableMetaModel<?, ?, ?> variableMetaModel) -> ((InnerVariableMetaModel<?>) variableMetaModel)
                                    .variableDescriptor().getOrdinal());

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
    public boolean allowsUnassigned() {
        return variableDescriptor.allowsUnassigned();
    }

    @Override
    public boolean isValueRangeOnSolution() {
        return variableDescriptor.canExtractValueRangeFromSolution();
    }

    @Override
    public boolean equals(Object o) {
        // Do not use entity in equality checks;
        // If an entity is subclassed,
        // that subclass will have it own distinct VariableMetaModel
        if (o instanceof DefaultPlanningVariableMetaModel<?, ?, ?> that) {
            return Objects.equals(variableDescriptor, that.variableDescriptor);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableDescriptor);
    }

    @Override
    public int compareTo(VariableMetaModel<Solution_, Entity_, Value_> other) {
        return VARIABLE_META_MODEL_COMPARATOR.compare(this, other);
    }

    @Override
    public String toString() {
        return "Genuine Variable '%s %s.%s' (allowsUnassigned: %b)"
                .formatted(type(), entity.getClass().getSimpleName(), name(), allowsUnassigned());
    }

}
