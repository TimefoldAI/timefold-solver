package ai.timefold.solver.core.impl.heuristic.selector.entity.decorator;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractDemandEnabledSelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.ReachableEntities;
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

    private int replayedEntityOrdinal = -1;
    private BasicVariableDescriptor<Solution_>[] basicVariableDescriptors;
    private ValueRangeManager<Solution_> valueRangeManager;
    private ReachableEntities reachableEntities;
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
        this.reachableEntities = valueRangeManager.getReachableEntities(getEntityDescriptor());
        // We also initialize the reachable values as it will be used by the iterators and the process may time-consuming 
        for (var descriptor : basicVariableList) {
            valueRangeManager.getReachableValues(descriptor);
        }
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
    private Integer selectReplayedEntity() {
        var iterator = replayingEntitySelector.iterator();
        if (iterator.hasNext()) {
            var entity = iterator.next();
            if (replayedEntityOrdinal == -1 || entity != reachableEntities.getReachableEntity(replayedEntityOrdinal)) {
                this.replayedEntityOrdinal = reachableEntities.getReachableEntityOrdinal(entity);
            }
        }
        return replayedEntityOrdinal;
    }

    @Override
    public Iterator<Object> endingIterator() {
        return new OriginalFilteringValueRangeIterator<>(this::selectReplayedEntity, 0, basicVariableDescriptors,
                reachableEntities, valueRangeManager);
    }

    @Override
    public Iterator<Object> iterator() {
        if (randomSelection) {
            if (!childEntitySelector.isNeverEnding()) {
                throw new IllegalArgumentException(
                        "Impossible state: childEntitySelector must provide a never ending approach.");
            }
            return new RandomFilteringValueRangeIterator<>(this::selectReplayedEntity, basicVariableDescriptors,
                    reachableEntities, valueRangeManager, workingRandom);
        } else {
            return new OriginalFilteringValueRangeIterator<>(this::selectReplayedEntity, 0, basicVariableDescriptors,
                    reachableEntities, valueRangeManager);
        }
    }

    @Override
    public ListIterator<Object> listIterator() {
        if (!randomSelection) {
            return new OriginalFilteringValueRangeIterator<>(this::selectReplayedEntity, 0, basicVariableDescriptors,
                    reachableEntities, valueRangeManager);
        } else {
            throw new IllegalStateException("The selector (%s) does not support a ListIterator with randomSelection (%s)."
                    .formatted(this, randomSelection));
        }
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        if (!randomSelection) {
            return new OriginalFilteringValueRangeIterator<>(this::selectReplayedEntity, index, basicVariableDescriptors,
                    reachableEntities, valueRangeManager);
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
        private final Supplier<Integer> upcomingEntitySupplier;
        private final BasicVariableDescriptor<Solution_>[] basicVariableDescriptors;
        private final ValueRangeManager<Solution_> valueRangeManager;
        private final ReachableEntities<Solution_> reachableEntities;
        private boolean initialized = false;
        private int replayedEntityOrdinal = -1;

        private AbstractFilteringValueRangeIterator(Supplier<Integer> upcomingEntitySupplier,
                BasicVariableDescriptor<Solution_>[] basicVariableDescriptors, ReachableEntities<Solution_> reachableEntities,
                ValueRangeManager<Solution_> valueRangeManager) {
            this.upcomingEntitySupplier = upcomingEntitySupplier;
            this.basicVariableDescriptors = basicVariableDescriptors;
            this.valueRangeManager = valueRangeManager;
            this.reachableEntities = reachableEntities;
        }

        void initialize() {
            if (initialized) {
                return;
            }
            checkReplayedEntity();
            initialized = true;
        }

        void checkReplayedEntity() {
            var updatedReplayedEntityOrdinal = upcomingEntitySupplier.get();
            if (replayedEntityOrdinal == -1 || replayedEntityOrdinal != updatedReplayedEntityOrdinal) {
                replayedEntityOrdinal = updatedReplayedEntityOrdinal;
                processReplayedEntity(reachableEntities, replayedEntityOrdinal);
            }
        }

        abstract void processReplayedEntity(ReachableEntities<Solution_> reachableEntities, int replayedEntityOrdinal);

        Object getEntity(int ordinal) {
            return reachableEntities.getReachableEntity(ordinal);
        }

        Object currentReplayedEntity() {
            return getEntity(replayedEntityOrdinal);
        }

        /**
         * The other entity is reachable if it accepts all assigned values from the replayed entity, and vice versa.
         */
        boolean isReachable(int otherEntityOrdinal) {
            if (replayedEntityOrdinal == otherEntityOrdinal) {
                // Same entity cannot be swapped
                return false;
            }

            if (basicVariableDescriptors.length == 1) {
                return isReachable(replayedEntityOrdinal, otherEntityOrdinal, basicVariableDescriptors[0],
                        valueRangeManager.getReachableValues(basicVariableDescriptors[0]));
            } else {
                for (BasicVariableDescriptor<Solution_> basicVariableDescriptor : basicVariableDescriptors) {
                    if (!isReachable(replayedEntityOrdinal, otherEntityOrdinal, basicVariableDescriptor,
                            valueRangeManager.getReachableValues(basicVariableDescriptor))) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean isReachable(int replayedEntityOrdinal, int otherEntityOrdinal,
                BasicVariableDescriptor<Solution_> variableDescriptor, ReachableValues reachableValues) {
            if (!reachableEntities.isReachable(replayedEntityOrdinal, otherEntityOrdinal)) {
                return false;
            }
            var replayedEntity = reachableEntities.getReachableEntity(replayedEntityOrdinal);
            var otherEntity = reachableEntities.getReachableEntity(otherEntityOrdinal);
            var otherValue = variableDescriptor.getValue(otherEntity);
            var replayedValueAccepted = otherValue == null
                    || reachableValues.entityContains(replayedEntityOrdinal, reachableValues.getValueOrdinal(otherValue));
            if (!replayedValueAccepted) {
                return false;
            }
            var replayedValue = variableDescriptor.getValue(replayedEntity);
            return replayedValue == null
                    || reachableValues.entityContains(otherEntityOrdinal, reachableValues.getValueOrdinal(replayedValue));
        }
    }

    private static class OriginalFilteringValueRangeIterator<Solution_> extends AbstractFilteringValueRangeIterator<Solution_> {

        private final int index;
        private ListIterator<Integer> entityIterator;

        private OriginalFilteringValueRangeIterator(Supplier<Integer> upcomingEntitySupplier, int index,
                BasicVariableDescriptor<Solution_>[] basicVariableDescriptors, ReachableEntities<Solution_> reachableEntities,
                ValueRangeManager<Solution_> valueRangeManager) {
            super(upcomingEntitySupplier, basicVariableDescriptors, reachableEntities, valueRangeManager);
            this.index = index;
        }

        @Override
        void processReplayedEntity(ReachableEntities<Solution_> reachableEntities, int replayedEntityOrdinal) {
            entityIterator = reachableEntities.listIterator(replayedEntityOrdinal, index);
        }

        @Override
        protected Object createUpcomingSelection() {
            initialize();
            while (entityIterator.hasNext()) {
                var otherEntityOrdinal = entityIterator.next();
                if (isReachable(otherEntityOrdinal)) {
                    return getEntity(otherEntityOrdinal);
                }
            }
            return noUpcomingSelection();
        }

        @Override
        protected Object createPreviousSelection() {
            initialize();
            while (entityIterator.hasPrevious()) {
                var otherEntityOrdinal = entityIterator.previous();
                if (isReachable(otherEntityOrdinal)) {
                    return getEntity(otherEntityOrdinal);
                }
            }
            return noUpcomingSelection();
        }
    }

    private static class RandomFilteringValueRangeIterator<Solution_> extends AbstractFilteringValueRangeIterator<Solution_> {

        private final Random workingRandom;
        private Iterator<Integer> entityIterator;

        private RandomFilteringValueRangeIterator(Supplier<Integer> upcomingEntitySupplier,
                BasicVariableDescriptor<Solution_>[] basicVariableDescriptors, ReachableEntities<Solution_> reachableEntities,
                ValueRangeManager<Solution_> valueRangeManager, Random workingRandom) {
            super(upcomingEntitySupplier, basicVariableDescriptors, reachableEntities, valueRangeManager);
            this.workingRandom = workingRandom;
        }

        @Override
        void processReplayedEntity(ReachableEntities<Solution_> reachableEntities, int replayedEntityOrdinal) {
            this.entityIterator = reachableEntities.randomIterator(replayedEntityOrdinal, workingRandom);
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
            Integer nextOrdinal;
            // We expect the entity iterator to apply a never-ending random selection approach
            if (!entityIterator.hasNext()) {
                return noUpcomingSelection();
            }
            nextOrdinal = entityIterator.next();
            if (isReachable(nextOrdinal)) {
                return getEntity(nextOrdinal);
            }
            return noUpcomingSelection();
        }

        @Override
        protected Object createPreviousSelection() {
            throw new UnsupportedOperationException();
        }
    }
}
