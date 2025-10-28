package ai.timefold.solver.core.impl.heuristic.selector.common.decorator;

import java.util.Set;
import java.util.SortedSet;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

import org.jspecify.annotations.NullMarked;

/**
 * Decides the order of a {@link Set} of selection values.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <T> the selection type
 */
@NullMarked
@FunctionalInterface
public interface SelectionSetSorter<Solution_, T> {

    /**
     * Creates a copy of the provided set and sort the data.
     * 
     * @param solution never null, the current solution
     * @param selectionSet never null, a {@link Set} of values that will be used as input for sorting.
     */
    SortedSet<T> sort(Solution_ solution, Set<T> selectionSet);

}
