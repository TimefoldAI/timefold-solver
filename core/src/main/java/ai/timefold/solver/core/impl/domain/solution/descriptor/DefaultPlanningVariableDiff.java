package ai.timefold.solver.core.impl.domain.solution.descriptor;

import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningEntityDiff;
import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningVariableDiff;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
record DefaultPlanningVariableDiff<Solution_, Entity_, Value_>(PlanningEntityDiff<Solution_, Entity_> entityDiff,
        VariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ oldValue, Value_ newValue)
        implements
            PlanningVariableDiff<Solution_, Entity_, Value_> {

    DefaultPlanningVariableDiff {
        Objects.requireNonNull(entityDiff);
        Objects.requireNonNull(variableMetaModel);
    }

    @Override
    public String toString() {
        return """
                Difference between two solutions in a %s planning variable (%s) of a planning entity (%s) of type (%s):
                  Old value: %s
                  New value: %s
                """
                .formatted(variableMetaModel.isGenuine() ? "genuine" : "shadow", variableMetaModel.name(),
                        entityDiff.entity(), entityDiff.entityMetaModel().type(), oldValue, newValue);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof DefaultPlanningVariableDiff<?, ?, ?> that)) {
            return false;
        }
        return Objects.equals(variableMetaModel, that.variableMetaModel)
                && Objects.equals(entityDiff, that.entityDiff);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableMetaModel, entityDiff);
    }
}
