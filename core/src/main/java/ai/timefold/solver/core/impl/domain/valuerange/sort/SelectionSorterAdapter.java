package ai.timefold.solver.core.impl.domain.valuerange.sort;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record SelectionSorterAdapter<Solution_, T>(Solution_ solution,
        SelectionSorter<Solution_, T> innerSelectionSorter) implements ValueRangeSorter<T> {

    public static <Solution_, T> ValueRangeSorter<T> of(Solution_ solution, SelectionSorter<Solution_, T> selectionSorter) {
        return new SelectionSorterAdapter<>(solution, selectionSorter);
    }

    @Override
    public void sort(List<T> selectionList) {
        innerSelectionSorter.sort(solution, selectionList);
    }

    @Override
    public SortedSet<T> sort(Set<T> selectionSet) {
        return innerSelectionSorter.sort(solution, selectionSet);
    }

    @Override
    public SelectionSorter<?, T> getInnerSorter() {
        return innerSelectionSorter;
    }
}
