package ai.timefold.solver.core.api.domain.common;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.Selector;

import org.jspecify.annotations.NullMarked;

/**
 * Creates a {@link Comparable} to decide the order of a collection of selections
 * (a selection is a {@link PlanningEntity}, a planningValue, a {@link Move} or a {@link Selector}).
 * The selections are then sorted by some specific metric,
 * normally ascending unless it's configured descending.
 * The property {@code sortManner},
 * present in the selector configurations such as {@link ValueSelectorConfig} and {@link EntitySelectorConfig},
 * specifies how the data will be sorted.
 * Additionally,
 * the property {@code constructionHeuristicType} from {@link ConstructionHeuristicPhaseConfig} can also configure how entities
 * and values are sorted.
 * <p>
 * Implementations are expected to be stateless.
 * The solver may choose to reuse instances.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <T> the selection type
 * @param <V> the returning type
 *
 * @see ValueSelectorConfig
 * @see EntitySelectorConfig
 * @see ConstructionHeuristicPhaseConfig
 */
@NullMarked
@FunctionalInterface
public interface ComparatorFactory<Solution_, T, V extends Comparable<V>> {

    /**
     * @param solution never null, the {@link PlanningSolution} to which the selection belongs or applies to
     * @param selection never null, a {@link PlanningEntity}, a planningValue, a {@link Move} or a {@link Selector}
     * @return never null, for example a {@link Integer}, {@link Double} or a more complex {@link Comparable}
     */
    V createSorter(Solution_ solution, T selection);

}
