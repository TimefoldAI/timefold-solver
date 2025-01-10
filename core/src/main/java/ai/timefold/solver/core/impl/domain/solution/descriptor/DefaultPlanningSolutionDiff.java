package ai.timefold.solver.core.impl.domain.solution.descriptor;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningEntityDiff;
import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningSolutionDiff;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class DefaultPlanningSolutionDiff<Solution_> implements PlanningSolutionDiff<Solution_> {

    private static final int TO_STRING_ITEM_LIMIT = 5;

    private final PlanningSolutionMetaModel<Solution_> solutionMetaModel;
    private final Solution_ oldSolution;
    private final Solution_ newSolution;
    private final Set<Object> removedEntities;
    private final Set<Object> addedEntities;
    private final Map<Class<?>, Set<PlanningEntityDiff<Solution_, ?>>> entityDiffMap = new LinkedHashMap<>();

    DefaultPlanningSolutionDiff(PlanningSolutionMetaModel<Solution_> solutionMetaModel, Solution_ oldSolution,
            Solution_ newSolution, Set<?> removedEntities, Set<?> addedEntities) {
        this.solutionMetaModel = Objects.requireNonNull(solutionMetaModel);
        this.oldSolution = Objects.requireNonNull(oldSolution);
        this.newSolution = Objects.requireNonNull(newSolution);
        this.removedEntities = Set.copyOf(Objects.requireNonNull(removedEntities));
        this.addedEntities = Set.copyOf(Objects.requireNonNull(addedEntities));
    }

    @Override
    public PlanningSolutionMetaModel<Solution_> solutionMetaModel() {
        return solutionMetaModel;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable <Entity_> PlanningEntityDiff<Solution_, Entity_> entityDiff(Entity_ entity) {
        return entityDiffs((Class<Entity_>) entity.getClass()).stream()
                .filter(entityDiff -> entityDiff.entity().equals(entity))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <Entity_> Set<PlanningEntityDiff<Solution_, Entity_>> entityDiffs() {
        if (entityDiffMap.isEmpty()) {
            return Collections.emptySet();
        }
        if (entityDiffMap.size() != 1) {
            throw new UnsupportedOperationException("""
                    There is more than one entity class in the planning solution (%s).
                    Use entityDiffs(Class) instead.""".formatted(entityDiffMap.keySet()));
        }
        return entityDiffs((Class<Entity_>) entityDiffMap.keySet().iterator().next());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <Entity_> Set<PlanningEntityDiff<Solution_, Entity_>> entityDiffs(Class<Entity_> entityClass) {
        var entityDiffSet = (Set) entityDiffMap.get(entityClass);
        if (entityDiffSet == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(entityDiffSet);
    }

    void addEntityDiff(PlanningEntityDiff<Solution_, ?> entityDiff) {
        entityDiffMap.computeIfAbsent(entityDiff.entityMetaModel().type(), k -> new LinkedHashSet<>())
                .add(entityDiff);
    }

    @Override
    public Solution_ oldSolution() {
        return oldSolution;
    }

    @Override
    public Solution_ newSolution() {
        return newSolution;
    }

    @Override
    public Set<Object> removedEntities() {
        return removedEntities;
    }

    @Override
    public Set<Object> addedEntities() {
        return addedEntities;
    }

    @Override
    public String toString() {
        var removedEntityString = collectionToString(removedEntities);
        var addedEntityString = collectionToString(addedEntities);
        var entityDiffStringList = entityDiffMap.values().stream()
                .flatMap(Set::stream)
                .map(DefaultPlanningSolutionDiff::entityDiffToString)
                .toList();
        var entityDiffString = collectionToString(entityDiffStringList);
        return """
                Difference(s) between old planning solution (%s) and new planning solution (%s):

                - Old solution entities not present in new solution:
                %s

                - New solution entities not present in old solution:
                %s

                - Entities changed between the solutions:
                %s
                """.formatted(oldSolution, newSolution, removedEntityString, addedEntityString, entityDiffString);
    }

    private static String collectionToString(Collection<?> entitySet) {
        if (entitySet.isEmpty()) {
            return "  (None.)";
        }
        var addedEntityString = entitySet.stream()
                .limit(TO_STRING_ITEM_LIMIT)
                .map(s -> "  " + s.toString())
                .collect(Collectors.joining(System.lineSeparator()));
        if (entitySet.size() > TO_STRING_ITEM_LIMIT) {
            addedEntityString += System.lineSeparator() + "  ...";
        }
        return addedEntityString;
    }

    static String entityDiffToString(PlanningEntityDiff<?, ?> entityDiff) {
        var entityClass = entityDiff.entityMetaModel().type().getSimpleName();
        var entity = entityDiff.entity();
        var variableDiffs = entityDiff.variableDiffs();
        if (variableDiffs.size() == 1) {
            var variableDiff = variableDiffs.iterator().next();
            return "%s %s (%s -> %s)".formatted(entityClass, entity, variableDiff.oldValue(), variableDiff.newValue());
        }
        var variableDiffString = variableDiffs.stream()
                .map(diff -> "    %s: %s -> %s".formatted(diff.variableMetaModel().name(), diff.oldValue(),
                        diff.newValue()))
                .collect(Collectors.joining(System.lineSeparator()));
        return """
                %s %s:
                %s""".formatted(entityClass, entity, variableDiffString);
    }

}
