package ai.timefold.solver.core.impl.heuristic.selector.entity.decorator;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacerFactory;
import ai.timefold.solver.core.impl.constructionheuristic.placer.QueuedValuePlacer;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractDemandEnabledSelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.ReachableValues;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * The decorator returns a list of reachable entities for a specific value.
 * It enables the creation of a filtering tier when using entity-provided value ranges,
 * ensuring only valid and reachable entities are returned.
 * An entity is considered reachable to a value if its value range includes that value.
 * <p>
 * The decorator can only be applied to list variables.
 * <p>
 * <code>
 *
 * e1 = entity_range[v1, v2, v3]
 *
 * e2 = entity_range[v1, v4]
 *
 * v1 = [e1, e2]
 *
 * v2 = [e1]
 *
 * v3 = [e1]
 *
 * v4 = [e2]
 *
 * </code>
 * <p>
 * This node is currently used by the {@link QueuedValuePlacer} to build an initial solution.
 * To illustrate its usage, let’s assume how moves are generated.
 * First, a value is selected using a value selector.
 * Then,
 * a change move selector generates all possible moves for that value to the available entities
 * and selects the entity and position with the best score.
 * <p>
 * Considering the previous process and the current goal of this node,
 * we can observe that once a value is selected, only change moves to reachable entities will be generated.
 * This ensures that entities that do not accept the currently selected value will not produce any change moves.
 *
 * @see ListChangeMoveSelectorFactory
 * @see EntityPlacerFactory
 *
 * @param <Solution_> the solution type
 */
public final class FilteringEntityByValueSelector<Solution_> extends AbstractDemandEnabledSelector<Solution_>
        implements EntitySelector<Solution_> {

    private final IterableValueSelector<Solution_> replayingValueSelector;
    private final EntitySelector<Solution_> childEntitySelector;
    private final boolean randomSelection;

    private Object replayedValue;
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
        return other instanceof FilteringEntityByValueSelector<?> that
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
                var allValues = reachableValues.extractEntitiesAsList(Objects.requireNonNull(currentUpcomingValue));
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

    private static class RandomFilteringValueRangeIterator implements Iterator<Object> {

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
            var oldUpcomingValue = currentUpcomingValue;
            currentUpcomingValue = upcomingValueSupplier.get();
            if (currentUpcomingValue == null) {
                entityList = Collections.emptyList();
            } else if (oldUpcomingValue != currentUpcomingValue) {
                loadValues();
            }
        }

        private void loadValues() {
            this.entityList = reachableValues.extractEntitiesAsList(currentUpcomingValue);
        }

        @Override
        public boolean hasNext() {
            initialize();
            return entityList != null && !entityList.isEmpty();
        }

        @Override
        public Object next() {
            if (entityList.isEmpty()) {
                throw new NoSuchElementException();
            }
            var index = workingRandom.nextInt(entityList.size());
            return entityList.get(index);
        }
    }

}
