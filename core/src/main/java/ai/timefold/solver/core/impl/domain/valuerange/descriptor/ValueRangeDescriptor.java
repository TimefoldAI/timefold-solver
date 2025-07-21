package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public interface ValueRangeDescriptor<Solution_> {

    /**
     * @return never null
     */
    GenuineVariableDescriptor<Solution_> getVariableDescriptor();

    /**
     * When the planning variable accepts unassigned values, it returns true; otherwise, it returns false.
     */
    boolean acceptNullInValueRange();

    /**
     * @return true if the {@link ValueRange} is countable
     *         (for example a double value range between 1.2 and 1.4 is not countable)
     */
    boolean isCountable();

    /**
     * Returns true if the value range is defined at the solution level and can be directly extracted from the solution;
     * otherwise, it returns false, as the value range can only be extracted or computed from the entities.
     */
    boolean canExtractValueRangeFromSolution();

    /**
     * @return true if the {@link ValueRange} might contain a planning entity instance
     *         (not necessarily of the same entity class as this entity class of this descriptor.
     */
    boolean mightContainEntity();

    /**
     * The method allows extracting the value range from a solution or an entity,
     * and it is compatible with problem facts defined in the solution or entity classes.
     * The method should not be invoked directly by selectors or other components of the solver.
     * The {@link ValueRangeManager#getFromSolution(ValueRangeDescriptor, Object)}
     * and {@link ValueRangeManager#getFromEntity(ValueRangeDescriptor, Object)}
     * serve as the single source of truth for managing value ranges and should be used by outer components.
     * <p>
     * Calling this method outside the resolver may lead to unnecessary recomputation of ranges.
     * 
     * @param solution the solution
     * @param entity the entity. To avoid this parameter,
     *        use {@link IterableValueRangeDescriptor#extractValueRange} instead.
     * 
     * @return never null
     * 
     * @see ValueRangeManager
     */
    <Value_> ValueRange<Value_> extractValueRange(@Nullable Solution_ solution, @Nullable Object entity);

    /**
     * The method allows extracting the value range size from a solution or an entity,
     * and it is compatible with problem facts defined in the solution or entity classes.
     * The method should not be invoked directly by selectors or other components of the solver.
     * The {@link ValueRangeManager#countOnSolution(ValueRangeDescriptor, Object)}
     * and {@link ValueRangeManager#countOnEntity(ValueRangeDescriptor, Object)}
     * serve as the single source of truth for managing value ranges and should be used by outer components.
     * <p>
     * Calling this method outside the resolver may lead to unnecessary recomputation of ranges.
     * 
     * @param solution the solution
     * @param entity the entity. To avoid this parameter,
     *        use {@link IterableValueRangeDescriptor#extractValueRangeSize} instead.
     * 
     * @return never null
     * 
     * @throws UnsupportedOperationException if {@link #isCountable()} returns false
     * 
     * @see ValueRangeManager
     */
    long extractValueRangeSize(@Nullable Solution_ solution, @Nullable Object entity);

}
