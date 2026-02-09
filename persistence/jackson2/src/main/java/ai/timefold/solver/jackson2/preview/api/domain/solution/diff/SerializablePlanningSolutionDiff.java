package ai.timefold.solver.jackson2.preview.api.domain.solution.diff;

import java.util.Collection;
import java.util.stream.Collectors;

import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningSolutionDiff;

import com.fasterxml.jackson.annotation.JsonProperty;

record SerializablePlanningSolutionDiff(
        @JsonProperty("removed_entities") Collection<Object> removedEntities,
        @JsonProperty("added_entities") Collection<Object> addedEntities,
        @JsonProperty("entity_diffs") Collection<SerializablePlanningEntityDiff<?>> entityDiffs) {

    public static <Solution_> SerializablePlanningSolutionDiff of(PlanningSolutionDiff<Solution_> diff) {
        return new SerializablePlanningSolutionDiff(
                diff.removedEntities(),
                diff.addedEntities(),
                diff.entityDiffs().stream()
                        .map(SerializablePlanningEntityDiff::of)
                        .collect(Collectors.toList()));
    }

}
