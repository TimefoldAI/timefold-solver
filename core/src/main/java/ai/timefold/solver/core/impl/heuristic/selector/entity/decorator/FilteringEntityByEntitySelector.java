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
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * The decorator returns a list of reachable entities for a specific entity.
 * It enables the creation of a filtering tier when using entity value selectors,
 * ensuring only valid and reachable entities are returned.
 * <p>
 * e1 = entity_range[v1, v2, v3]
 * e2 = entity_range[v1, v4]
 * <p>
 * Solution: e1(v2) and e2(v1)
 * e1 = [e2] -> e1 accepts v1 and e2 is reachable
 * e2 = [] -> e2 does not accept v2 and e1 is not reachable
 *
 * @param <Solution_> the solution type
 */
public final class FilteringEntityByEntitySelector<Solution_> extends AbstractDemandEnabledSelector<Solution_>
        implements EntitySelector<Solution_> {

    private final EntitySelector<Solution_> replayingEntitySelector;
    private final EntitySelector<Solution_> childEntitySelector;
    private final boolean randomSelection;

    private ReplayedEntity replayedEntity;
    private List<BasicVariableDescriptor<Solution_>> basicVariableDescriptorList;
    private List<ReachableValues> reachableValueList;
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
        this.basicVariableDescriptorList = basicVariableList;
        var valueRangeManager = phaseScope.getScoreDirector().getValueRangeManager();
        this.reachableValueList = basicVariableList.stream()
                .map(valueRangeManager::getReachableValues)
                .toList();
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
        this.basicVariableDescriptorList = null;
        this.reachableValueList = null;
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
            this.replayedEntity = new ReplayedEntity(entity, extractAssignedValues(entity));
        }
        return replayedEntity;
    }

    private List<Object> extractAssignedValues(Object entity) {
        return basicVariableDescriptorList.stream()
                .map(v -> v.getValue(entity))
                .toList();
    }

    @Override
    public Iterator<Object> endingIterator() {
        return new OriginalFilteringValueRangeIterator(this::selectReplayedEntity, this::extractAssignedValues,
                childEntitySelector.listIterator(), reachableValueList);
    }

    @Override
    public Iterator<Object> iterator() {
        if (randomSelection) {
            if (!childEntitySelector.isNeverEnding()) {
                throw new IllegalArgumentException(
                        "Impossible state: childEntitySelector must provide a never ending approach.");
            }
            return new RandomFilteringValueRangeIterator(this::selectReplayedEntity, this::extractAssignedValues,
                    childEntitySelector.iterator(), reachableValueList, (int) (entitiesSize * 10));
        } else {
            return new OriginalFilteringValueRangeIterator(this::selectReplayedEntity, this::extractAssignedValues,
                    childEntitySelector.listIterator(), reachableValueList);
        }
    }

    @Override
    public ListIterator<Object> listIterator() {
        if (!randomSelection) {
            return new OriginalFilteringValueRangeIterator(this::selectReplayedEntity, this::extractAssignedValues,
                    childEntitySelector.listIterator(), reachableValueList);
        } else {
            throw new IllegalStateException("The selector (%s) does not support a ListIterator with randomSelection (%s)."
                    .formatted(this, randomSelection));
        }
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        if (!randomSelection) {
            return new OriginalFilteringValueRangeIterator(this::selectReplayedEntity, this::extractAssignedValues,
                    childEntitySelector.listIterator(index), reachableValueList);
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

    private record ReplayedEntity(Object entity, List<Object> assignedValues) {
    }

    private abstract static class AbstractFilteringValueRangeIterator<Type extends Iterator<Object>>
            extends UpcomingSelectionListIterator<Object> {
        private final Supplier<ReplayedEntity> upcomingEntitySupplier;
        private final Function<Object, List<Object>> extractAssignedValuesFunction;
        private final List<ReachableValues> reachableValueList;
        Type entityIterator;
        private boolean initialized = false;
        private ReplayedEntity replayedEntity;

        private AbstractFilteringValueRangeIterator(Supplier<ReplayedEntity> upcomingEntitySupplier,
                Function<Object, List<Object>> extractAssignedValuesFunction, Type entityIterator,
                List<ReachableValues> reachableValueList) {
            this.upcomingEntitySupplier = upcomingEntitySupplier;
            this.extractAssignedValuesFunction = extractAssignedValuesFunction;
            this.entityIterator = entityIterator;
            this.reachableValueList = reachableValueList;
        }

        void initialize() {
            if (initialized) {
                return;
            }
            var currentUpcomingEntity = upcomingEntitySupplier.get();
            if (currentUpcomingEntity == null) {
                this.replayedEntity = null;
                entityIterator = (Type) Collections.emptyListIterator();
            } else {
                replayedEntity = currentUpcomingEntity;
            }
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
            if (reachableValueList.size() == 1) {
                return isReachable(replayedEntity.entity(), replayedEntity.assignedValues().get(0), otherEntity,
                        otherValueAssignedValues.get(0), reachableValueList.get(0));
            } else {
                for (var i = 0; i < replayedEntity.assignedValues().size(); i++) {
                    var replayedValue = replayedEntity.assignedValues().get(i);
                    var otherValue = otherValueAssignedValues.get(i);
                    var reachableValues = reachableValueList.get(i);
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
    }

    private static class OriginalFilteringValueRangeIterator extends AbstractFilteringValueRangeIterator<ListIterator<Object>> {

        private OriginalFilteringValueRangeIterator(Supplier<ReplayedEntity> upcomingEntitySupplier,
                Function<Object, List<Object>> extractAssignedValuesFunction, ListIterator<Object> entityIterator,
                List<ReachableValues> reachableValueList) {
            super(upcomingEntitySupplier, extractAssignedValuesFunction, entityIterator, reachableValueList);
        }

        @Override
        protected Object createPreviousSelection() {
            initialize();
            if (!entityIterator.hasPrevious()) {
                return noUpcomingSelection();
            }
            int counter = 0;
            do {
                // For random selection, the entity iterator is expected to return a random sequence of values
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
    }

    private static class RandomFilteringValueRangeIterator extends AbstractFilteringValueRangeIterator<Iterator<Object>> {
        private final int maxBailoutSize;

        private RandomFilteringValueRangeIterator(Supplier<ReplayedEntity> upcomingEntitySupplier,
                Function<Object, List<Object>> extractAssignedValuesFunction, Iterator<Object> entityIterator,
                List<ReachableValues> reachableValueList, int maxBailoutSize) {
            super(upcomingEntitySupplier, extractAssignedValuesFunction, entityIterator, reachableValueList);
            this.maxBailoutSize = maxBailoutSize;
        }

        @Override
        protected Object createPreviousSelection() {
            throw new UnsupportedOperationException();
        }

        @Override
        boolean stopEarlier(int currentCount) {
            return currentCount < maxBailoutSize;
        }
    }
}
