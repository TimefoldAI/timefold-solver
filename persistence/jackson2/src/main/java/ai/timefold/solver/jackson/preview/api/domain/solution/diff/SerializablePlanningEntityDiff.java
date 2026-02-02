package ai.timefold.solver.jackson.preview.api.domain.solution.diff;

import java.util.Collection;
import java.util.stream.Collectors;

import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningEntityDiff;

import com.fasterxml.jackson.annotation.JsonProperty;

record SerializablePlanningEntityDiff<Entity_>(
        @JsonProperty("entity_class") String entityClass,
        @JsonProperty("entity") Entity_ entity,
        @JsonProperty("variable_diffs") Collection<SerializablePlanningVariableDiff<?>> variableDiffs) {

    public static <Entity_> SerializablePlanningEntityDiff<Entity_> of(PlanningEntityDiff<?, Entity_> diff) {
        return new SerializablePlanningEntityDiff<>(
                diff.entity().getClass().getCanonicalName(),
                diff.entity(),
                diff.variableDiffs().stream()
                        .map(SerializablePlanningVariableDiff::of)
                        .collect(Collectors.toList()));
    }

}
