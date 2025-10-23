package ai.timefold.solver.core.impl.heuristic.selector.common.decorator;

import java.util.Comparator;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
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
 * @deprecated Deprecated in favor of {@link ComparatorFactory}.
 * 
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <T> the selection type
 */
@Deprecated(forRemoval = true, since = "1.28.0")
@FunctionalInterface
public interface SelectionSorterWeightFactory<Solution_, T> extends ComparatorFactory<Solution_, T> {

    /**
     * @param solution never null, the {@link PlanningSolution} to which the selection belongs or applies to
     * @param selection never null, a {@link PlanningEntity}, a planningValue, a {@link Move} or a {@link Selector}
     * @return never null, for example a {@link Integer}, {@link Double} or a more complex {@link Comparable}
     */
    Comparable createSorterWeight(Solution_ solution, T selection);

    /**
     * Default implementation for enabling interconnection between the two comparator contracts.
     */
    @Override
    default Comparator<T> createComparator(Solution_ solution) {
        return (v1, v2) -> createSorterWeight(solution, v1).compareTo(createSorterWeight(solution, v2));
    }
}
