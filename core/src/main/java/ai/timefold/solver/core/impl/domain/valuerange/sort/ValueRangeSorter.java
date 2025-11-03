package ai.timefold.solver.core.impl.domain.valuerange.sort;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.jspecify.annotations.NullMarked;

/**
 * Basic contract for sorting a range of elements.
 * 
 * @param <T> the value range type
 */
@NullMarked
public interface ValueRangeSorter<T> {

    /**
     * Creates a copy of the provided list and sort the data.
     * 
     * @param selectionList never null, a {@link List} of values that will be used as input for sorting.
     */
    List<T> sort(List<T> selectionList);

    /**
     * Creates a copy of the provided set and sort the data.
     * 
     * @param selectionSet never null, a {@link Set} of values that will be used as input for sorting.
     */
    SortedSet<T> sort(Set<T> selectionSet);

}
