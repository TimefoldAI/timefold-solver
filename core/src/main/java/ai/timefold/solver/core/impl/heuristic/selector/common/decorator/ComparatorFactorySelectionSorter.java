package ai.timefold.solver.core.impl.heuristic.selector.common.decorator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder;

import org.jspecify.annotations.NullMarked;

/**
 * Sorts a selection {@link List} based on a {@link ComparatorFactory}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <T> the selection type
 */
@NullMarked
public final class ComparatorFactorySelectionSorter<Solution_, T> implements SelectionSorter<Solution_, T> {

    private final ComparatorFactory<Solution_, T> selectionComparatorFactory;
    private final SelectionSorterOrder selectionSorterOrder;

    public ComparatorFactorySelectionSorter(ComparatorFactory<Solution_, T> selectionComparatorFactory,
            SelectionSorterOrder selectionSorterOrder) {
        this.selectionComparatorFactory = selectionComparatorFactory;
        this.selectionSorterOrder = selectionSorterOrder;
    }

    private Comparator<T> getAppliedComparator(Comparator<T> comparator) {
        return switch (selectionSorterOrder) {
            case ASCENDING -> comparator;
            case DESCENDING -> Collections.reverseOrder(comparator);
        };
    }

    @Override
    public List<T> sort(Solution_ solution, List<T> selectionList) {
        var appliedComparator = getAppliedComparator(selectionComparatorFactory.createComparator(solution));
        var sortedList = new ArrayList<>(selectionList);
        sortedList.sort(appliedComparator);
        return Collections.unmodifiableList(sortedList);
    }

    @Override
    public SortedSet<T> sort(Solution_ solution, Set<T> selectionSet) {
        var treeSet = new TreeSet<>(getAppliedComparator(selectionComparatorFactory.createComparator(solution)));
        treeSet.addAll(selectionSet);
        return Collections.unmodifiableSortedSet(treeSet);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ComparatorFactorySelectionSorter<?, ?> that)) {
            return false;
        }
        return Objects.equals(selectionComparatorFactory, that.selectionComparatorFactory)
                && selectionSorterOrder == that.selectionSorterOrder;
    }

    @Override
    public int hashCode() {
        return Objects.hash(selectionComparatorFactory, selectionSorterOrder);
    }
}
