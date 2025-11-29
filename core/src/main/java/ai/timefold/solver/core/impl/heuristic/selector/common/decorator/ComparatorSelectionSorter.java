package ai.timefold.solver.core.impl.heuristic.selector.common.decorator;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder;

import org.jspecify.annotations.NullMarked;

/**
 * Sorts a selection {@link List} based on a {@link Comparator}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <T> the selection type
 */
@NullMarked
public final class ComparatorSelectionSorter<Solution_, T> implements SelectionSorter<Solution_, T> {

    private final Comparator<T> appliedComparator;

    public ComparatorSelectionSorter(Comparator<T> comparator, SelectionSorterOrder selectionSorterOrder) {
        switch (selectionSorterOrder) {
            case ASCENDING:
                this.appliedComparator = comparator;
                break;
            case DESCENDING:
                this.appliedComparator = Collections.reverseOrder(comparator);
                break;
            default:
                throw new IllegalStateException("The selectionSorterOrder (" + selectionSorterOrder
                        + ") is not implemented.");
        }
    }

    @Override
    public void sort(Solution_ solution, List<T> selectionList) {
        selectionList.sort(appliedComparator);
    }

    @Override
    public SortedSet<T> sort(Solution_ solution, Set<T> selectionSet) {
        var treeSet = new TreeSet<>(appliedComparator);
        treeSet.addAll(selectionSet);
        return Collections.unmodifiableSortedSet(treeSet);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || getClass() != other.getClass())
            return false;
        ComparatorSelectionSorter<?, ?> that = (ComparatorSelectionSorter<?, ?>) other;
        return Objects.equals(appliedComparator, that.appliedComparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appliedComparator);
    }
}
