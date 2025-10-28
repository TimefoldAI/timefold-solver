package ai.timefold.solver.core.impl.domain.valuerange.sort;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSetSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record SelectionSorterAdapter<Solution_, T>(Solution_ solution,
        SelectionSorter<Solution_, T> selectionSorter) implements ValueRangeSorter<T> {

    public static <Solution_, T> ValueRangeSorter<T> of(Solution_ solution, SelectionSorter<Solution_, T> selectionSorter) {
        return new SelectionSorterAdapter<>(solution, selectionSorter);
    }

    @Override
    public void sort(List<T> selectionList) {
        selectionSorter.sort(solution, selectionList);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public SortedSet<T> sort(Set<T> selectionSet) {
        if (!(selectionSorter instanceof SelectionSetSorter selectionSetSorter)) {
            throw new IllegalStateException(
                    "Impossible state: the sorting operation cannot be performed because the sorter does not support sorting collection sets.");
        }
        return selectionSetSorter.sort(solution, selectionSet);
    }
}
