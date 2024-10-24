package ai.timefold.solver.core.api.domain.entity;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

import org.jspecify.annotations.NonNull;

/**
 * Decides on accepting or discarding a {@link PlanningEntity}.
 * A pinned {@link PlanningEntity}'s planning variables are never changed.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Entity_> the entity type, the class with the {@link PlanningEntity} annotation
 */
public interface PinningFilter<Solution_, Entity_> {

    /**
     * @param solution working solution to which the entity belongs
     * @param entity a {@link PlanningEntity}
     * @return true if the entity it is pinned, false if the entity is movable.
     */
    boolean accept(@NonNull Solution_ solution, @NonNull Entity_ entity);

}
