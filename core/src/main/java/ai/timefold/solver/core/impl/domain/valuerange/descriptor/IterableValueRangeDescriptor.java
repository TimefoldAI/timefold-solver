package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.score.director.ValueRangeResolver;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public interface IterableValueRangeDescriptor<Solution_> extends ValueRangeDescriptor<Solution_> {

    /**
     * As specified by {@link ValueRangeDescriptor#extractValueRange}.
     * <p>
     * The method allows extracting the value range only from the solution,
     * and it is compatible with problem facts defined in the solution class.
     * The method should not be invoked directly by selectors or other components of the solver.
     * The {@link ValueRangeResolver#extractValueRangeFromSolution(ValueRangeDescriptor, Object)}
     * serves as the single source of truth for managing value ranges and should be used by outer components.
     * <p>
     * Calling this method outside the resolver may lead to unnecessary recomputation of ranges.
     *
     * @param solution never null
     * @return never null
     *
     * @see ValueRangeResolver
     * @see ValueRangeDescriptor#extractValueRange
     */
    <T> ValueRange<T> extractValueRange(Solution_ solution);

    /**
     * As specified by {@link ValueRangeDescriptor#extractValueRangeSize}.
     * <p>
     * The method allows extracting the value range size only from the solution,
     * and it is compatible with problem facts defined in the solution class.
     * The method should not be invoked directly by selectors or other components of the solver.
     * The {@link ValueRangeResolver#extractValueRangeSizeFromSolution(ValueRangeDescriptor, Object)}
     * serves as the single source of truth for managing value ranges and should be used by outer components.
     * <p>
     * Calling this method outside the resolver may lead to unnecessary recomputation of ranges.
     *
     * @param solution never null
     * @return never null
     *
     * @see ValueRangeResolver
     * @see ValueRangeDescriptor#extractValueRangeSize
     */
    long extractValueRangeSize(Solution_ solution);

}
