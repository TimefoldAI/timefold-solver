package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import java.util.Iterator;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.FromEntityPropertyValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractDemandEnabledSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.FromEntityPropertyValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * The value range for list variables requires the selector to be entity-independent,
 * as it needs to fetch the entire list of values.
 * Fetching the list of values is not a problem when the value range is located within the solution class,
 * serving as a single source of truth.
 * In cases where it is entity-dependent,
 * the list of entities must be read to generate the corresponding list of values.
 * <p>
 * This selector adapts {@link FromEntityPropertyValueSelector} to behave like an entity-independent selector
 * and meets the requirement to retrieve the complete list of values.
 * 
 * @param <Solution_> the solution type
 */
public final class IterableFromEntityPropertyValueSelector<Solution_> extends AbstractDemandEnabledSelector<Solution_>
        implements IterableValueSelector<Solution_> {

    private final FromEntityPropertyValueSelector<Solution_> childValueSelector;
    private final SelectionCacheType minimumCacheType;
    private final boolean randomSelection;
    private final FromEntityPropertyValueRangeDescriptor<Solution_> valueRangeDescriptor;
    private InnerScoreDirector<Solution_, ?> innerScoreDirector = null;

    public IterableFromEntityPropertyValueSelector(FromEntityPropertyValueSelector<Solution_> childValueSelector,
            boolean randomSelection) {
        this(childValueSelector, SelectionCacheType.JUST_IN_TIME, randomSelection);
    }

    public IterableFromEntityPropertyValueSelector(FromEntityPropertyValueSelector<Solution_> childValueSelector,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        this.childValueSelector = childValueSelector;
        this.minimumCacheType = minimumCacheType;
        this.randomSelection = randomSelection;
        this.valueRangeDescriptor = (FromEntityPropertyValueRangeDescriptor<Solution_>) childValueSelector
                .getVariableDescriptor().getValueRangeDescriptor();
    }

    // ************************************************************************
    // Life-cycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        this.childValueSelector.solvingStarted(solverScope);
        this.innerScoreDirector = solverScope.getScoreDirector();
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        childValueSelector.solvingEnded(solverScope);
        this.innerScoreDirector = null;
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        childValueSelector.phaseStarted(phaseScope);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        childValueSelector.phaseEnded(phaseScope);
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public FromEntityPropertyValueSelector<Solution_> getChildValueSelector() {
        return childValueSelector;
    }

    @Override
    public SelectionCacheType getCacheType() {
        return minimumCacheType;
    }

    @Override
    public GenuineVariableDescriptor<Solution_> getVariableDescriptor() {
        return childValueSelector.getVariableDescriptor();
    }

    @Override
    public long getSize(Object entity) {
        return childValueSelector.getSize(entity);
    }

    @Override
    public Iterator<Object> iterator(Object entity) {
        return childValueSelector.iterator(entity);
    }

    @Override
    public Iterator<Object> endingIterator(Object entity) {
        return childValueSelector.endingIterator(entity);
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
    public long getSize() {
        return innerScoreDirector.getValueRangeManager().countOnSolution(valueRangeDescriptor,
                innerScoreDirector.getWorkingSolution());
    }

    @Override
    public Iterator<Object> iterator() {
        var valueRange = innerScoreDirector.getValueRangeManager()
                .getFromSolution(valueRangeDescriptor, innerScoreDirector.getWorkingSolution(),
                        childValueSelector.getSelectionSorter());
        if (randomSelection) {
            return valueRange.createRandomIterator(workingRandom);
        } else {
            return valueRange.createOriginalIterator();
        }
    }

    @Override
    public boolean equals(Object other) {
        return childValueSelector.equals(other);
    }

    @Override
    public int hashCode() {
        return childValueSelector.hashCode();
    }
}
