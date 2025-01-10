package ai.timefold.solver.core.impl.domain.solution.descriptor;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningEntityDiff;
import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningSolutionDiff;
import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningVariableDiff;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class DefaultPlanningEntityDiff<Solution_, Entity_> implements PlanningEntityDiff<Solution_, Entity_> {

    private final PlanningSolutionDiff<Solution_> solutionDiff;
    private final Entity_ entity;
    private final PlanningEntityMetaModel<Solution_, Entity_> entityMetaModel;
    private final Map<String, PlanningVariableDiff<Solution_, Entity_, ?>> variableDiffMap = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    DefaultPlanningEntityDiff(PlanningSolutionDiff<Solution_> solutionDiff, Entity_ entity) {
        this.solutionDiff = solutionDiff;
        this.entity = entity;
        this.entityMetaModel = solutionDiff.solutionMetaModel().entity((Class<Entity_>) entity.getClass());
    }

    @Override
    public PlanningSolutionDiff<Solution_> solutionDiff() {
        return solutionDiff;
    }

    @Override
    public Entity_ entity() {
        return entity;
    }

    @Override
    public PlanningEntityMetaModel<Solution_, Entity_> entityMetaModel() {
        return entityMetaModel;
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
                """.formatted(entity, entityMetaModel.type(), entityDiffToString(this));
    }

    private static String entityDiffToString(PlanningEntityDiff<?, ?> entityDiff) {
        var variableDiffs = entityDiff.variableDiffs();
        return variableDiffs.stream()
                .map(diff -> "  %s: %s -> %s".formatted(diff.variableMetaModel().name(), diff.oldValue(),
                        diff.newValue()))
                .collect(Collectors.joining(System.lineSeparator()));
    }

}
