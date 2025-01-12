package ai.timefold.solver.core.preview.api.domain.solution.diff;

import java.util.Set;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A diff between two instances of a {@link PlanningSolution}.
 * Obtain using {@link SolutionManager#diff(Object, Object)}.
 * The first argument to that method is called the "old" solution,
 * and the second argument is called the "new" solution.
 * See the Javadoc of {@link SolutionManager#diff(Object, Object)} for more information.
 * <p>
 * This interface is not intended to be implemented by users.
 * The default implementation has a default {@code toString()} method that prints a summary of the differences.
 * Do not attempt to parse that string - it is subject to change in the future.
 *
 * @param <Solution_>
 */
@NullMarked
public interface PlanningSolutionDiff<Solution_> {

    /**
     * Describes the {@link PlanningSolution} class.
     */
    PlanningSolutionMetaModel<Solution_> solutionMetaModel();

    /**
     * Returns the diff for the given entity, or null if the entity is not present in the diff.
     *
     * @param entity Entity to check for.
     * @return Null if the entity is not present in the diff (= did not change.)
     */
    @Nullable
    <Entity_> PlanningEntityDiff<Solution_, Entity_> entityDiff(Entity_ entity);

    /**
     * Returns the diffs of all entities that can be found in both the old and new solution,
     * where at least one variable (genuine or shadow) of that entity changed.
     */
    Set<PlanningEntityDiff<Solution_, ?>> entityDiffs();

    /**
     * As defined by {@link #entityDiffs()}, but only for entities of the given class.
     *
     * @param entityClass Entity class to filter on.
     */
    <Entity_> Set<PlanningEntityDiff<Solution_, Entity_>> entityDiffs(Class<Entity_> entityClass);

    /**
     * Returns all entities that were present in the old solution, but are not in the new.
     */
    Set<Object> removedEntities();

    /**
     * Returns all entities that are present in the new solution, but were not in the old.
     */
    Set<Object> addedEntities();

    /**
     * Returns the old solution.
     */
    Solution_ oldSolution();

    /**
     * Returns the new solution.
     */
    Solution_ newSolution();

}
