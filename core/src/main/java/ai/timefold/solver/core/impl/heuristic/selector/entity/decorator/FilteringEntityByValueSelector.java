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
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionListIterator;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * The decorator returns a list of reachable entities for a specific value.
 * It enables the creation of a filtering tier when using entity-provided value ranges,
 * ensuring only valid and reachable entities are returned.
 * An entity is considered reachable to a value if its value range includes that value.
 * <p>
 * The decorator can only be applied to list variables.
 * <p>
 * 
 * <pre>
 * e1 = entity_range[v1, v2, v3]
 * e2 = entity_range[v1, v4]
 *
 * v1 = [e1, e2]
 *
 * v2 = [e1]
 *
 * v3 = [e1]
 *
 * v4 = [e2]
 * </pre>
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
    private final boolean isExhaustiveSearch;

    private Object replayedValue;
    private Object replayedEntity;
    private ReachableValues<Object, Object> reachableValues;
    private long entitiesSize;

    public FilteringEntityByValueSelector(EntitySelector<Solution_> childEntitySelector,
            IterableValueSelector<Solution_> replayingValueSelector, boolean randomSelection, boolean isExhaustiveSearch) {
        this.replayingValueSelector = replayingValueSelector;
        this.childEntitySelector = childEntitySelector;
        this.randomSelection = randomSelection;
        this.isExhaustiveSearch = isExhaustiveSearch;
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
        this.childEntitySelector.phaseEnded(phaseScope);
        this.replayedValue = null;
        this.reachableValues = null;
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        this.childEntitySelector.stepStarted(stepScope);
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

    /**
     * The exhaustive search uses a replaying entity selector to guarantee which search node will be explored.
     * Thus, the {@code childEntitySelector} will be a replaying selector when {@code isExhaustiveSearch} is set to
     * {@code true}.
     */
    private Object selectReplayedEntity() {
        if (!isExhaustiveSearch) {
            throw new IllegalStateException("Impossible state: exhaustiveSearch is set to false");
        }
        var iterator = childEntitySelector.iterator();
        if (iterator.hasNext()) {
            replayedEntity = iterator.next();
        }
        return replayedEntity;
    }

    @Override
    public Iterator<Object> endingIterator() {
        return new OriginalFilteringValueRangeIterator<>(this::selectReplayedValue, reachableValues);
    }

    @Override
    public Iterator<Object> iterator() {
        if (randomSelection) {
            if (isExhaustiveSearch) {
                throw new IllegalStateException("The random iterator is not supported for the exhaustive search.");
            }
            return new RandomFilteringValueRangeIterator<>(this::selectReplayedValue, reachableValues, workingRandom);
        } else {
            if (isExhaustiveSearch) {
                return new ExhaustiveOriginalFilteringValueRangeIterator<>(this::selectReplayedEntity,
                        this::selectReplayedValue, reachableValues);
            } else {
                return new OriginalFilteringValueRangeIterator<>(this::selectReplayedValue, reachableValues);
            }
        }
    }

    @Override
    public ListIterator<Object> listIterator() {
        if (isExhaustiveSearch) {
            throw new IllegalStateException("The list iterator is not supported for the exhaustive search.");
        }
        return new OriginalFilteringValueRangeListIterator<>(this::selectReplayedValue, childEntitySelector.listIterator(),
                reachableValues);
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        if (isExhaustiveSearch) {
            throw new IllegalStateException("The list iterator is not supported for the exhaustive search.");
        }
        return new OriginalFilteringValueRangeListIterator<>(this::selectReplayedValue, childEntitySelector.listIterator(index),
                reachableValues);
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

    private static class ExhaustiveOriginalFilteringValueRangeIterator<Entity_, Value_>
            extends OriginalFilteringValueRangeIterator<Entity_, Value_> {

        private final Supplier<Entity_> replayingEntitySupplier;

        private ExhaustiveOriginalFilteringValueRangeIterator(Supplier<Entity_> replayingEntitySupplier,
                Supplier<Value_> upcomingValueSupplier,
                ReachableValues<Entity_, Value_> reachableValues) {
            super(upcomingValueSupplier, reachableValues);
            this.replayingEntitySupplier = replayingEntitySupplier;
        }

        @Override
        protected void initialize() {
            if (entityIterator != null) {
                return;
            }
            var currentUpcomingValue = upcomingValueSupplier.get();
            if (currentUpcomingValue == null) {
                entityIterator = Collections.emptyIterator();
            } else {
                var currentReplayedEntity = replayingEntitySupplier.get();
                if (currentReplayedEntity == null) {
                    entityIterator = Collections.emptyIterator();
                } else {
                    if (reachableValues.isEntityReachable(currentUpcomingValue, currentReplayedEntity)) {
                        // We will only return the replayed entity if it is included in the set of reachable entities
                        entityIterator = Collections.singletonList(currentReplayedEntity).iterator();
                    } else {
                        entityIterator = Collections.emptyIterator();
                    }
                }
            }
        }
    }

    private static class OriginalFilteringValueRangeIterator<Entity_, Value_> extends UpcomingSelectionIterator<Entity_> {

        protected final Supplier<Value_> upcomingValueSupplier;
        protected final ReachableValues<Entity_, Value_> reachableValues;
        protected Iterator<Entity_> entityIterator;

        private OriginalFilteringValueRangeIterator(Supplier<Value_> upcomingValueSupplier,
                ReachableValues<Entity_, Value_> reachableValues) {
            this.reachableValues = Objects.requireNonNull(reachableValues);
            this.upcomingValueSupplier = Objects.requireNonNull(upcomingValueSupplier);
        }

        protected void initialize() {
            if (entityIterator != null) {
                return;
            }
            var currentUpcomingValue = upcomingValueSupplier.get();
            if (currentUpcomingValue == null) {
                entityIterator = Collections.emptyIterator();
            } else {
                var allValues = reachableValues.extractEntitiesAsList(Objects.requireNonNull(currentUpcomingValue));
                this.entityIterator = Objects.requireNonNull(allValues).iterator();
            }
        }

        @Override
        protected Entity_ createUpcomingSelection() {
            initialize();
            if (!entityIterator.hasNext()) {
                return noUpcomingSelection();
            }
            return entityIterator.next();
        }
    }

    private static class OriginalFilteringValueRangeListIterator<Entity_, Value_>
            extends UpcomingSelectionListIterator<Entity_> {

        private final Supplier<Value_> upcomingValueSupplier;
        private final ListIterator<Entity_> entityIterator;
        private final ReachableValues<Entity_, Value_> reachableValues;
        private Value_ replayedValue;

        private OriginalFilteringValueRangeListIterator(Supplier<Value_> upcomingValueSupplier,
                ListIterator<Entity_> entityIterator, ReachableValues<Entity_, Value_> reachableValues) {
            this.upcomingValueSupplier = upcomingValueSupplier;
            this.entityIterator = entityIterator;
            this.reachableValues = reachableValues;
        }

        void checkReplayedValue() {
            var newReplayedValue = upcomingValueSupplier.get();
            if (newReplayedValue != replayedValue) {
                replayedValue = newReplayedValue;
            }
        }

        @Override
        public boolean hasNext() {
            checkReplayedValue();
            return super.hasNext();
        }

        @Override
        public boolean hasPrevious() {
            checkReplayedValue();
            return super.hasPrevious();
        }

        @Override
        protected Entity_ createUpcomingSelection() {
            if (!entityIterator.hasNext()) {
                return noUpcomingSelection();
            }
            while (entityIterator.hasNext()) {
                var otherEntity = entityIterator.next();
                if (reachableValues.isEntityReachable(replayedValue, otherEntity)) {
                    return otherEntity;
                }
            }
            return noUpcomingSelection();
        }

        @Override
        protected Entity_ createPreviousSelection() {
            if (!entityIterator.hasPrevious()) {
                return noUpcomingSelection();
            }
            while (entityIterator.hasPrevious()) {
                var otherEntity = entityIterator.previous();
                if (reachableValues.isEntityReachable(replayedValue, otherEntity)) {
                    return otherEntity;
                }
            }
            return noUpcomingSelection();
        }
    }

    private static class RandomFilteringValueRangeIterator<Entity_, Value_> implements Iterator<Entity_> {

        private final Supplier<Value_> upcomingValueSupplier;
        private final ReachableValues<Entity_, Value_> reachableValues;
        private final Random workingRandom;
        private Value_ currentUpcomingValue;
        private List<Entity_> entityList;

        private RandomFilteringValueRangeIterator(Supplier<Value_> upcomingValueSupplier,
                ReachableValues<Entity_, Value_> reachableValues, Random workingRandom) {
            this.upcomingValueSupplier = upcomingValueSupplier;
            this.reachableValues = Objects.requireNonNull(reachableValues);
            this.workingRandom = workingRandom;
        }

        private void checkReplayedValue() {
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
            checkReplayedValue();
            return entityList != null && !entityList.isEmpty();
        }

        @Override
        public Entity_ next() {
            if (entityList.isEmpty()) {
                throw new NoSuchElementException();
            }
            var index = workingRandom.nextInt(entityList.size());
            return entityList.get(index);
        }
    }

}
