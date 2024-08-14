package ai.timefold.solver.core.impl.heuristic.selector.common.decorator;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.Selector;

/**
 * Decides on accepting or discarding a selection,
 * which is either a {@link PlanningEntity}, a planning value, a {@link Move} or a {@link Selector}).
 * For example, a pinned {@link PlanningEntity} is rejected and therefore never used in a {@link Move}.
 * <p>
 * A filtered selection is considered as not selected, it does not count as an unaccepted selection.
 *
 * <p>
 * Implementations are expected to be stateless.
 * The solver may choose to reuse instances.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <T> the selection type.
 *        On problems using multiple planning variables on a single entity without specifying single variable name,
 *        this needs to be {@link Object} as variables of both types will be tested.
 */
@FunctionalInterface
public interface SelectionFilter<Solution_, T> {

    /**
     * Creates a {@link SelectionFilter} which applies all the provided filters one after another.
     * Once one filter in the sequence returns false, no later filers are evaluated.
     *
     * @param filterArray filters to apply, never null
     * @return never null
     * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
     * @param <T> the selection type
     */
    @SafeVarargs
    static <Solution_, T> SelectionFilter<Solution_, T> compose(SelectionFilter<Solution_, T>... filterArray) {
        return compose(Arrays.asList(filterArray));
    }

    /**
     * As defined by {@link #compose(SelectionFilter[])}.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static <Solution_, T> SelectionFilter<Solution_, T> compose(List<SelectionFilter<Solution_, T>> filterList) {
        return switch (filterList.size()) {
            case 0 -> CompositeSelectionFilter.NOOP;
            case 1 -> filterList.get(0);
            default -> {
                var distinctFilterArray = filterList.stream()
                        .flatMap(filter -> {
                            if (filter == CompositeSelectionFilter.NOOP) {
                                return Stream.empty();
                            } else if (filter instanceof CompositeSelectionFilter<Solution_, T> compositeSelectionFilter) {
                                // Decompose composites if necessary; avoids needless recursion.
                                return Arrays.stream(compositeSelectionFilter.selectionFilterArray());
                            } else {
                                return Stream.of(filter);
                            }
                        })
                        .distinct()
                        .toArray(SelectionFilter[]::new);
                yield switch (distinctFilterArray.length) {
                    case 0 -> CompositeSelectionFilter.NOOP;
                    case 1 -> distinctFilterArray[0];
                    default -> new CompositeSelectionFilter<>(distinctFilterArray);
                };
            }
        };
    }

    /**
     * @param scoreDirector never null, the {@link ScoreDirector}
     *        which has the {@link ScoreDirector#getWorkingSolution()} to which the selection belongs or applies to
     * @param selection never null, a {@link PlanningEntity}, a planningValue, a {@link Move} or a {@link Selector}
     * @return true if the selection is accepted (for example it is movable),
     *         false if the selection will be discarded (for example it is pinned)
     */
    boolean accept(ScoreDirector<Solution_> scoreDirector, T selection);

}
