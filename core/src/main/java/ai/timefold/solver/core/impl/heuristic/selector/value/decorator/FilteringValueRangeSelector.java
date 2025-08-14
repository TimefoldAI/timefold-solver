package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

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

    private Object replayedValue = null;
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

    /**
     * The expected replayed value corresponds to the selected value when the replaying selector has the next value.
     * Once it is selected, it will be reused until a new value is replayed by the recorder selector.
     */
    private Object selectReplayedValue() {
        var iterator = replayingValueSelector.iterator();
        if (iterator.hasNext()) {
            replayedValue = iterator.next();
        }
        return replayedValue;
    }

    @Override
    public Iterator<Object> iterator() {
        if (randomSelection) {
            return new RandomFilteringValueRangeIterator(this::selectReplayedValue, reachableValues, listVariableStateSupply,
                    workingRandom, (int) getSize(), checkSourceAndDestination);
        } else {
            return new OriginalFilteringValueRangeIterator(this::selectReplayedValue, reachableValues, listVariableStateSupply,
                    checkSourceAndDestination);
        }
    }

    @Override
    public Iterator<Object> endingIterator(Object entity) {
        return new OriginalFilteringValueRangeIterator(this::selectReplayedValue, reachableValues, listVariableStateSupply,
                checkSourceAndDestination);
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

        AbstractFilteringValueRangeIterator(ReachableValues reachableValues,
                ListVariableStateSupply<Solution_> listVariableStateSupply, boolean checkSourceAndDestination,
                boolean useValueList) {
            this.reachableValues = Objects.requireNonNull(reachableValues);
            this.listVariableStateSupply = listVariableStateSupply;
            this.checkSourceAndDestination = checkSourceAndDestination;
            this.useValueList = useValueList;
        }

        void loadValues() {
            if (currentUpcomingValue == null) {
                noData();
                return;
            }
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
            upcomingCreated = false;
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

        boolean isReachable(Object destinationValue) {
            var sourceValid = true;
            var destinationValid = true;
            // Test if the source accepts the destination entity
            var assignedDestinationPosition = listVariableStateSupply.getElementPosition(destinationValue);
            if (assignedDestinationPosition instanceof PositionInList elementPosition) {
                sourceValid = entitiesSet.contains(elementPosition.entity());
            }
            if (checkSourceAndDestination && sourceValid && currentUpcomingEntity != null) {
                // Test if the destination accepts the source entity
                destinationValid = Objects.requireNonNull(reachableValues.extractEntities(destinationValue))
                        .contains(currentUpcomingEntity);
            }
            return sourceValid && destinationValid;
        }
    }

    private class OriginalFilteringValueRangeIterator extends AbstractFilteringValueRangeIterator {
        private final Supplier<Object> upcomingValueSupplier;
        // The value iterator returns all reachable values
        private Iterator<Object> valueIterator;

        private OriginalFilteringValueRangeIterator(Supplier<Object> upcomingValueSupplier, ReachableValues reachableValues,
                ListVariableStateSupply<Solution_> listVariableStateSupply, boolean checkSourceAndDestination) {
            super(reachableValues, listVariableStateSupply, checkSourceAndDestination, false);
            this.upcomingValueSupplier = upcomingValueSupplier;
        }

        void initialize() {
            if (initialized) {
                return;
            }
            this.currentUpcomingValue = upcomingValueSupplier.get();
            loadValues();
            if (hasData) {
                valueIterator = Objects.requireNonNull(valuesSet).iterator();
            } else {
                valueIterator = Collections.emptyIterator();
            }
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
            } while (!isReachable(next));
            return next;
        }
    }

    private class RandomFilteringValueRangeIterator extends AbstractFilteringValueRangeIterator {

        private final Supplier<Object> upcomingValueSupplier;
        private final Random workingRandom;
        private final int maxBailoutSize;

        private RandomFilteringValueRangeIterator(Supplier<Object> upcomingValueSupplier, ReachableValues reachableValues,
                ListVariableStateSupply<Solution_> listVariableStateSupply, Random workingRandom, int maxBailoutSize,
                boolean checkSourceAndDestination) {
            super(reachableValues, listVariableStateSupply, checkSourceAndDestination, true);
            this.upcomingValueSupplier = upcomingValueSupplier;
            this.workingRandom = workingRandom;
            this.maxBailoutSize = maxBailoutSize;
        }

        private void initialize() {
            if (initialized) {
                return;
            }
            this.currentUpcomingValue = upcomingValueSupplier.get();
            loadValues();
        }

        @Override
        public boolean hasNext() {
            if (currentUpcomingValue != null) {
                var updatedUpcomingValue = upcomingValueSupplier.get();
                if (updatedUpcomingValue != currentUpcomingValue) {
                    // The iterator may be reused
                    // like in the ElementPositionRandomIterator,
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
            } while (!isReachable(next));
            return next;
        }

    }

}
