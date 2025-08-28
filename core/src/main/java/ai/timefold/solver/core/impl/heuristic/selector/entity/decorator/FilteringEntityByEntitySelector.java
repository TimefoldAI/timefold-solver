package ai.timefold.solver.core.impl.heuristic.selector.entity.decorator;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractDemandEnabledSelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.ReachableValues;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionListIterator;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * The decorator returns a list of reachable entities for a specific entity.
 * It enables the creation of a filtering tier when using entity selectors,
 * ensuring only valid and reachable entities are returned.
 * <p>
 * e1 = entity_range[v1, v2, v3]
 * e2 = entity_range[v1, v4]
 * e3 = entity_range[v4, v5]
 * <p>
 * By default, one entity can only be reached by another if they share at least one common value within the range.
 * When values are assigned, both sides must accept these values.
 * <p>
 * e1 = [e2]
 * e2 = [e1, e3]
 * e3 = [e2]
 * <p>
 * 1. e1(null) and e2(null) - they are reachable
 * <p>
 * 2. e1(null) and e3(null) - they are not reachable
 * <p>
 * 3. e1(v2) and e2(v1) - they are not reachable
 * e1: e1 accepts v1 and e2 is reachable
 * e2: e2 does not accept v2 and e1 is not reachable
 *
 * @param <Solution_> the solution type
 */
public final class FilteringEntityByEntitySelector<Solution_> extends AbstractDemandEnabledSelector<Solution_>
        implements EntitySelector<Solution_> {

    private final EntitySelector<Solution_> replayingEntitySelector;
    private final EntitySelector<Solution_> childEntitySelector;
    private final boolean randomSelection;

    private ReplayedEntity replayedEntity;
    private BasicVariableDescriptor<Solution_>[] basicVariableDescriptors;
    private ReachableValues[] reachableValues;
    private ValueRangeManager<Solution_> valueRangeManager;
    private long entitiesSize;

    public FilteringEntityByEntitySelector(EntitySelector<Solution_> childEntitySelector,
            EntitySelector<Solution_> replayingEntitySelector, boolean randomSelection) {
        this.replayingEntitySelector = replayingEntitySelector;
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
        var basicVariableList = childEntitySelector.getEntityDescriptor().getGenuineBasicVariableDescriptorList().stream()
                .filter(v -> !v.isListVariable() && !v.canExtractValueRangeFromSolution())
                .map(v -> (BasicVariableDescriptor<Solution_>) v)
                .toList();
        if (basicVariableList.isEmpty()) {
            throw new IllegalStateException("Impossible state: no basic variable found for the entity %s."
                    .formatted(childEntitySelector.getEntityDescriptor().getEntityClass()));
        }
        this.entitiesSize = childEntitySelector.getEntityDescriptor().extractEntities(phaseScope.getWorkingSolution()).size();
        this.basicVariableDescriptors = basicVariableList.toArray(new BasicVariableDescriptor[0]);
        this.valueRangeManager = phaseScope.getScoreDirector().getValueRangeManager();
        this.reachableValues = basicVariableList.stream()
                .map(valueRangeManager::getReachableValues)
                .toArray(ReachableValues[]::new);
        this.childEntitySelector.phaseStarted(phaseScope);
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        this.childEntitySelector.stepStarted(stepScope);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        this.childEntitySelector.phaseEnded(phaseScope);
        this.replayedEntity = null;
        this.valueRangeManager = null;
        this.basicVariableDescriptors = null;
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
     * The expected replayed entity corresponds to the selected entity when the replaying selector has the next value.
     * Once it is selected, it will be reused until a new entity is replayed by the recorder selector.
     */
    private ReplayedEntity selectReplayedEntity() {
        var iterator = replayingEntitySelector.iterator();
        if (iterator.hasNext()) {
            var entity = iterator.next();
            if (replayedEntity == null || entity != replayedEntity.entity()) {
                var reachableEntityList = valueRangeManager.getReachableEntities(replayingEntitySelector.getEntityDescriptor())
                        .extractEntitiesAsList(entity);
                this.replayedEntity = new ReplayedEntity(entity, extractAssignedValues(entity), reachableEntityList);
            }
        }
        return replayedEntity;
    }

    private Object[] extractAssignedValues(Object entity) {
        var assignedValues = new Object[basicVariableDescriptors.length];
        for (var i = 0; i < basicVariableDescriptors.length; i++) {
            assignedValues[i] = basicVariableDescriptors[i].getValue(entity);
        }
        return assignedValues;
    }

    @Override
    public Iterator<Object> endingIterator() {
        return new OriginalFilteringValueRangeIterator(this::selectReplayedEntity, this::extractAssignedValues, 0,
                reachableValues);
    }

    @Override
    public Iterator<Object> iterator() {
        if (randomSelection) {
            if (!childEntitySelector.isNeverEnding()) {
                throw new IllegalArgumentException(
                        "Impossible state: childEntitySelector must provide a never ending approach.");
            }
            return new RandomFilteringValueRangeIterator(this::selectReplayedEntity, this::extractAssignedValues,
                    reachableValues);
        } else {
            return new OriginalFilteringValueRangeIterator(this::selectReplayedEntity, this::extractAssignedValues, 0,
                    reachableValues);
        }
    }

    @Override
    public ListIterator<Object> listIterator() {
        if (!randomSelection) {
            return new OriginalFilteringValueRangeIterator(this::selectReplayedEntity, this::extractAssignedValues, 0,
                    reachableValues);
        } else {
            throw new IllegalStateException("The selector (%s) does not support a ListIterator with randomSelection (%s)."
                    .formatted(this, randomSelection));
        }
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        if (!randomSelection) {
            return new OriginalFilteringValueRangeIterator(this::selectReplayedEntity, this::extractAssignedValues, index,
                    reachableValues);
        } else {
            throw new IllegalStateException("The selector (%s) does not support a ListIterator with randomSelection (%s)."
                    .formatted(this, randomSelection));
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FilteringEntityByEntitySelector<?> that
                && Objects.equals(childEntitySelector, that.childEntitySelector)
                && Objects.equals(replayingEntitySelector, that.replayingEntitySelector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(childEntitySelector, replayingEntitySelector);
    }

    private record ReplayedEntity(Object entity, Object[] assignedValues, List<Object> reachableEntities) {
    }

    private abstract static class AbstractFilteringValueRangeIterator<Type extends Iterator<Object>>
            extends UpcomingSelectionListIterator<Object> {
        private final Supplier<ReplayedEntity> upcomingEntitySupplier;
        private final Function<Object, Object[]> extractAssignedValuesFunction;
        private final ReachableValues[] reachableValues;
        private Type entityIterator;
        private boolean initialized = false;
        ReplayedEntity replayedEntity;

        private AbstractFilteringValueRangeIterator(Supplier<ReplayedEntity> upcomingEntitySupplier,
                Function<Object, Object[]> extractAssignedValuesFunction, ReachableValues[] reachableValues) {
            this.upcomingEntitySupplier = upcomingEntitySupplier;
            this.extractAssignedValuesFunction = extractAssignedValuesFunction;
            this.reachableValues = reachableValues;
        }

        void checkReplayedEntity() {
            var updatedReplayedEntity = upcomingEntitySupplier.get();
            if (replayedEntity == null || replayedEntity.entity() != updatedReplayedEntity.entity()) {
                replayedEntity = updatedReplayedEntity;
            }
            entityIterator = fetchEntityIterator(replayedEntity);
        }

        abstract Type fetchEntityIterator(ReplayedEntity entity);

        void initialize() {
            if (initialized) {
                return;
            }
            var currentUpcomingEntity = upcomingEntitySupplier.get();
            if (currentUpcomingEntity == null) {
                this.replayedEntity = null;
            } else {
                replayedEntity = currentUpcomingEntity;
            }
            entityIterator = fetchEntityIterator(replayedEntity);
            initialized = true;
        }

        /**
         * The other entity is reachable if it accepts all assigned values from the replayed entity, and vice versa.
         */
        boolean isReachable(Object otherEntity) {
            if (replayedEntity.entity() == otherEntity) {
                // Same entity cannot be swapped
                return false;
            }
            var otherValueAssignedValues = extractAssignedValuesFunction.apply(otherEntity);
            if (reachableValues.length == 1) {
                return isReachable(replayedEntity.entity(), replayedEntity.assignedValues()[0], otherEntity,
                        otherValueAssignedValues[0], reachableValues[0]);
            } else {
                for (var i = 0; i < replayedEntity.assignedValues().length; i++) {
                    var replayedValue = replayedEntity.assignedValues()[i];
                    var otherValue = otherValueAssignedValues[i];
                    var reachableValues = this.reachableValues[i];
                    if (!isReachable(replayedEntity.entity(), replayedValue, otherEntity, otherValue, reachableValues)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean isReachable(Object replayedEntity, Object replayedValue, Object otherEntity, Object otherValue,
                ReachableValues reachableValues) {
            var replayedValueAccepted = otherValue == null || reachableValues.isEntityReachable(otherValue, replayedEntity);
            var otherValueAccepted = replayedValue == null || reachableValues.isEntityReachable(replayedValue, otherEntity);
            return replayedValueAccepted && otherValueAccepted;
        }

        abstract Object createPreviousSelection(Type entityIterator);

        /**
         * @param counter current number of iterations.
         * @return true if the iterator should stop; otherwise, it should continue.
         */
        abstract boolean stopEarlier(int counter);

        @Override
        protected Object createUpcomingSelection() {
            initialize();
            if (!entityIterator.hasNext()) {
                return noUpcomingSelection();
            }
            int counter = 0;
            do {
                // For random selection, the entity iterator is expected to return a random sequence of values
                var entity = entityIterator.next();
                if (isReachable(entity)) {
                    return entity;
                }
            } while (entityIterator.hasNext() && !stopEarlier(++counter));
            return noUpcomingSelection();
        }

        @Override
        protected Object createPreviousSelection() {
            return createPreviousSelection(entityIterator);
        }
    }

    private static class OriginalFilteringValueRangeIterator extends AbstractFilteringValueRangeIterator<ListIterator<Object>> {

        private final int index;

        private OriginalFilteringValueRangeIterator(Supplier<ReplayedEntity> upcomingEntitySupplier,
                Function<Object, Object[]> extractAssignedValuesFunction, int index, ReachableValues[] reachableValueList) {
            super(upcomingEntitySupplier, extractAssignedValuesFunction, reachableValueList);
            this.index = index;
        }

        @Override
        protected Object createPreviousSelection(ListIterator<Object> entityIterator) {
            initialize();
            if (!entityIterator.hasPrevious()) {
                return noUpcomingSelection();
            }
            int counter = 0;
            do {
                var entity = entityIterator.previous();
                if (isReachable(entity)) {
                    return entity;
                }
            } while (entityIterator.hasPrevious() && !stopEarlier(++counter));
            return noUpcomingSelection();
        }

        @Override
        boolean stopEarlier(int currentCount) {
            return false;
        }

        @Override
        ListIterator<Object> fetchEntityIterator(ReplayedEntity entity) {
            if (entity == null) {
                return Collections.emptyListIterator();
            }
            return replayedEntity.reachableEntities().listIterator(index);
        }
    }

    private static class RandomFilteringValueRangeIterator extends AbstractFilteringValueRangeIterator<Iterator<Object>> {

        private int bailoutSize = 1;

        private RandomFilteringValueRangeIterator(Supplier<ReplayedEntity> upcomingEntitySupplier,
                Function<Object, Object[]> extractAssignedValuesFunction, ReachableValues[] reachableValues) {
            super(upcomingEntitySupplier, extractAssignedValuesFunction, reachableValues);
        }

        @Override
        public boolean hasNext() {
            checkReplayedEntity();
            var hasNext = super.hasNext();
            if (!hasNext && replayedEntity.reachableEntities().size() > 1) {
                // If a valid move is not found with the given bailout size,
                // we can still use the iterator as long as the entityIterator has not been exhausted
                this.upcomingCreated = true;
                this.hasUpcomingSelection = true;
                // We assigned the same entity from the left side, which will result in a non-doable move
                this.upcomingSelection = replayedEntity.entity();
                return true;
            }
            return hasNext;
        }

        @Override
        Object createPreviousSelection(Iterator<Object> entityIterator) {
            throw new UnsupportedOperationException();
        }

        @Override
        boolean stopEarlier(int currentCount) {

            return currentCount > bailoutSize;
        }

        @Override
        Iterator<Object> fetchEntityIterator(ReplayedEntity entity) {
            if (entity == null) {
                bailoutSize = 0;
                return Collections.emptyIterator();
            }
            // The maximum number of attempts is equal to 1% of the number of available entities.
            // We won't spend too much time trying to generate a single move for the current selection.
            // If we are unable to generate, the move iterator can still be used in later iterations.
            this.bailoutSize = (int) Math.max(1, replayedEntity.reachableEntities().size() * 0.01);
            return replayedEntity.reachableEntities().iterator();
        }
    }
}
