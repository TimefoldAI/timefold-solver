package ai.timefold.solver.core.impl.heuristic.selector.value;

import java.util.Iterator;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractDemandEnabledSelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;

/**
 * This is the common {@link ValueSelector} implementation.
 */
public final class IterableFromSolutionPropertyValueSelector<Solution_>
        extends AbstractDemandEnabledSelector<Solution_>
        implements IterableValueSelector<Solution_> {

    private final ValueRangeDescriptor<Solution_> valueRangeDescriptor;
    private final SelectionSorter<Solution_, Object> selectionSorter;
    private final SelectionCacheType minimumCacheType;
    private final boolean randomSelection;
    private final boolean valueRangeMightContainEntity;

    private ValueRange<Object> cachedValueRange = null;
    private Long cachedEntityListRevision = null;
    private boolean cachedEntityListIsDirty = false;

    public IterableFromSolutionPropertyValueSelector(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
            SelectionSorter<Solution_, Object> selectionSorter, SelectionCacheType minimumCacheType, boolean randomSelection) {
        this.valueRangeDescriptor = valueRangeDescriptor;
        this.selectionSorter = selectionSorter;
        this.minimumCacheType = minimumCacheType;
        this.randomSelection = randomSelection;
        valueRangeMightContainEntity = valueRangeDescriptor.mightContainEntity();
    }

    @Override
    public GenuineVariableDescriptor<Solution_> getVariableDescriptor() {
        return valueRangeDescriptor.getVariableDescriptor();
    }

    @Override
    public SelectionCacheType getCacheType() {
        var intrinsicCacheType = valueRangeMightContainEntity ? SelectionCacheType.STEP : SelectionCacheType.PHASE;
        return (intrinsicCacheType.compareTo(minimumCacheType) > 0) ? intrinsicCacheType : minimumCacheType;
    }

    @Override
    public SelectionSorter<Solution_, Object> getSelectionSorter() {
        return selectionSorter;
    }

    // ************************************************************************
    // Cache lifecycle methods
    // ************************************************************************

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        var scoreDirector = phaseScope.getScoreDirector();
        cachedValueRange = scoreDirector.getValueRangeManager().getFromSolution(valueRangeDescriptor,
                scoreDirector.getWorkingSolution(), selectionSorter);
        if (valueRangeMightContainEntity) {
            cachedEntityListRevision = scoreDirector.getWorkingEntityListRevision();
            cachedEntityListIsDirty = false;
        }
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        if (valueRangeMightContainEntity) {
            var scoreDirector = stepScope.getScoreDirector();
            if (scoreDirector.isWorkingEntityListDirty(cachedEntityListRevision)) {
                if (minimumCacheType.compareTo(SelectionCacheType.STEP) > 0) {
                    cachedEntityListIsDirty = true;
                } else {
                    cachedValueRange = scoreDirector.getValueRangeManager().getFromSolution(valueRangeDescriptor,
                            scoreDirector.getWorkingSolution(), selectionSorter);
                    cachedEntityListRevision = scoreDirector.getWorkingEntityListRevision();
                }
            }
        }
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        cachedValueRange = null;
        if (valueRangeMightContainEntity) {
            cachedEntityListRevision = null;
            cachedEntityListIsDirty = false;
        }
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

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
        return getSize();
    }

    @Override
    public long getSize() {
        return ((CountableValueRange<?>) cachedValueRange).getSize();
    }

    @Override
    public Iterator<Object> iterator(Object entity) {
        return iterator();
    }

    @Override
    public Iterator<Object> iterator() {
        checkCachedEntityListIsDirty();
        if (randomSelection) {
            return cachedValueRange.createRandomIterator(workingRandom);
        }
        if (cachedValueRange instanceof CountableValueRange<Object> range) {
            return range.createOriginalIterator();
        }
        throw new IllegalStateException("Value range's class (" + cachedValueRange.getClass().getCanonicalName() + ") " +
                "does not implement " + CountableValueRange.class + ", " +
                "yet selectionOrder is not " + SelectionOrder.RANDOM + ".\n" +
                "Maybe switch selectors' selectionOrder to " + SelectionOrder.RANDOM + "?\n" +
                "Maybe switch selectors' cacheType to " + SelectionCacheType.JUST_IN_TIME + "?");
    }

    @Override
    public Iterator<Object> endingIterator(Object entity) {
        return endingIterator();
    }

    public Iterator<Object> endingIterator() {
        return ((CountableValueRange<Object>) cachedValueRange).createOriginalIterator();
    }

    private void checkCachedEntityListIsDirty() {
        if (cachedEntityListIsDirty) {
            throw new IllegalStateException("The selector (" + this + ") with minimumCacheType (" + minimumCacheType
                    + ")'s workingEntityList became dirty between steps but is still used afterwards.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IterableFromSolutionPropertyValueSelector<?> that))
            return false;
        return randomSelection == that.randomSelection && Objects.equals(valueRangeDescriptor, that.valueRangeDescriptor)
                && Objects.equals(selectionSorter, that.selectionSorter) && minimumCacheType == that.minimumCacheType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueRangeDescriptor, selectionSorter, minimumCacheType, randomSelection);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getVariableDescriptor().getVariableName() + ")";
    }

}
