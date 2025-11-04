package ai.timefold.solver.core.impl.domain.valuerange.sort;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record SelectionSorterAdapter<Solution_, T>(Solution_ solution,
        SelectionSorter<Solution_, T> selectionSorter) implements ValueRangeSorter<T> {

    public static <Solution_, T> ValueRangeSorter<T> of(Solution_ solution, SelectionSorter<Solution_, T> selectionSorter) {
        return new SelectionSorterAdapter<>(solution, selectionSorter);
    }

    @Override
    public List<T> sort(List<T> selectionList) {
        return selectionSorter.sort(solution, selectionList);
    }

    @Override
    public SortedSet<T> sort(Set<T> selectionSet) {
        return selectionSorter.sort(solution, selectionSet);
    }
}
