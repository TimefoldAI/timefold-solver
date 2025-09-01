package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import java.util.Iterator;
import java.util.Objects;
import java.util.Random;
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

    private Integer replayedValueOrdinal = -1;
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
    private Integer selectReplayedValue() {
        var iterator = replayingValueSelector.iterator();
        if (iterator.hasNext()) {
            replayedValueOrdinal = reachableValues.getValueOrdinal(iterator.next());
        }
        return replayedValueOrdinal;
    }

    @Override
    public Iterator<Object> iterator() {
        if (randomSelection) {
            return new RandomFilteringValueRangeIterator(this::selectReplayedValue, reachableValues,
                    listVariableStateSupply, workingRandom, checkSourceAndDestination);
        } else {
            return new OriginalFilteringValueRangeIterator(this::selectReplayedValue, reachableValues,
                    listVariableStateSupply, checkSourceAndDestination);
        }
    }

    @Override
    public Iterator<Object> endingIterator(Object entity) {
        return new OriginalFilteringValueRangeIterator(this::selectReplayedValue, reachableValues,
                listVariableStateSupply, checkSourceAndDestination);
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
        private final Supplier<Integer> upcomingValueSupplier;
        private final ListVariableStateSupply<Solution_> listVariableStateSupply;
        private final ReachableValues reachableValues;
        private final boolean checkSourceAndDestination;
        private boolean initialized = false;
        private boolean hasData = false;
        private int currentUpcomingValueOrdinal = -1;
        @Nullable
        private Integer currentUpcomingEntityOrdinal;

        AbstractFilteringValueRangeIterator(Supplier<Integer> upcomingValueSupplier, ReachableValues reachableValues,
                ListVariableStateSupply<Solution_> listVariableStateSupply, boolean checkSourceAndDestination) {
            this.upcomingValueSupplier = upcomingValueSupplier;
            this.reachableValues = Objects.requireNonNull(reachableValues);
            this.listVariableStateSupply = listVariableStateSupply;
            this.checkSourceAndDestination = checkSourceAndDestination;
        }

        void initialize() {
            if (initialized) {
                return;
            }
            checkUpcomingValue();
        }

        void checkUpcomingValue() {
            if (currentUpcomingValueOrdinal != -1) {
                var updatedUpcomingValue = upcomingValueSupplier.get();
                if (!updatedUpcomingValue.equals(currentUpcomingValueOrdinal)) {
                    // The iterator may be reused
                    // like in the ElementPositionRandomIterator,
                    // even if the entity has changed.
                    // Therefore,
                    // we need to update the value list to ensure it is consistent.
                    loadValues(updatedUpcomingValue);
                }
            } else {
                loadValues(upcomingValueSupplier.get());
            }
        }

        /**
         * This method initializes the basic structure required for the child iterators,
         * including the upcoming entity and the upcoming list.
         * 
         * @param upcomingValueOrdinal the upcoming value ordinal
         */
        private void loadValues(@Nullable Integer upcomingValueOrdinal) {
            if (upcomingValueOrdinal == null) {
                noData();
                return;
            }
            if (upcomingValueOrdinal.equals(currentUpcomingValueOrdinal)) {
                return;
            }
            currentUpcomingValueOrdinal = upcomingValueOrdinal;
            currentUpcomingEntityOrdinal = null;
            if (checkSourceAndDestination) {
                // Load the current assigned entity of the selected value
                var position =
                        listVariableStateSupply.getElementPosition(reachableValues.getValue(currentUpcomingValueOrdinal));
                if (position instanceof PositionInList positionInList) {
                    currentUpcomingEntityOrdinal = reachableValues.getEntityOrdinal(positionInList.entity());
                }
            }
            upcomingCreated = false;
            this.hasData = processUpcomingValue(currentUpcomingValueOrdinal, reachableValues);
            this.initialized = true;
        }

        abstract boolean processUpcomingValue(int upcomingValueOrdinal, ReachableValues reachableValues);

        boolean hasNoData() {
            return !hasData;
        }

        private void noData() {
            this.currentUpcomingEntityOrdinal = null;
            this.hasData = false;
            this.initialized = true;
        }

        boolean isReachable(Integer destinationValueOrdinal) {
            Integer destinationEntityOrdinal = null;
            var assignedDestinationPosition =
                    listVariableStateSupply.getElementPosition(reachableValues.getValue(destinationValueOrdinal));
            if (assignedDestinationPosition instanceof PositionInList elementPosition) {
                destinationEntityOrdinal = reachableValues.getEntityOrdinal(elementPosition.entity());
            }
            if (checkSourceAndDestination) {
                return reachableValues.isEntityReachable(currentUpcomingValueOrdinal, destinationEntityOrdinal)
                        && reachableValues.isEntityReachable(destinationValueOrdinal, currentUpcomingEntityOrdinal);
            } else {
                return reachableValues.isEntityReachable(currentUpcomingValueOrdinal, destinationEntityOrdinal);
            }
        }

        Object currentUpcomingValue() {
            return reachableValues.getValue(currentUpcomingValueOrdinal);
        }
    }

    private class OriginalFilteringValueRangeIterator extends AbstractFilteringValueRangeIterator {
        // The value iterator returns all reachable values
        private Iterator<Integer> reachableValueIterator;

        private OriginalFilteringValueRangeIterator(Supplier<Integer> upcomingValueSupplier, ReachableValues reachableValues,
                ListVariableStateSupply<Solution_> listVariableStateSupply, boolean checkSourceAndDestination) {
            super(upcomingValueSupplier, reachableValues, listVariableStateSupply, checkSourceAndDestination);
        }

        @Override
        boolean processUpcomingValue(int upcomingOrdinal, ReachableValues reachableValues) {
            reachableValueIterator = reachableValues.getOriginalValueIterator(upcomingOrdinal);
            return reachableValueIterator.hasNext();
        }

        @Override
        protected Object createUpcomingSelection() {
            initialize();
            if (hasNoData()) {
                return noUpcomingSelection();
            }
            Integer nextOrdinal;
            do {
                if (!reachableValueIterator.hasNext()) {
                    return noUpcomingSelection();
                }
                nextOrdinal = reachableValueIterator.next();
            } while (!isReachable(nextOrdinal));
            return reachableValues.getValue(nextOrdinal);
        }
    }

    private class RandomFilteringValueRangeIterator extends AbstractFilteringValueRangeIterator {

        private final Random workingRandom;
        private Iterator<Integer> reachableValueIterator = null;

        private RandomFilteringValueRangeIterator(Supplier<Integer> upcomingValueSupplier, ReachableValues reachableValues,
                ListVariableStateSupply<Solution_> listVariableStateSupply, Random workingRandom,
                boolean checkSourceAndDestination) {
            super(upcomingValueSupplier, reachableValues, listVariableStateSupply, checkSourceAndDestination);
            this.workingRandom = workingRandom;
        }

        @Override
        boolean processUpcomingValue(int upcomingOrdinal, ReachableValues reachableValues) {
            this.reachableValueIterator = reachableValues.getRandomValueIterator(upcomingOrdinal, workingRandom);
            return reachableValueIterator.hasNext();
        }

        @Override
        public boolean hasNext() {
            checkUpcomingValue();
            var hasNext = super.hasNext();
            if (!hasNext && reachableValueIterator != null && reachableValueIterator.hasNext()) {
                // if a valid move is not found with the given bailout size,
                // we can still use the iterator as long as the currentUpcomingList is not empty
                this.upcomingCreated = true;
                this.hasUpcomingSelection = true;
                // We assigned the same value to the left side, which will result in a non-doable move
                this.upcomingSelection = currentUpcomingValue();
                return true;
            }
            return hasNext;
        }

        @Override
        protected Object createUpcomingSelection() {
            initialize();
            if (hasNoData()) {
                return noUpcomingSelection();
            }
            var next = reachableValueIterator.next();
            if (isReachable(next)) {
                return reachableValues.getValue(next);
            }
            return noUpcomingSelection();
        }

    }

}
