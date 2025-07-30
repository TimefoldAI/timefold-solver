package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractDemandEnabledSelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.SelectionCacheLifecycleBridge;
import ai.timefold.solver.core.impl.heuristic.selector.common.SelectionCacheLifecycleListener;
import ai.timefold.solver.core.impl.heuristic.selector.common.demand.ReachableValueMatrix;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;

/**
 * The decorator returns a list of reachable values for a specific value.
 * It enables the creation of a filtering tier when using entity value selectors,
 * ensuring only valid and reachable values are returned.
 * 
 * e1 = entity_range[v1, v2, v3]
 * e2 = entity_range[v1, v4]
 * 
 * v1 = [v2, v3, v4]
 * v2 = [v1, v3]
 * v3 = [v1, v2]
 * v4 = [v1]
 * 
 * @param <Solution_> the solution type
 */
public final class FilteringValueRangeSelector<Solution_> extends AbstractDemandEnabledSelector<Solution_>
        implements IterableValueSelector<Solution_>, SelectionCacheLifecycleListener<Solution_> {

    private final IterableValueSelector<Solution_> nonReplayingValueSelector;
    private final IterableValueSelector<Solution_> replayingValueSelector;
    private final boolean randomSelection;

    private long valuesSize;
    private ListVariableStateSupply<Solution_> listVariableStateSupply;

    private long cachedEntityListRevision;
    private ReachableValueMatrix reachableValueMatrix;

    public FilteringValueRangeSelector(IterableValueSelector<Solution_> nonReplayingValueSelector,
            IterableValueSelector<Solution_> replayingValueSelector, boolean randomSelection) {
        this.nonReplayingValueSelector = nonReplayingValueSelector;
        this.replayingValueSelector = replayingValueSelector;
        this.randomSelection = randomSelection;
        phaseLifecycleSupport.addEventListener(new SelectionCacheLifecycleBridge<>(SelectionCacheType.STEP, this));
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void constructCache(SolverScope<Solution_> solverScope) {
        loadEntityMatrix(solverScope.getScoreDirector());
    }

    @Override
    public void disposeCache(SolverScope<Solution_> solverScope) {
        // Dispose only at the end of the phase
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        this.nonReplayingValueSelector.solvingStarted(solverScope);
        this.replayingValueSelector.solvingStarted(solverScope);
        this.listVariableStateSupply = solverScope.getScoreDirector()
                .getListVariableStateSupply(
                        (ListVariableDescriptor<Solution_>) nonReplayingValueSelector.getVariableDescriptor());
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        this.nonReplayingValueSelector.phaseStarted(phaseScope);
        this.replayingValueSelector.phaseStarted(phaseScope);
        loadEntityMatrix(phaseScope.getScoreDirector());
        valuesSize = reachableValueMatrix.getSize();
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        this.nonReplayingValueSelector.phaseEnded(phaseScope);
        this.replayingValueSelector.phaseEnded(phaseScope);
        this.reachableValueMatrix = null;
    }

    private void loadEntityMatrix(InnerScoreDirector<Solution_, ?> scoreDirector) {
        if (reachableValueMatrix == null || scoreDirector.isWorkingEntityListDirty(cachedEntityListRevision)) {
            var demand = scoreDirector.getValueRangeManager()
                    .getDemand(nonReplayingValueSelector.getVariableDescriptor().getValueRangeDescriptor());
            this.reachableValueMatrix = scoreDirector.getSupplyManager().demand(demand).read();
            this.cachedEntityListRevision = scoreDirector.getWorkingEntityListRevision();
        }
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public IterableValueSelector<Solution_> getChildValueSelector() {
        return nonReplayingValueSelector;
    }

    @Override
    public GenuineVariableDescriptor<Solution_> getVariableDescriptor() {
        return nonReplayingValueSelector.getVariableDescriptor();
    }

    @Override
    public boolean isCountable() {
        return nonReplayingValueSelector.isCountable();
    }

    @Override
    public boolean isNeverEnding() {
        return nonReplayingValueSelector.isNeverEnding();
    }

    @Override
    public long getSize(Object entity) {
        return getSize();
    }

    @Override
    public long getSize() {
        return valuesSize;
    }

    @Override
    public Iterator<Object> iterator(Object entity) {
        return iterator();
    }

    @Override
    public Iterator<Object> iterator() {
        if (randomSelection) {
            return new RandomFilteringValueRangeIterator(replayingValueSelector.iterator(), listVariableStateSupply,
                    workingRandom, (int) getSize());
        } else {
            return new OriginalFilteringValueRangeIterator(replayingValueSelector.iterator(),
                    nonReplayingValueSelector.iterator(), listVariableStateSupply);
        }
    }

    @Override
    public Iterator<Object> endingIterator(Object entity) {
        return new OriginalFilteringValueRangeIterator(replayingValueSelector.iterator(),
                nonReplayingValueSelector.iterator(), listVariableStateSupply);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FilteringValueRangeSelector<?> that
                && Objects.equals(nonReplayingValueSelector, that.nonReplayingValueSelector)
                && Objects.equals(replayingValueSelector, that.replayingValueSelector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nonReplayingValueSelector, replayingValueSelector);
    }

    private abstract class AbstractFilteringValueRangeIterator extends UpcomingSelectionIterator<Object> {

        private final ListVariableStateSupply<Solution_> listVariableStateSupply;
        Object currentUpcomingValue;
        Set<Object> valuesCacheSet;
        Set<Object> entitiesCacheSet;

        private AbstractFilteringValueRangeIterator(ListVariableStateSupply<Solution_> listVariableStateSupply) {
            this.listVariableStateSupply = listVariableStateSupply;
        }

        boolean isEntityReachable(Object value) {
            // Test if the assigned entity is valid
            var assignedEntityPosition = listVariableStateSupply.getElementPosition(value);
            if (assignedEntityPosition instanceof PositionInList elementPosition) {
                return entitiesCacheSet.contains(elementPosition.entity());
            }
            // Unassigned element is valid
            return true;
        }

        boolean isValueOrEntityReachable(Object value) {
            // Test if the value is valid first
            if (!valuesCacheSet.contains(value)) {
                return false;
            }
            // Test if the assigned entity is valid
            var assignedEntityPosition = listVariableStateSupply.getElementPosition(value);
            if (assignedEntityPosition instanceof PositionInList elementPosition) {
                return entitiesCacheSet.contains(elementPosition.entity());
            }
            // Unassigned element is valid
            return true;
        }
    }

    private class OriginalFilteringValueRangeIterator extends AbstractFilteringValueRangeIterator {
        // The value iterator that only replays the current selected value
        private final Iterator<Object> replayingValueIterator;
        // The value iterator returns all possible values based on its settings.
        // However,
        // it may include invalid values that need to be filtered out.
        // This iterator must be used to ensure that all positions are included in the CH phase.
        // This does not apply to the LS phase.
        private final Iterator<Object> valueIterator;
        private boolean initialized = false;
        private boolean hasData = false;

        private OriginalFilteringValueRangeIterator(Iterator<Object> replayingValueIterator, Iterator<Object> valueIterator,
                ListVariableStateSupply<Solution_> listVariableStateSupply) {
            super(listVariableStateSupply);
            this.replayingValueIterator = replayingValueIterator;
            this.valueIterator = valueIterator;
        }

        private void initialize() {
            if (initialized) {
                return;
            }
            if (!valueIterator.hasNext()) {
                this.hasData = false;
            } else if (replayingValueIterator.hasNext()) {
                this.currentUpcomingValue = Objects.requireNonNull(replayingValueIterator.next());
                this.entitiesCacheSet = reachableValueMatrix.extractReachableEntities(currentUpcomingValue);
                this.valuesCacheSet = reachableValueMatrix.extractReachableValues(currentUpcomingValue);
                this.hasData = true;
            }
            this.initialized = true;
        }

        @Override
        protected Object createUpcomingSelection() {
            initialize();
            if (!hasData) {
                return noUpcomingSelection();
            }
            Object next;
            do {
                if (!valueIterator.hasNext()) {
                    return noUpcomingSelection();
                }
                next = valueIterator.next();
            } while (!isValueOrEntityReachable(next));
            return next;
        }
    }

    private class RandomFilteringValueRangeIterator extends AbstractFilteringValueRangeIterator {

        private final Iterator<Object> replayingValueIterator;
        private final Random workingRandom;
        private final int maxBailoutSize;
        private List<Object> valueList;

        private RandomFilteringValueRangeIterator(Iterator<Object> replayingValueIterator,
                ListVariableStateSupply<Solution_> listVariableStateSupply, Random workingRandom, int maxBailoutSize) {
            super(listVariableStateSupply);
            this.replayingValueIterator = replayingValueIterator;
            this.workingRandom = workingRandom;
            this.maxBailoutSize = maxBailoutSize;
        }

        private void initialize() {
            if (valueList != null) {
                return;
            }
            currentUpcomingValue = Objects.requireNonNull(replayingValueIterator.next());
            loadValues();
        }

        private void loadValues() {
            var allValues = reachableValueMatrix.extractReachableValuesAsList(currentUpcomingValue);
            if (allValues != null) {
                this.valueList = allValues;
                this.entitiesCacheSet = reachableValueMatrix.extractReachableEntities(currentUpcomingValue);
            } else {
                this.valueList = Collections.emptyList();
            }
        }

        @Override
        public boolean hasNext() {
            if (replayingValueIterator.hasNext() && currentUpcomingValue != null) {
                var updatedUpcomingValue = replayingValueIterator.next();
                if (updatedUpcomingValue != currentUpcomingValue) {
                    // The iterator is reused in the ElementPositionRandomIterator,
                    // even if the entity has changed.
                    // Therefore,
                    // we need to update the value list to ensure it is consistent.
                    this.currentUpcomingValue = updatedUpcomingValue;
                    loadValues();
                }
            }
            return super.hasNext();
        }

        @Override
        protected Object createUpcomingSelection() {
            initialize();
            if (valueList.isEmpty()) {
                return noUpcomingSelection();
            }
            Object next;
            var bailoutSize = maxBailoutSize;
            do {
                if (bailoutSize <= 0) {
                    return noUpcomingSelection();
                }
                bailoutSize--;
                var index = workingRandom.nextInt(valueList.size());
                next = valueList.get(index);
            } while (!isEntityReachable(next));
            return next;
        }

    }

}
