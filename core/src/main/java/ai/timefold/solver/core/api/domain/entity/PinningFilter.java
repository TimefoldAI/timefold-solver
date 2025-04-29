package ai.timefold.solver.core.api.domain.entity;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

import org.jspecify.annotations.NullMarked;

/**
 * Decides on accepting or discarding a {@link PlanningEntity}.
 * A pinned {@link PlanningEntity}'s planning variables are never changed.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Entity_> the entity type, the class with the {@link PlanningEntity} annotation
 * @deprecated Use {@link PlanningPin} instead.
 */
@Deprecated(forRemoval = true, since = "1.23.0")
@NullMarked
public interface PinningFilter<Solution_, Entity_> {

    /**
     * @param solution working solution to which the entity belongs
     * @param entity a {@link PlanningEntity}
     * @return true if the entity it is pinned, false if the entity is movable.
     */
    boolean accept(Solution_ solution, Entity_ entity);

}
