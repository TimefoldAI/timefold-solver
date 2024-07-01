package ai.timefold.solver.core.api.score.stream.common;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;

/**
 * Calculates the unfairness measure for a particular input.
 * It is the result
 * of applying the {@link ConstraintCollectors#loadBalance(Function, ToLongFunction) load-balancing} constraint
 * collector.
 *
 * @param <Balanced_> type of the item being balanced
 */
public interface LoadBalance<Balanced_> {

    /**
     * Returns the items being balanced, along with their total load.
     * The iteration order of the map is undefined.
     * For use in justifications, create a defensive copy of the map;
     * the map itself is mutable and will be mutated by the constraint collector.
     *
     * @return never null
     */
    Map<Balanced_, Long> loads();

    /**
     * The unfairness measure describes how fairly the load is distributed over the items;
     * the higher the number, the higher the imbalance.
     * When zero, the load is perfectly balanced.
     * <p>
     * Unfairness is a dimensionless number which is solution-specific.
     * Comparing unfairness between solutions of different input problems is not helpful.
     * Only compare unfairness measures of solutions which have the same set of balanced items as input.
     *
     * @return never null, never negative, six decimal places
     */
    BigDecimal unfairness();

}
