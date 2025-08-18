package ai.timefold.solver.core.impl.heuristic.selector.entity.decorator;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractDemandEnabledSelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.ReachableValues;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * The decorator returns a list of reachable entities for a specific value.
 * It enables the creation of a filtering tier when using entity value selectors,
 * ensuring only valid and reachable entities are returned.
 *
 * e1 = entity_range[v1, v2, v3]
 * e2 = entity_range[v1, v4]
 *
 * v1 = [e1, e2]
 * v2 = [e1]
 * v3 = [e1]
 * v4 = [e2]
 *
 * @param <Solution_> the solution type
 */
public final class FilteringEntityValueRangeSelector<Solution_> extends AbstractDemandEnabledSelector<Solution_>
        implements EntitySelector<Solution_> {

    private final IterableValueSelector<Solution_> replayingValueSelector;
    private final EntitySelector<Solution_> childEntitySelector;
    private final boolean randomSelection;

    private Object replayedValue;
    private ReachableValues reachableValues;
    private long entitiesSize;

    public FilteringEntityValueRangeSelector(EntitySelector<Solution_> childEntitySelector,
            IterableValueSelector<Solution_> replayingValueSelector, boolean randomSelection) {
        this.replayingValueSelector = replayingValueSelector;
        this.childEntitySelector = childEntitySelector;
        this.randomSelection = randomSelection;
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        this.childEntitySelector.solvingStarted(solverScope);
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        this.entitiesSize = childEntitySelector.getEntityDescriptor().extractEntities(phaseScope.getWorkingSolution()).size();
        this.reachableValues = phaseScope.getScoreDirector().getValueRangeManager()
                .getReachableValues(phaseScope.getScoreDirector().getSolutionDescriptor().getListVariableDescriptor());
        if (phaseScope instanceof ConstructionHeuristicPhaseScope) {
            this.reachableValues.enableSort();
        }
        this.childEntitySelector.phaseStarted(phaseScope);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        this.reachableValues = null;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public EntitySelector<Solution_> getChildEntitySelector() {
        return childEntitySelector;
    }

    @Override
    public EntityDescriptor<Solution_> getEntityDescriptor() {
        return childEntitySelector.getEntityDescriptor();
    }

    @Override
    public long getSize() {
        return entitiesSize;
    }

    @Override
    public boolean isCountable() {
        return childEntitySelector.isCountable();
    }

    @Override
    public boolean isNeverEnding() {
        return childEntitySelector.isNeverEnding();
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
    public Iterator<Object> endingIterator() {
        return new OriginalFilteringValueRangeIterator(this::selectReplayedValue, reachableValues);
    }

    @Override
    public Iterator<Object> iterator() {
        if (randomSelection) {
            return new RandomFilteringValueRangeIterator(this::selectReplayedValue, reachableValues, workingRandom);
        } else {
            return new OriginalFilteringValueRangeIterator(this::selectReplayedValue, reachableValues);
        }
    }

    @Override
    public ListIterator<Object> listIterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FilteringEntityValueRangeSelector<?> that
                && Objects.equals(childEntitySelector, that.childEntitySelector)
                && Objects.equals(replayingValueSelector, that.replayingValueSelector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(childEntitySelector, replayingValueSelector);
    }

    private static class OriginalFilteringValueRangeIterator extends UpcomingSelectionIterator<Object> {

        private final Supplier<Object> upcomingValueSupplier;
        private final ReachableValues reachableValues;
        private Iterator<Object> valueIterator;

        private OriginalFilteringValueRangeIterator(Supplier<Object> upcomingValueSupplier, ReachableValues reachableValues) {
            this.reachableValues = Objects.requireNonNull(reachableValues);
            this.upcomingValueSupplier = Objects.requireNonNull(upcomingValueSupplier);
        }

        private void initialize() {
            if (valueIterator != null) {
                return;
            }
            var currentUpcomingValue = upcomingValueSupplier.get();
            if (currentUpcomingValue == null) {
                valueIterator = Collections.emptyIterator();
            } else {
                var allValues = Objects.requireNonNull(reachableValues)
                        .extractEntitiesAsList(Objects.requireNonNull(currentUpcomingValue));
                this.valueIterator = Objects.requireNonNull(allValues).iterator();
            }
        }

        @Override
        protected Object createUpcomingSelection() {
            initialize();
            if (!valueIterator.hasNext()) {
                return noUpcomingSelection();
            }
            return valueIterator.next();
        }
    }

    private static class RandomFilteringValueRangeIterator extends UpcomingSelectionIterator<Object> {

        private final Supplier<Object> upcomingValueSupplier;
        private final ReachableValues reachableValues;
        private final Random workingRandom;
        private Object currentUpcomingValue;
        private List<Object> entityList;

        private RandomFilteringValueRangeIterator(Supplier<Object> upcomingValueSupplier, ReachableValues reachableValues,
                Random workingRandom) {
            this.upcomingValueSupplier = upcomingValueSupplier;
            this.reachableValues = Objects.requireNonNull(reachableValues);
            this.workingRandom = workingRandom;
        }

        private void initialize() {
            if (entityList != null) {
                return;
            }
            currentUpcomingValue = upcomingValueSupplier.get();
            if (currentUpcomingValue == null) {
                entityList = Collections.emptyList();
            } else {
                loadValues();
            }
        }

        private void loadValues() {
            upcomingCreated = false;
            this.entityList = reachableValues.extractEntitiesAsList(currentUpcomingValue);
        }

        @Override
        public boolean hasNext() {
            if (currentUpcomingValue != null) {
                var updatedUpcomingValue = upcomingValueSupplier.get();
                if (updatedUpcomingValue != currentUpcomingValue) {
                    // The iterator is reused in the ElementPositionRandomIterator,
                    // even if the value has changed.
                    // Therefore,
                    // we need to update the value list to ensure it is consistent.
                    currentUpcomingValue = updatedUpcomingValue;
                    loadValues();
                }
            }
            return super.hasNext();
        }

        @Override
        protected Object createUpcomingSelection() {
            initialize();
            if (entityList.isEmpty()) {
                return noUpcomingSelection();
            }
            var index = workingRandom.nextInt(entityList.size());
            return entityList.get(index);
        }
    }

}
