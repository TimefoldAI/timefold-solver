package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractDemandEnabledSelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.ReachableValues;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The decorator returns a list of reachable values for a specific value.
 * It enables the creation of a filtering tier when using entity-provided value ranges,
 * ensuring only valid and reachable values are returned.
 * <p>
 * e1 = entity_range[v1, v2, v3]
 * e2 = entity_range[v1, v4]
 * <p>
 * v1 = [v2, v3, v4]
 * v2 = [v1, v3]
 * v3 = [v1, v2]
 * v4 = [v1]
 * 
 * @param <Solution_> the solution type
 */
public final class FilteringValueRangeSelector<Solution_> extends AbstractDemandEnabledSelector<Solution_>
        implements IterableValueSelector<Solution_> {

    private final IterableValueSelector<Solution_> nonReplayingValueSelector;
    private final IterableValueSelector<Solution_> replayingValueSelector;
    private final boolean randomSelection;

    private long valuesSize;
    private ListVariableStateSupply<Solution_> listVariableStateSupply;
    private ReachableValues reachableValues;

    private final boolean checkSourceAndDestination;

    public FilteringValueRangeSelector(IterableValueSelector<Solution_> nonReplayingValueSelector,
            IterableValueSelector<Solution_> replayingValueSelector, boolean randomSelection,
            boolean checkSourceAndDestination) {
        this.nonReplayingValueSelector = nonReplayingValueSelector;
        this.replayingValueSelector = replayingValueSelector;
        this.randomSelection = randomSelection;
        this.checkSourceAndDestination = checkSourceAndDestination;
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        this.nonReplayingValueSelector.solvingStarted(solverScope);
        this.replayingValueSelector.solvingStarted(solverScope);
        this.listVariableStateSupply = solverScope.getScoreDirector().getListVariableStateSupply(
                (ListVariableDescriptor<Solution_>) nonReplayingValueSelector.getVariableDescriptor());
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        this.nonReplayingValueSelector.phaseStarted(phaseScope);
        this.replayingValueSelector.phaseStarted(phaseScope);
        this.reachableValues = phaseScope.getScoreDirector().getValueRangeManager()
                .getReachableValues(listVariableStateSupply.getSourceVariableDescriptor());
        valuesSize = reachableValues.getSize();
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        this.nonReplayingValueSelector.phaseEnded(phaseScope);
        this.replayingValueSelector.phaseEnded(phaseScope);
        this.reachableValues = null;
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
            // If the nonReplayingValueSelector does not have any additional configuration,
            // we can bypass it and only use reachable values,
            // which helps optimize the number of evaluations.
            // However, if the nonReplayingValueSelector includes custom configurations,
            // such as filtering,
            // we will first evaluate its values and then filter out those that are not reachable.
            if (nonReplayingValueSelector instanceof IterableFromEntityPropertyValueSelector<Solution_>) {
                return new OptimizedRandomFilteringValueRangeIterator(replayingValueSelector.iterator(),
                        listVariableStateSupply,
                        reachableValues, workingRandom, (int) getSize(), checkSourceAndDestination);
            } else {
                return new RandomFilteringValueRangeIterator(replayingValueSelector.iterator(),
                        nonReplayingValueSelector.iterator(), listVariableStateSupply, reachableValues, (int) getSize(),
                        checkSourceAndDestination);
            }
        } else {
            return new OriginalFilteringValueRangeIterator(replayingValueSelector.iterator(),
                    nonReplayingValueSelector.iterator(), listVariableStateSupply, reachableValues, checkSourceAndDestination);
        }
    }

    @Override
    public Iterator<Object> endingIterator(Object entity) {
        return new OriginalFilteringValueRangeIterator(replayingValueSelector.iterator(),
                nonReplayingValueSelector.iterator(), listVariableStateSupply, reachableValues, checkSourceAndDestination);
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

    @NullMarked
    private abstract class AbstractFilteringValueRangeIterator extends UpcomingSelectionIterator<Object> {

        private final ListVariableStateSupply<Solution_> listVariableStateSupply;
        private final ReachableValues reachableValues;
        // Check if the source and destination entity range accepts the selected values 
        private final boolean checkSourceAndDestination;
        // Use the value list instead of the set, as it is required by random access iterators
        private final boolean useValueList;
        boolean initialized = false;
        boolean hasData = false;
        @Nullable
        Object currentUpcomingValue;
        @Nullable
        Object currentUpcomingEntity;
        @Nullable
        Set<Object> valuesSet;
        @Nullable
        List<Object> valueList;
        @Nullable
        Set<Object> entitiesSet;

        AbstractFilteringValueRangeIterator(ListVariableStateSupply<Solution_> listVariableStateSupply,
                ReachableValues reachableValues, boolean checkSourceAndDestination, boolean useValueList) {
            this.reachableValues = Objects.requireNonNull(reachableValues);
            this.listVariableStateSupply = listVariableStateSupply;
            this.checkSourceAndDestination = checkSourceAndDestination;
            this.useValueList = useValueList;
        }

        void loadValues(Object upcomingValue) {
            this.currentUpcomingValue = upcomingValue;
            this.entitiesSet = reachableValues.extractEntities(currentUpcomingValue);
            this.valueList = null;
            this.valuesSet = null;
            if (useValueList) {
                // Load the random access list
                valueList = Objects.requireNonNull(reachableValues.extractValuesAsList(currentUpcomingValue));
                if (valueList.isEmpty()) {
                    noData();
                    return;
                }
            } else {
                // Load the fast access set
                this.valuesSet = reachableValues.extractValues(currentUpcomingValue);
                if (valuesSet == null || valuesSet.isEmpty()) {
                    noData();
                    return;
                }
            }
            currentUpcomingEntity = null;
            if (checkSourceAndDestination) {
                // Load the current assigned entity of the selected value
                var position = listVariableStateSupply.getElementPosition(currentUpcomingValue);
                if (position instanceof PositionInList positionInList) {
                    currentUpcomingEntity = positionInList.entity();
                }
            }
            this.hasData = true;
            this.initialized = true;
        }

        void noData() {
            this.entitiesSet = null;
            this.valuesSet = null;
            this.valueList = null;
            this.currentUpcomingEntity = null;
            this.hasData = false;
            this.initialized = true;
        }

        boolean isEntityReachable(Object destinationValue) {
            var destinationValid = true;
            var sourceValid = true;
            // Test if the destination entity accepts the selected value
            var assignedDestinationPosition = listVariableStateSupply.getElementPosition(destinationValue);
            if (assignedDestinationPosition instanceof PositionInList elementPosition) {
                destinationValid = entitiesSet.contains(elementPosition.entity());
            }
            if (checkSourceAndDestination && destinationValid && currentUpcomingEntity != null) {
                // Test if the source entity accepts the destination value
                sourceValid = Objects.requireNonNull(reachableValues.extractEntities(destinationValue))
                        .contains(currentUpcomingEntity);
            }
            return sourceValid && destinationValid;
        }

        boolean isValueOrEntityReachable(Object destinationValue) {
            // Test if the source accepts the destination value
            // It is unnecessary to check if the destination accepts the upcoming value,
            // as it is assumed that the related value selector will only return values reachable from the currently selected value
            if (!valuesSet.contains(destinationValue)) {
                return false;
            }
            return isEntityReachable(destinationValue);
        }
    }

    private abstract class AbstractUpcomingValueRangeIterator extends AbstractFilteringValueRangeIterator {
        // The value iterator that only replays the current selected value
        final Iterator<Object> replayingValueIterator;
        // The value iterator returns all possible values based on the outer selector settings.
        final Iterator<Object> valueIterator;

        private AbstractUpcomingValueRangeIterator(Iterator<Object> replayingValueIterator, Iterator<Object> valueIterator,
                ListVariableStateSupply<Solution_> listVariableStateSupply, ReachableValues reachableValues,
                boolean checkSourceAndDestination, boolean useValueList) {
            super(listVariableStateSupply, reachableValues, checkSourceAndDestination, useValueList);
            this.replayingValueIterator = replayingValueIterator;
            this.valueIterator = valueIterator;
        }

        void initialize() {
            if (initialized) {
                return;
            }
            if (replayingValueIterator.hasNext()) {
                var upcomingValue = replayingValueIterator.next();
                if (!valueIterator.hasNext()) {
                    noData();
                } else {
                    loadValues(Objects.requireNonNull(upcomingValue));
                }
            } else {
                noData();
            }
        }
    }

    private class OriginalFilteringValueRangeIterator extends AbstractUpcomingValueRangeIterator {

        private OriginalFilteringValueRangeIterator(Iterator<Object> replayingValueIterator, Iterator<Object> valueIterator,
                ListVariableStateSupply<Solution_> listVariableStateSupply, ReachableValues reachableValues,
                boolean checkSourceAndDestination) {
            super(replayingValueIterator, valueIterator, listVariableStateSupply, reachableValues, checkSourceAndDestination,
                    false);
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

    private class RandomFilteringValueRangeIterator extends AbstractUpcomingValueRangeIterator {
        private final int maxBailoutSize;

        private RandomFilteringValueRangeIterator(Iterator<Object> replayingValueIterator, Iterator<Object> valueIterator,
                ListVariableStateSupply<Solution_> listVariableStateSupply, ReachableValues reachableValues,
                int maxBailoutSize, boolean checkSourceAndDestination) {
            super(replayingValueIterator, valueIterator, listVariableStateSupply, reachableValues, checkSourceAndDestination,
                    false);
            this.maxBailoutSize = maxBailoutSize;
        }

        @Override
        protected Object createUpcomingSelection() {
            initialize();
            if (!hasData) {
                return noUpcomingSelection();
            }
            Object next;
            var bailoutSize = maxBailoutSize;
            do {
                if (bailoutSize <= 0 || !valueIterator.hasNext()) {
                    return noUpcomingSelection();
                }
                bailoutSize--;
                next = valueIterator.next();
            } while (!isValueOrEntityReachable(next));
            return next;
        }
    }

    /**
     * The optimized iterator only traverses reachable values from the current selection.
     * Unlike {@link RandomFilteringValueRangeIterator},
     * it does not use an outer iterator to filter out non-reachable values.
     */
    private class OptimizedRandomFilteringValueRangeIterator extends AbstractFilteringValueRangeIterator {

        private final Iterator<Object> replayingValueIterator;
        private final Random workingRandom;
        private final int maxBailoutSize;

        private OptimizedRandomFilteringValueRangeIterator(Iterator<Object> replayingValueIterator,
                ListVariableStateSupply<Solution_> listVariableStateSupply, ReachableValues reachableValues,
                Random workingRandom, int maxBailoutSize, boolean checkSourceAndDestination) {
            super(listVariableStateSupply, reachableValues, checkSourceAndDestination, true);
            this.replayingValueIterator = replayingValueIterator;
            this.workingRandom = workingRandom;
            this.maxBailoutSize = maxBailoutSize;
        }

        private void initialize() {
            if (initialized) {
                return;
            }
            loadValues(Objects.requireNonNull(replayingValueIterator.next()));
        }

        @Override
        public boolean hasNext() {
            if (replayingValueIterator.hasNext() && currentUpcomingValue != null) {
                var updatedUpcomingValue = replayingValueIterator.next();
                if (updatedUpcomingValue != currentUpcomingValue) {
                    // The iterator may be reused
                    // like in the ElementPositionRandomIterator,
                    // even if the entity has changed.
                    // Therefore,
                    // we need to update the value list to ensure it is consistent.
                    loadValues(updatedUpcomingValue);
                }
            }
            return super.hasNext();
        }

        @Override
        protected Object createUpcomingSelection() {
            initialize();
            if (!hasData) {
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
