package ai.timefold.solver.core.impl.heuristic.selector.common.decorator;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.Selector;

import org.jspecify.annotations.NullMarked;

/**
 * Decides the order of a {@link List} of selection
 * (which is a {@link PlanningEntity}, a planningValue, a {@link Move} or a {@link Selector}).
 *
 * <p>
 * Implementations are expected to be stateless.
 * The solver may choose to reuse instances.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <T> the selection type
 */
@NullMarked
public interface SelectionSorter<Solution_, T> {

    /**
     * Creates a copy of the provided list and sort the data.
     * 
     * @param solution never null, the current solution
     * @param selectionList never null, a {@link List}
     *        of {@link PlanningEntity}, planningValue, {@link Move} or {@link Selector} that will be sorted.
     */
    List<T> sort(Solution_ solution, List<T> selectionList);

    /**
     * Creates a copy of the provided set and sort the data.
     *
     * @param solution never null, the current solution
     * @param selectionSet never null, a {@link Set} of values that will be used as input for sorting.
     */
    SortedSet<T> sort(Solution_ solution, Set<T> selectionSet);

}
