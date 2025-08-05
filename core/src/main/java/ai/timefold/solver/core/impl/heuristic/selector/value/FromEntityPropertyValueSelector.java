package ai.timefold.solver.core.impl.heuristic.selector.value;

import java.util.Iterator;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractCachingEnabledSelector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.jspecify.annotations.NonNull;

/**
 * This is the common {@link ValueSelector} implementation.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class FromEntityPropertyValueSelector<Solution_>
        extends AbstractCachingEnabledSelector<Solution_, CountableValueRange<Object>>
        implements ValueSelector<Solution_> {

    private final ValueRangeDescriptor<Solution_> valueRangeDescriptor;
    private final boolean randomSelection;

    private InnerScoreDirector<Solution_, ?> scoreDirector;

    public FromEntityPropertyValueSelector(ValueRangeDescriptor<Solution_> valueRangeDescriptor, boolean randomSelection) {
        this.valueRangeDescriptor = valueRangeDescriptor;
        this.randomSelection = randomSelection;
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        this.scoreDirector = solverScope.getScoreDirector();
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        this.scoreDirector = null;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public GenuineVariableDescriptor<Solution_> getVariableDescriptor() {
        return valueRangeDescriptor.getVariableDescriptor();
    }

    @Override
    public @NonNull CountableValueRange<Object> buildCacheItem(@NonNull InnerScoreDirector<Solution_, ?> scoreDirector) {
        return scoreDirector.getValueRangeManager().getFromSolution(valueRangeDescriptor);
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
        if (entity == null) {
            // When the entity is null, the size of the complete list of values is returned
            // This logic aligns with the requirements for Nearby in the enterprise repository
            return Objects.requireNonNull(getCachedItem()).getSize();
        } else {
            return scoreDirector.getValueRangeManager().countOnEntity(valueRangeDescriptor, entity);
        }
    }

    @Override
    public Iterator<Object> iterator(Object entity) {
        var valueRange = scoreDirector.getValueRangeManager().getFromEntity(valueRangeDescriptor, entity);
        if (!randomSelection) {
            return valueRange.createOriginalIterator();
        } else {
            return valueRange.createRandomIterator(workingRandom);
        }
    }

    @Override
    public Iterator<Object> endingIterator(Object entity) {
        if (entity == null) {
            // When the entity is null, the complete list of values is returned
            // This logic aligns with the requirements for Nearby in the enterprise repository
            return Objects.requireNonNull(getCachedItem()).createOriginalIterator();
        } else {
            var valueRange = scoreDirector.getValueRangeManager().getFromEntity(valueRangeDescriptor, entity);
            return valueRange.createOriginalIterator();
        }
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
