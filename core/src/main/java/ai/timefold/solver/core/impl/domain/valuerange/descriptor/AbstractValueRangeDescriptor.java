package ai.timefold.solver.core.impl.domain.valuerange.descriptor;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;

import org.jspecify.annotations.NullMarked;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public abstract sealed class AbstractValueRangeDescriptor<Solution_>
        permits AbstractFromPropertyValueRangeDescriptor, CompositeValueRangeDescriptor {

    private final int ordinal;
    protected final GenuineVariableDescriptor<Solution_> variableDescriptor;

    protected AbstractValueRangeDescriptor(int ordinal, GenuineVariableDescriptor<Solution_> variableDescriptor) {
        this.ordinal = ordinal;
        this.variableDescriptor = variableDescriptor;
    }

    /**
     * True when {@link PlanningVariable#allowsUnassigned()}.
     * Always false with {@link PlanningListVariable}
     * as list variables get unassigned through a different mechanism
     * (e.g. ElementPositionRandomIterator).
     */
    public boolean acceptsNullInValueRange() {
        return getVariableDescriptor() instanceof BasicVariableDescriptor<Solution_> basicVariableDescriptor
                && basicVariableDescriptor.allowsUnassigned();
    }

    /**
     * Returns true if the value range is defined at the solution level and can be directly extracted from the solution;
     * otherwise, it returns false, as the value range can only be extracted or computed from the entities.
     */
    public abstract boolean canExtractValueRangeFromSolution();

    /**
     * Extracts the {@link ValueRange} from the solution or,
     * if the value range is defined at the entity level,
     * extracts a composite {@link ValueRange} from all entities in the solution.
     * <p>
     * The method should not be invoked directly by selectors or other components of the solver.
     * The {@link ValueRangeManager#getFromSolution(AbstractValueRangeDescriptor, Object)}
     * and {@link ValueRangeManager#getFromEntity(AbstractValueRangeDescriptor, Object)}
     * serve as the single source of truth for managing value ranges and should be used by outer components.
     * <p>
     * Calling this method outside {@link ValueRangeManager} may lead to unnecessary recomputation of ranges.
     */
    public abstract <T> ValueRange<T> extractAllValues(Solution_ solution);

    /**
     * Extracts the {@link ValueRange} from the planning entity.
     * If the value range is defined at the solution level instead,
     * this method reads the value range from there.
     * <p>
     * The method should not be invoked directly by selectors or other components of the solver.
     * The {@link ValueRangeManager#getFromSolution(AbstractValueRangeDescriptor, Object)}
     * and {@link ValueRangeManager#getFromEntity(AbstractValueRangeDescriptor, Object)}
     * serve as the single source of truth for managing value ranges and should be used by outer components.
     * <p>
     * Calling this method outside {@link ValueRangeManager} may lead to unnecessary recomputation of ranges.
     */
    public abstract <T> ValueRange<T> extractValuesFromEntity(Solution_ solution, Object entity);

    /**
     * A number unique within a {@link SolutionDescriptor}, increasing sequentially from zero.
     * Used for indexing in arrays to avoid object hash lookups in maps.
     *
     * @return zero or higher
     */
    public int getOrdinal() {
        return ordinal;
    }

    /**
     * @return never null
     */
    public GenuineVariableDescriptor<Solution_> getVariableDescriptor() {
        return variableDescriptor;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    /**
     * @return true if the {@link ValueRange} might contain a planning entity instance
     *         (not necessarily of the same entity class as this entity class of this descriptor.
     */
    public boolean mightContainEntity() {
        SolutionDescriptor<Solution_> solutionDescriptor = variableDescriptor.getEntityDescriptor().getSolutionDescriptor();
        Class<?> variablePropertyType = variableDescriptor.getVariablePropertyType();
        for (Class<?> entityClass : solutionDescriptor.getEntityClassSet()) {
            if (variablePropertyType.isAssignableFrom(entityClass)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + variableDescriptor.getVariableName() + ")";
    }

}
