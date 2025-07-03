package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.FromListVarEntityPropertyValueSelector;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public interface ValueRangeDescriptor<Solution_> {

    /**
     * @return never null
     */
    GenuineVariableDescriptor<Solution_> getVariableDescriptor();

    /**
     * @return true if the {@link ValueRange} is countable
     *         (for example a double value range between 1.2 and 1.4 is not countable)
     */
    boolean isCountable();

    /**
     * If this method return true, this instance is safe to cast to {@link EntityIndependentValueRangeDescriptor},
     * otherwise it requires an entity to determine the {@link ValueRange}.
     *
     * @return true if the {@link ValueRange} is the same for all entities of the same solution
     */
    boolean isEntityIndependent();

    /**
     * If this method returns true, it indicates that the value range is entity-dependent,
     * but it is adapted to function like it is entity-independent.
     * There is a specific case when it returns true,
     * specifically when using an entity-dependent value range for a list variable.
     *
     * @return true if the {@link ValueRange} is adapted to behave like an entity-independent value range; otherwise, returns
     *         false.
     *
     * @see FromListVarEntityPropertyValueRangeDescriptor
     * @see FromListVarEntityPropertyValueSelector
     */
    default boolean isAdaptedToEntityIndependent() {
        return false;
    }

    /**
     * @return true if the {@link ValueRange} might contain a planning entity instance
     *         (not necessarily of the same entity class as this entity class of this descriptor.
     */
    boolean mightContainEntity();

    /**
     * @param solution never null
     * @param entity never null. To avoid this parameter,
     *        use {@link EntityIndependentValueRangeDescriptor#extractValueRange} instead.
     * @return never null
     */
    <Value_> ValueRange<Value_> extractValueRange(Solution_ solution, Object entity);

    /**
     * @param solution never null
     * @param entity never null. To avoid this parameter,
     *        use {@link EntityIndependentValueRangeDescriptor#extractValueRangeSize} instead.
     * @return never null
     * @throws UnsupportedOperationException if {@link #isCountable()} returns false
     */
    long extractValueRangeSize(Solution_ solution, Object entity);

}
