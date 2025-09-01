package ai.timefold.solver.core.impl.heuristic.selector.entity.decorator;

import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

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
 * <p>
 * The decorator can only be applied to list variables.
 * <p>
 * e1 = entity_range[v1, v2, v3]
 * e2 = entity_range[v1, v4]
 * <p>
 * v1 = [e1, e2]
 * v2 = [e1]
 * v3 = [e1]
 * v4 = [e2]
 *
 * @param <Solution_> the solution type
 */
public final class FilteringEntityByValueSelector<Solution_> extends AbstractDemandEnabledSelector<Solution_>
        implements EntitySelector<Solution_> {

    private final IterableValueSelector<Solution_> replayingValueSelector;
    private final EntitySelector<Solution_> childEntitySelector;
    private final boolean randomSelection;

    private int replayedValueOrdinal = -1;
    private ReachableValues reachableValues;
    private long entitiesSize;

    public FilteringEntityByValueSelector(EntitySelector<Solution_> childEntitySelector,
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
                .getReachableValues(Objects.requireNonNull(
                        phaseScope.getScoreDirector().getSolutionDescriptor().getListVariableDescriptor(),
                        "Impossible state: the list variable cannot be null."));
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
    private Integer selectReplayedValue() {
        var iterator = replayingValueSelector.iterator();
        if (iterator.hasNext()) {
            replayedValueOrdinal = reachableValues.getValueOrdinal(iterator.next());
        }
        return replayedValueOrdinal;
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
        return other instanceof FilteringEntityByValueSelector<?> that
                && Objects.equals(childEntitySelector, that.childEntitySelector)
                && Objects.equals(replayingValueSelector, that.replayingValueSelector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(childEntitySelector, replayingValueSelector);
    }

    private static class OriginalFilteringValueRangeIterator extends UpcomingSelectionIterator<Object> {

        private final Supplier<Integer> upcomingValueSupplier;
        private final ReachableValues reachableValues;
        private Iterator<Integer> entityIterator;

        private OriginalFilteringValueRangeIterator(Supplier<Integer> upcomingValueSupplier, ReachableValues reachableValues) {
            this.reachableValues = Objects.requireNonNull(reachableValues);
            this.upcomingValueSupplier = Objects.requireNonNull(upcomingValueSupplier);
        }

        private void initialize() {
            if (entityIterator != null) {
                return;
            }
            var currentUpcomingValueOrdinal = upcomingValueSupplier.get();
            if (currentUpcomingValueOrdinal == -1) {
                entityIterator = Collections.emptyIterator();
            } else {
                this.entityIterator =
                        reachableValues.getOriginalEntityIterator(Objects.requireNonNull(currentUpcomingValueOrdinal));
            }
        }

        @Override
        protected Object createUpcomingSelection() {
            initialize();
            if (!entityIterator.hasNext()) {
                return noUpcomingSelection();
            }
            return reachableValues.getEntity(entityIterator.next());
        }
    }

    private static class RandomFilteringValueRangeIterator extends UpcomingSelectionIterator<Object> {

        private final Supplier<Integer> upcomingValueSupplier;
        private final ReachableValues reachableValues;
        private final Random workingRandom;
        private int currentUpcomingValueOrdinal = -1;
        private Iterator<Integer> entityIterator;

        private RandomFilteringValueRangeIterator(Supplier<Integer> upcomingValueSupplier, ReachableValues reachableValues,
                Random workingRandom) {
            this.upcomingValueSupplier = upcomingValueSupplier;
            this.reachableValues = Objects.requireNonNull(reachableValues);
            this.workingRandom = workingRandom;
        }

        private void initialize() {
            if (entityIterator != null) {
                return;
            }
            currentUpcomingValueOrdinal = upcomingValueSupplier.get();
            if (currentUpcomingValueOrdinal == -1) {
                entityIterator = Collections.emptyIterator();
            } else {
                loadValues();
            }
        }

        private void loadValues() {
            upcomingCreated = false;
            this.entityIterator = reachableValues.getRandomEntityIterator(currentUpcomingValueOrdinal, workingRandom);
        }

        @Override
        public boolean hasNext() {
            if (currentUpcomingValueOrdinal != -1) {
                var updatedUpcomingValue = upcomingValueSupplier.get();
                if (!updatedUpcomingValue.equals(currentUpcomingValueOrdinal)) {
                    // The iterator is reused in the ElementPositionRandomIterator,
                    // even if the value has changed.
                    // Therefore,
                    // we need to update the value list to ensure it is consistent.
                    currentUpcomingValueOrdinal = updatedUpcomingValue;
                    loadValues();
                }
            }
            return super.hasNext();
        }

        @Override
        protected Object createUpcomingSelection() {
            initialize();
            if (!entityIterator.hasNext()) {
                return noUpcomingSelection();
            }
            return reachableValues.getEntity(entityIterator.next());
        }
    }

}
