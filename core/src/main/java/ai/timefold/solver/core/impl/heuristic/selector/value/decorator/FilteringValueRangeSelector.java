package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractDemandEnabledSelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.ReachableValues;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListSwapMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListSwapMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelectorFactory;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The decorator returns a list of reachable values for a specific value.
 * It enables the creation of a filtering tier when using entity-provided value ranges,
 * ensuring only valid and reachable values are returned.
 * A value is considered reachable to another value if both exist within their respective entity value ranges.
 * <p>
 * The decorator can only be applied to list variables.
 * <p>
 * <code>
 *
 * e1 = entity_range[v1, v2, v3]
 *
 * e2 = entity_range[v1, v4]
 *
 * v1 = [v2, v3, v4]
 *
 * v2 = [v1, v3]
 *
 * v3 = [v1, v2]
 *
 * v4 = [v1]
 *
 * </code>
 * <p>
 * This node is currently used by the {@link ListChangeMoveSelector} and {@link ListSwapMoveSelector} selectors.
 * To illustrate its usage, let’s assume how moves are generated for the list swap type.
 * Initially, the swap move selector used a left value selector to choose a value.
 * After that, it uses a right value selector to choose another value to swap them.
 * <p>
 * Based on the previously described process and the current goal of this node,
 * we can observe that once a value is selected using the left value selector,
 * the right node can filter out all non-reachable values and generate a valid move.
 * A move is considered valid only if both entities accept each other's values.
 * The filtering process of invalid values allows the solver to explore the solution space more efficiently.
 *
 * @see ListChangeMoveSelectorFactory
 * @see DestinationSelectorFactory
 * @see ListSwapMoveSelectorFactory
 * @see ValueSelectorFactory
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
    private abstract class AbstractFilteringValueRangeIterator implements Iterator<Object> {
        private final Supplier<Object> upcomingValueSupplier;
        private final ListVariableStateSupply<Solution_> listVariableStateSupply;
        private final ReachableValues reachableValues;
        private final boolean checkSourceAndDestination;
        private boolean initialized = false;
        private boolean hasData = false;
        @Nullable
        private Object currentUpcomingValue;
        @Nullable
        private Object currentUpcomingEntity;
        @Nullable
        private List<Object> currentUpcomingList;

        AbstractFilteringValueRangeIterator(Supplier<Object> upcomingValueSupplier, ReachableValues reachableValues,
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
            if (currentUpcomingValue != null) {
                var updatedUpcomingValue = upcomingValueSupplier.get();
                if (updatedUpcomingValue != currentUpcomingValue) {
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
         * @param upcomingValue the upcoming value
         */
        private void loadValues(@Nullable Object upcomingValue) {
            if (upcomingValue == null) {
                noData();
                return;
            }
            if (upcomingValue == currentUpcomingValue) {
                return;
            }
            currentUpcomingValue = upcomingValue;
            currentUpcomingEntity = null;
            currentUpcomingList = null;
            if (checkSourceAndDestination) {
                // Load the current assigned entity of the selected value
                var position = listVariableStateSupply.getElementPosition(currentUpcomingValue);
                if (position instanceof PositionInList positionInList) {
                    currentUpcomingEntity = positionInList.entity();
                }
            }
            currentUpcomingList = reachableValues.extractValuesAsList(currentUpcomingValue);
            processUpcomingValue(currentUpcomingValue, currentUpcomingList);
            this.hasData = !currentUpcomingList.isEmpty();
            this.initialized = true;
        }

        abstract void processUpcomingValue(Object upcomingValue, List<Object> upcomingList);

        boolean hasNoData() {
            return !hasData;
        }

        private void noData() {
            this.currentUpcomingEntity = null;
            this.hasData = false;
            this.initialized = true;
            this.currentUpcomingList = Collections.emptyList();
        }

        boolean isReachable(Object destinationValue) {
            Object destinationEntity = null;
            var assignedDestinationPosition = listVariableStateSupply.getElementPosition(destinationValue);
            if (assignedDestinationPosition instanceof PositionInList elementPosition) {
                destinationEntity = elementPosition.entity();
            }
            if (checkSourceAndDestination) {
                return reachableValues.isEntityReachable(Objects.requireNonNull(currentUpcomingValue), destinationEntity)
                        && reachableValues.isEntityReachable(Objects.requireNonNull(destinationValue), currentUpcomingEntity);
            } else {
                return reachableValues.isEntityReachable(Objects.requireNonNull(currentUpcomingValue), destinationEntity);
            }
        }
    }

    private class OriginalFilteringValueRangeIterator extends AbstractFilteringValueRangeIterator {
        // The value iterator returns all reachable values
        private Iterator<Object> reachableValueIterator;
        private Object selected = null;

        private OriginalFilteringValueRangeIterator(Supplier<Object> upcomingValueSupplier, ReachableValues reachableValues,
                                                    ListVariableStateSupply<Solution_> listVariableStateSupply, boolean checkSourceAndDestination) {
            super(upcomingValueSupplier, reachableValues, listVariableStateSupply, checkSourceAndDestination);
        }

        @Override
        void processUpcomingValue(Object upcomingValue, List<Object> upcomingList) {
            reachableValueIterator = Objects.requireNonNull(upcomingList).iterator();
            this.selected = null;
        }

        @Override
        public boolean hasNext() {
            this.selected = pickNext();
            return selected != null;
        }

        private Object pickNext() {
            if (selected != null) {
                throw new IllegalStateException("The next value has already been picked.");
            }
            initialize();
            this.selected = null;
            while (reachableValueIterator.hasNext()) {
                var value = reachableValueIterator.next();
                if (isReachable(value)) {
                    return value;
                }
            }
            return null;
        }

        @Override
        public Object next() {
            if (selected == null) {
                throw new NoSuchElementException();
            }
            var result = selected;
            this.selected = null;
            return result;
        }
    }

    private class RandomFilteringValueRangeIterator extends AbstractFilteringValueRangeIterator {

        private final Random workingRandom;
        private int maxBailoutSize = 1;
        private Object replayedValue;
        private List<Object> reachableValueList = null;

        private RandomFilteringValueRangeIterator(Supplier<Object> upcomingValueSupplier, ReachableValues reachableValues,
                                                  ListVariableStateSupply<Solution_> listVariableStateSupply, Random workingRandom,
                                                  boolean checkSourceAndDestination) {
            super(upcomingValueSupplier, reachableValues, listVariableStateSupply, checkSourceAndDestination);
            this.workingRandom = workingRandom;
        }

        @Override
        void processUpcomingValue(Object upcomingValue, List<Object> upcomingList) {
            this.replayedValue = upcomingValue;
            this.reachableValueList = upcomingList;
            this.maxBailoutSize = upcomingList.size();
        }

        @Override
        public boolean hasNext() {
            checkUpcomingValue();
            return reachableValues != null && !reachableValueList.isEmpty();
        }

        @Override
        public Object next() {
            if (hasNoData()) {
                throw new NoSuchElementException();
            }
            Object next;
            var bailoutSize = maxBailoutSize;
            do {
                bailoutSize--;
                var index = workingRandom.nextInt(Objects.requireNonNull(reachableValueList).size());
                next = reachableValueList.get(index);
                if (isReachable(next)) {
                    return next;
                }
            } while (bailoutSize > 0);
            // if a valid move is not found with the given bailout size,
            // we assign the same value to the left side, which will result in a non-doable move
            return replayedValue;
        }
    }

}
