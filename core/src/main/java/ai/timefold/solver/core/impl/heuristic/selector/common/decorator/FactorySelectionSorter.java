package ai.timefold.solver.core.impl.heuristic.selector.common.decorator;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import ai.timefold.solver.core.api.domain.common.SorterFactory;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.Selector;

/**
 * Sorts a selection {@link List} based on a {@link SorterFactory}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <T> the selection type
 */
public final class FactorySelectionSorter<Solution_, T> implements SelectionSorter<Solution_, T> {

    private final SorterFactory<Solution_, T> selectionSorterFactory;
    private final Comparator<Comparable> comparator;

    public FactorySelectionSorter(SorterFactory<Solution_, T> selectionSorterFactory,
                                  SelectionSorterOrder selectionSorterOrder) {
        this.selectionSorterFactory = selectionSorterFactory;
        switch (selectionSorterOrder) {
            case ASCENDING:
                this.comparator = Comparator.naturalOrder();
                break;
            case DESCENDING:
                this.comparator = Collections.reverseOrder();
                break;
            default:
                throw new IllegalStateException("The selectionSorterOrder (" + selectionSorterOrder
                        + ") is not implemented.");
        }
    }

    @Override
    public void sort(ScoreDirector<Solution_> scoreDirector, List<T> selectionList) {
        sort(scoreDirector.getWorkingSolution(), selectionList);
    }

    /**
     * @param solution never null, the {@link PlanningSolution} to which the selections belong or apply to
     * @param selectionList never null, a {@link List}
     *        of {@link PlanningEntity}, planningValue, {@link Move} or {@link Selector}
     */
    public void sort(Solution_ solution, List<T> selectionList) {
        SortedMap<Comparable, T> selectionMap = new TreeMap<>(comparator);
        for (T selection : selectionList) {
            Comparable difficultyWeight = selectionSorterFactory.createSorter(solution, selection);
            T previous = selectionMap.put(difficultyWeight, selection);
            if (previous != null) {
                throw new IllegalStateException("The selectionList contains 2 times the same selection ("
                        + previous + ") and (" + selection + ").");
            }
        }
        selectionList.clear();
        selectionList.addAll(selectionMap.values());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || getClass() != other.getClass())
            return false;
        FactorySelectionSorter<?, ?> that = (FactorySelectionSorter<?, ?>) other;
        return Objects.equals(selectionSorterFactory, that.selectionSorterFactory)
                && Objects.equals(comparator, that.comparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selectionSorterFactory, comparator);
    }
}
