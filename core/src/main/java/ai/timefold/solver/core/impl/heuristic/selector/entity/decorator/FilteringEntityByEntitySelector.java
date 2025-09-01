package ai.timefold.solver.core.impl.heuristic.selector.entity.decorator;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractDemandEnabledSelector;
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
 * By default, one entity can be reached by another if assigned values are null.
 * When values are assigned, both sides must accept these values.
 * <p>
 * e1 = [e2]
 * e2 = [e1, e3]
 * e3 = [e2]
 * <p>
 * 1. e1(null) and e2(null) - they are reachable
 * <p>
 * 2. e1(v2) and e2(v1) - they are not reachable
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

    private Object replayedEntity;
    private BasicVariableDescriptor<Solution_>[] basicVariableDescriptors;
    private ValueRangeManager<Solution_> valueRangeManager;
    private int entitiesSize;

    public FilteringEntityByEntitySelector(EntitySelector<Solution_> childEntitySelector,
            EntitySelector<Solution_> replayingEntitySelector, boolean randomSelection) {
        this.childEntitySelector = childEntitySelector;
        this.replayingEntitySelector = replayingEntitySelector;
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
    private Object selectReplayedEntity() {
        var iterator = replayingEntitySelector.iterator();
        if (iterator.hasNext()) {
            var entity = iterator.next();
            if (replayedEntity == null || entity != replayedEntity) {
                this.replayedEntity = entity;
            }
        }
        return replayedEntity;
    }

    @Override
    public Iterator<Object> endingIterator() {
        return new OriginalFilteringValueRangeIterator<>(this::selectReplayedEntity, childEntitySelector.listIterator(),
                basicVariableDescriptors, valueRangeManager);
    }

    @Override
    public Iterator<Object> iterator() {
        if (randomSelection) {
            if (!childEntitySelector.isNeverEnding()) {
                throw new IllegalArgumentException(
                        "Impossible state: childEntitySelector must provide a never ending approach.");
            }
            return new RandomFilteringValueRangeIterator<>(this::selectReplayedEntity, childEntitySelector.iterator(),
                    basicVariableDescriptors, valueRangeManager);
        } else {
            return new OriginalFilteringValueRangeIterator<>(this::selectReplayedEntity, childEntitySelector.listIterator(),
                    basicVariableDescriptors, valueRangeManager);
        }
    }

    @Override
    public ListIterator<Object> listIterator() {
        if (!randomSelection) {
            return new OriginalFilteringValueRangeIterator<>(this::selectReplayedEntity, childEntitySelector.listIterator(),
                    basicVariableDescriptors, valueRangeManager);
        } else {
            throw new IllegalStateException("The selector (%s) does not support a ListIterator with randomSelection (%s)."
                    .formatted(this, randomSelection));
        }
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        if (!randomSelection) {
            return new OriginalFilteringValueRangeIterator<>(this::selectReplayedEntity,
                    childEntitySelector.listIterator(index), basicVariableDescriptors, valueRangeManager);
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

    private abstract static class AbstractFilteringValueRangeIterator<Solution_> extends UpcomingSelectionListIterator<Object> {
        private final Supplier<Object> upcomingEntitySupplier;
        private final BasicVariableDescriptor<Solution_>[] basicVariableDescriptors;
        private final ValueRangeManager<Solution_> valueRangeManager;
        private boolean initialized = false;
        private Object replayedEntity;

        private AbstractFilteringValueRangeIterator(Supplier<Object> upcomingEntitySupplier,
                BasicVariableDescriptor<Solution_>[] basicVariableDescriptors, ValueRangeManager<Solution_> valueRangeManager) {
            this.upcomingEntitySupplier = upcomingEntitySupplier;
            this.basicVariableDescriptors = basicVariableDescriptors;
            this.valueRangeManager = valueRangeManager;
        }

        void initialize() {
            if (initialized) {
                return;
            }
            checkReplayedEntity();
            initialized = true;
        }

        void checkReplayedEntity() {
            var updatedReplayedEntity = upcomingEntitySupplier.get();
            if (replayedEntity == null || replayedEntity != updatedReplayedEntity) {
                replayedEntity = updatedReplayedEntity;
            }
        }

        Object currentReplayedEntity() {
            return replayedEntity;
        }

        /**
         * The other entity is reachable if it accepts all assigned values from the replayed entity, and vice versa.
         */
        boolean isReachable(Object otherEntity) {
            if (replayedEntity == otherEntity) {
                // Same entity cannot be swapped
                return false;
            }

            if (basicVariableDescriptors.length == 1) {
                return isReachable(replayedEntity, otherEntity, basicVariableDescriptors[0], valueRangeManager);
            } else {
                for (BasicVariableDescriptor<Solution_> basicVariableDescriptor : basicVariableDescriptors) {
                    if (!isReachable(replayedEntity, otherEntity, basicVariableDescriptor, valueRangeManager)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean isReachable(Object replayedEntity, Object otherEntity,
                BasicVariableDescriptor<Solution_> variableDescriptor, ValueRangeManager<Solution_> valueRangeManager) {
            var otherValue = variableDescriptor.getValue(otherEntity);
            var replayedValueAccepted = otherValue == null || valueRangeManager
                    .getFromEntity(variableDescriptor.getValueRangeDescriptor(), replayedEntity).contains(otherValue);
            if (!replayedValueAccepted) {
                return false;
            }
            var replayedValue = variableDescriptor.getValue(replayedEntity);
            return replayedValue == null || valueRangeManager
                    .getFromEntity(variableDescriptor.getValueRangeDescriptor(), otherEntity).contains(replayedValue);
        }
    }

    private static class OriginalFilteringValueRangeIterator<Solution_> extends AbstractFilteringValueRangeIterator<Solution_> {

        private final ListIterator<Object> entityIterator;

        private OriginalFilteringValueRangeIterator(Supplier<Object> upcomingEntitySupplier,
                ListIterator<Object> entityIterator, BasicVariableDescriptor<Solution_>[] basicVariableDescriptors,
                ValueRangeManager<Solution_> valueRangeManager) {
            super(upcomingEntitySupplier, basicVariableDescriptors, valueRangeManager);
            this.entityIterator = entityIterator;
        }

        @Override
        protected Object createUpcomingSelection() {
            initialize();
            while (entityIterator.hasNext()) {
                var entity = entityIterator.next();
                if (isReachable(entity)) {
                    return entity;
                }
            }
            return noUpcomingSelection();
        }

        @Override
        protected Object createPreviousSelection() {
            initialize();
            while (entityIterator.hasPrevious()) {
                var entity = entityIterator.previous();
                if (isReachable(entity)) {
                    return entity;
                }
            }
            return noUpcomingSelection();
        }
    }

    private static class RandomFilteringValueRangeIterator<Solution_> extends AbstractFilteringValueRangeIterator<Solution_> {

        private final Iterator<Object> entityIterator;

        private RandomFilteringValueRangeIterator(Supplier<Object> upcomingEntitySupplier, Iterator<Object> entityIterator,
                BasicVariableDescriptor<Solution_>[] basicVariableDescriptors, ValueRangeManager<Solution_> valueRangeManager) {
            super(upcomingEntitySupplier, basicVariableDescriptors, valueRangeManager);
            this.entityIterator = entityIterator;
        }

        @Override
        public boolean hasNext() {
            checkReplayedEntity();
            var hasNext = super.hasNext();
            if (!hasNext && entityIterator.hasNext()) {
                // If a valid move is not found with the given bailout size,
                // we can still use the iterator as long as the entity iterator is not exhausted
                this.upcomingCreated = true;
                this.hasUpcomingSelection = true;
                // We assigned the same entity from the left side, which will result in a non-doable move
                this.upcomingSelection = currentReplayedEntity();
                return true;
            }
            return hasNext;
        }

        @Override
        protected Object createUpcomingSelection() {
            initialize();
            Object next;
            // We expect the entity iterator to apply a never-ending random selection approach
            next = entityIterator.next();
            if (isReachable(next)) {
                return next;
            }
            return noUpcomingSelection();
        }

        @Override
        protected Object createPreviousSelection() {
            throw new UnsupportedOperationException();
        }
    }
}
