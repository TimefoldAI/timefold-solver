package ai.timefold.solver.core.impl.heuristic.selector.common.decorator;

import ai.timefold.solver.core.api.domain.common.SorterFactory;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.Selector;

/**
 * Creates a weight to decide the order of a collections of selections
 * (a selection is a {@link PlanningEntity}, a planningValue, a {@link Move} or a {@link Selector}).
 * The selections are then sorted by their weight,
 * normally ascending unless it's configured descending.
 *
 * <p>
 * Implementations are expected to be stateless.
 * The solver may choose to reuse instances.
 *
 * @deprecated Deprecated in favor of {@link SorterFactory}.
 * 
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <T> the selection type
 */
@Deprecated(forRemoval = true, since = "1.28.0")
public interface SelectionSorterWeightFactory<Solution_, T> extends SorterFactory<Solution_, T> {

    Comparable createSorterWeight(Solution_ solution, T selection);

    /**
     * The default implementation has been created to maintain compatibility with the old contract.
     */
    @Override
    default Comparable createSorter(Solution_ solution, T selection) {
        return createSorterWeight(solution, selection);
    }
}
