package ai.timefold.solver.core.impl.heuristic.selector.common.decorator;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder;

/**
 * Sorts a selection {@link List} based on a {@link ComparatorFactory}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <T> the selection type
 */
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
    public void sort(ScoreDirector<Solution_> scoreDirector, List<T> selectionList) {
        var appliedComparator =
                getAppliedComparator(selectionComparatorFactory.createComparator(scoreDirector.getWorkingSolution()));
        selectionList.sort(appliedComparator);
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
