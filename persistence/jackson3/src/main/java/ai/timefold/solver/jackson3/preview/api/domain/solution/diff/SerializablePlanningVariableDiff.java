package ai.timefold.solver.jackson3.preview.api.domain.solution.diff;

import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningVariableDiff;

import com.fasterxml.jackson.annotation.JsonProperty;

record SerializablePlanningVariableDiff<Value_>(
        @JsonProperty("name") String variableName,
        @JsonProperty("old_value") Value_ oldValue,
        @JsonProperty("new_value") Value_ newValue) {

    public static <Value_> SerializablePlanningVariableDiff<Value_> of(PlanningVariableDiff<?, ?, Value_> diff) {
        return new SerializablePlanningVariableDiff<>(
                diff.variableMetaModel().name(),
                diff.oldValue(),
                diff.newValue());
    }

}
