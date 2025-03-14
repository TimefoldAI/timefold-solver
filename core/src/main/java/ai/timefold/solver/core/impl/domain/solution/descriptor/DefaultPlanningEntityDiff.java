package ai.timefold.solver.core.impl.domain.solution.descriptor;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningEntityDiff;
import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningSolutionDiff;
import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningVariableDiff;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
record DefaultPlanningEntityDiff<Solution_, Entity_>(PlanningSolutionDiff<Solution_> solutionDiff, Entity_ entity,
        PlanningEntityMetaModel<Solution_, Entity_> entityMetaModel,
        Map<String, PlanningVariableDiff<Solution_, Entity_, ?>> variableDiffMap)
        implements
            PlanningEntityDiff<Solution_, Entity_> {

    @SuppressWarnings("unchecked")
    DefaultPlanningEntityDiff(PlanningSolutionDiff<Solution_> solutionDiff, Entity_ entity) {
        this(solutionDiff, entity,
                (PlanningEntityMetaModel<Solution_, Entity_>) solutionDiff.solutionMetaModel().entity(entity.getClass()));
    }

    DefaultPlanningEntityDiff(PlanningSolutionDiff<Solution_> solutionDiff, Entity_ entity,
            PlanningEntityMetaModel<Solution_, Entity_> entityMetaModel) {
        this(solutionDiff, entity, entityMetaModel, new LinkedHashMap<>(entityMetaModel.variables().size()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Value_> PlanningVariableDiff<Solution_, Entity_, Value_> variableDiff() {
        if (variableDiffMap.size() != 1) {
            throw new IllegalStateException("""
                    There is more than one planning variable (%s) on the planning entity (%s).
                    Use variableDiff(String) instead.""".formatted(variableDiffMap.keySet(), entity.getClass()));
        }
        return (PlanningVariableDiff<Solution_, Entity_, Value_>) variableDiffs().iterator().next();
    }

    void addVariableDiff(PlanningVariableDiff<Solution_, Entity_, ?> variableDiff) {
        variableDiffMap.put(variableDiff.variableMetaModel().name(), variableDiff);
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable <Value_> PlanningVariableDiff<Solution_, Entity_, Value_> variableDiff(String variableRef) {
        return (PlanningVariableDiff<Solution_, Entity_, Value_>) variableDiffMap.get(variableRef);
    }

    @Override
    public Collection<PlanningVariableDiff<Solution_, Entity_, ?>> variableDiffs() {
        return Collections.unmodifiableCollection(variableDiffMap.values());
    }

    @Override
    public String toString() {
        return """
                Difference between two solutions in variable(s) of planning entity (%s) of type (%s):
                %s
                """.formatted(entity, entityMetaModel().type(), entityDiffToString(this));
    }

    private static String entityDiffToString(PlanningEntityDiff<?, ?> entityDiff) {
        var variableDiffs = entityDiff.variableDiffs();
        if (variableDiffs.size() == 1) {
            var diff = variableDiffs.iterator().next();
            return "  %s: %s -> %s".formatted(diff.variableMetaModel().name(), diff.oldValue(), diff.newValue());
        }
        return variableDiffs.stream()
                .map(diff -> "  %s (%s): %s -> %s".formatted(diff.variableMetaModel().name(),
                        diff.variableMetaModel().isGenuine() ? "genuine" : "shadow", diff.oldValue(), diff.newValue()))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof DefaultPlanningEntityDiff<?, ?> that)) {
            return false;
        }
        return Objects.equals(entityMetaModel, that.entityMetaModel)
                && Objects.equals(solutionDiff, that.solutionDiff)
                && Objects.equals(entity, that.entity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityMetaModel, solutionDiff, entity);
    }
}
