package ai.timefold.solver.core.impl.domain.solution.descriptor;

import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningEntityDiff;
import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningVariableDiff;

record DefaultPlanningVariableDiff<Solution_, Entity_, Value_>(PlanningEntityDiff<Solution_, Entity_> entityDiff,
        GenuineVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ oldValue, Value_ newValue)
        implements
            PlanningVariableDiff<Solution_, Entity_, Value_> {

    DefaultPlanningVariableDiff {
        Objects.requireNonNull(entityDiff);
        Objects.requireNonNull(variableMetaModel);
    }

    @Override
    public String toString() {
        return """
                Difference between two solutions in a planning variable (%s) of a planning entity (%s) of type (%s):
                  Old value: %s
                  New value: %s
                """.formatted(variableMetaModel.name(), entityDiff.entity(), entityDiff.entityMetaModel().type(), oldValue,
                newValue);
    }
}
