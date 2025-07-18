package ai.timefold.solver.core.impl.heuristic.selector.value;

import java.util.Iterator;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractDemandEnabledSelector;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * This is the common {@link ValueSelector} implementation.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class FromEntityPropertyValueSelector<Solution_>
        extends AbstractDemandEnabledSelector<Solution_>
        implements ValueSelector<Solution_> {

    private final ValueRangeDescriptor<Solution_> valueRangeDescriptor;
    private final boolean randomSelection;

    private ValueRangeManager<Solution_> valueRangeManager;

    public FromEntityPropertyValueSelector(ValueRangeDescriptor<Solution_> valueRangeDescriptor, boolean randomSelection) {
        this.valueRangeDescriptor = valueRangeDescriptor;
        this.randomSelection = randomSelection;
    }

    @Override
    public GenuineVariableDescriptor<Solution_> getVariableDescriptor() {
        return valueRangeDescriptor.getVariableDescriptor();
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        this.valueRangeManager = solverScope.getValueRangeManager();
    }

    @Override
    public boolean isCountable() {
        return valueRangeDescriptor.isCountable();
    }

    @Override
    public boolean isNeverEnding() {
        return randomSelection || !isCountable();
    }

    @Override
    public long getSize(Object entity) {
        return valueRangeManager.countOnEntity(valueRangeDescriptor, entity);
    }

    @Override
    public Iterator<Object> iterator(Object entity) {
        ValueRange<Object> valueRange = valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        if (!randomSelection) {
            return ((CountableValueRange<Object>) valueRange).createOriginalIterator();
        } else {
            return valueRange.createRandomIterator(workingRandom);
        }
    }

    @Override
    public Iterator<Object> endingIterator(Object entity) {
        ValueRange<Object> valueRange = valueRangeManager.getFromEntity(valueRangeDescriptor, entity);
        return ((CountableValueRange<Object>) valueRange).createOriginalIterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FromEntityPropertyValueSelector<?> that = (FromEntityPropertyValueSelector<?>) o;
        return randomSelection == that.randomSelection && Objects.equals(valueRangeDescriptor, that.valueRangeDescriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueRangeDescriptor, randomSelection);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getVariableDescriptor().getVariableName() + ")";
    }

}
