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
        this.removedEntities = Collections.unmodifiableSet(Objects.requireNonNull(removedEntities));
        this.addedEntities = Collections.unmodifiableSet(Objects.requireNonNull(addedEntities));
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

    @Override
    public Set<PlanningEntityDiff<Solution_, ?>> entityDiffs() {
        if (entityDiffMap.isEmpty()) {
            return Collections.emptySet();
        }
        return entityDiffMap.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
        return toString(TO_STRING_ITEM_LIMIT);
    }

    /**
     * Like {@link #toString()}, but with a configurable limit on the number of items listed.
     * 
     * @param limit Use Integer.MAX_VALUE to list all items.
     * @return A string representation of the diff.
     */
    String toString(int limit) {
        var removedEntityString = collectionToString(removedEntities, limit);
        var addedEntityString = collectionToString(addedEntities, limit);
        var entityDiffStringList = entityDiffMap.values().stream()
                .flatMap(Set::stream)
                .map(DefaultPlanningSolutionDiff::entityDiffToString)
                .toList();
        var entityDiffString = collectionToString(entityDiffStringList, limit);
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

    private static String collectionToString(Collection<?> entitySet, int limit) {
        if (entitySet.isEmpty()) {
            return "  (None.)";
        }
        var addedEntityString = entitySet.stream()
                .limit(limit)
                .map(s -> "  " + s.toString())
                .collect(Collectors.joining(System.lineSeparator()));
        if (entitySet.size() > limit) {
            addedEntityString += System.lineSeparator() + "  ...";
        }
        return addedEntityString;
    }

    static String entityDiffToString(PlanningEntityDiff<?, ?> entityDiff) {
        var entity = entityDiff.entity();
        var variableDiffs = entityDiff.variableDiffs();
        if (entityDiff.entityMetaModel().variables().size() == 1) {
            var variableDiff = variableDiffs.iterator().next();
            return "%s (%s -> %s)".formatted(entity, variableDiff.oldValue(), variableDiff.newValue());
        }
        var variableDiffString = variableDiffs.stream()
                .map(diff -> "    %s (%s): %s -> %s".formatted(diff.variableMetaModel().name(),
                        diff.variableMetaModel().isGenuine() ? "genuine" : "shadow", diff.oldValue(), diff.newValue()))
                .collect(Collectors.joining(System.lineSeparator()));
        return """
                %s:
                %s""".formatted(entity, variableDiffString);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof DefaultPlanningSolutionDiff<?> that)) {
            return false;
        }
        return Objects.equals(solutionMetaModel, that.solutionMetaModel) && Objects.equals(oldSolution, that.oldSolution)
                && Objects.equals(newSolution, that.newSolution);
    }

    @Override
    public int hashCode() {
        return Objects.hash(solutionMetaModel, oldSolution, newSolution);
    }
}
