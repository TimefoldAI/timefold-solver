package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public interface EntityIndependentValueRangeDescriptor<Solution_> extends ValueRangeDescriptor<Solution_> {

    /**
     * As specified by {@link ValueRangeDescriptor#extractValueRange}.
     *
     * @param solution never null
     * @return never null
     * @see ValueRangeDescriptor#extractValueRange
     */
    ValueRange<?> extractValueRange(Solution_ solution);

    /**
     * As specified by {@link ValueRangeDescriptor#extractValueRangeSize}.
     *
     * @param solution never null
     * @return never null
     * @see ValueRangeDescriptor#extractValueRangeSize
     */
    long extractValueRangeSize(Solution_ solution);

}
