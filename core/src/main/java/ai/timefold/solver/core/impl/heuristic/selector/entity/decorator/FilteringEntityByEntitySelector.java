package ai.timefold.solver.core.impl.heuristic.selector.entity.decorator;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Random;
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
    private ReachableValues[] reachableValues;
    private ValueRangeManager<Solution_> valueRangeManager;
    private List<Object> allEntities;

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
        this.allEntities = childEntitySelector.getEntityDescriptor().extractEntities(phaseScope.getWorkingSolution());
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
        return allEntities.size();
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
        return new OriginalFilteringValueRangeIterator<>(this::selectReplayedEntity, allEntities, basicVariableDescriptors, 0,
                reachableValues);
    }

    @Override
    public Iterator<Object> iterator() {
        if (randomSelection) {
            if (!childEntitySelector.isNeverEnding()) {
                throw new IllegalArgumentException(
                        "Impossible state: childEntitySelector must provide a never ending approach.");
            }
            return new RandomFilteringValueRangeIterator<>(this::selectReplayedEntity, allEntities, basicVariableDescriptors,
                    reachableValues, workingRandom);
        } else {
            return new OriginalFilteringValueRangeIterator<>(this::selectReplayedEntity, allEntities, basicVariableDescriptors,
                    0, reachableValues);
        }
    }

    @Override
    public ListIterator<Object> listIterator() {
        if (!randomSelection) {
            return new OriginalFilteringValueRangeIterator<>(this::selectReplayedEntity, allEntities, basicVariableDescriptors,
                    0, reachableValues);
        } else {
            throw new IllegalStateException("The selector (%s) does not support a ListIterator with randomSelection (%s)."
                    .formatted(this, randomSelection));
        }
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        if (!randomSelection) {
            return new OriginalFilteringValueRangeIterator<>(this::selectReplayedEntity, allEntities, basicVariableDescriptors,
                    index, reachableValues);
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
        private final ReachableValues[] reachableValues;
        private boolean initialized = false;
        private Object replayedEntity;

        private AbstractFilteringValueRangeIterator(Supplier<Object> upcomingEntitySupplier,
                BasicVariableDescriptor<Solution_>[] basicVariableDescriptors, ReachableValues[] reachableValues) {
            this.upcomingEntitySupplier = upcomingEntitySupplier;
            this.basicVariableDescriptors = basicVariableDescriptors;
            this.reachableValues = reachableValues;
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
                processReplayedEntityChange(replayedEntity);
            }
        }

        abstract void processReplayedEntityChange(Object replayedEntity);

        /**
         * The other entity is reachable if it accepts all assigned values from the replayed entity, and vice versa.
         */
        boolean isReachable(Object otherEntity) {
            if (replayedEntity == otherEntity) {
                // Same entity cannot be swapped
                return false;
            }

            if (reachableValues.length == 1) {
                return isReachable(replayedEntity, basicVariableDescriptors[0].getValue(replayedEntity), otherEntity,
                        basicVariableDescriptors[0].getValue(otherEntity), reachableValues[0]);
            } else {
                for (var i = 0; i < basicVariableDescriptors.length; i++) {
                    var basicVariableDescriptor = basicVariableDescriptors[i];
                    var replayedValue = basicVariableDescriptor.getValue(replayedEntity);
                    var otherValue = basicVariableDescriptor.getValue(otherEntity);
                    if (!isReachable(replayedEntity, replayedValue, otherEntity, otherValue, this.reachableValues[i])) {
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
    }

    private static class OriginalFilteringValueRangeIterator<Solution_> extends AbstractFilteringValueRangeIterator<Solution_> {

        private final int startIndex;
        private int currentIndex;
        private List<Object> reachableEntityList = null;

        private OriginalFilteringValueRangeIterator(Supplier<Object> upcomingEntitySupplier, List<Object> allEntities,
                BasicVariableDescriptor<Solution_>[] basicVariableDescriptors, int startIndex,
                ReachableValues[] reachableValueList) {
            super(upcomingEntitySupplier, basicVariableDescriptors, reachableValueList);
            this.startIndex = startIndex;
            this.currentIndex = startIndex;
            this.reachableEntityList = Objects.requireNonNull(allEntities).subList(startIndex, allEntities.size());
        }

        @Override
        void processReplayedEntityChange(Object replayedEntity) {
            this.currentIndex = startIndex;
        }

        @Override
        protected Object createUpcomingSelection() {
            initialize();
            if (currentIndex >= reachableEntityList.size()) {
                return noUpcomingSelection();
            }
            do {
                var entity = reachableEntityList.get(currentIndex++);
                if (isReachable(entity)) {
                    return entity;
                }
            } while (currentIndex < reachableEntityList.size());
            return noUpcomingSelection();
        }

        @Override
        protected Object createPreviousSelection() {
            initialize();
            if (currentIndex <= 0) {
                return noUpcomingSelection();
            }
            do {
                var entity = reachableEntityList.get(currentIndex--);
                if (isReachable(entity)) {
                    return entity;
                }
            } while (currentIndex > 0);
            return noUpcomingSelection();
        }
    }

    private static class RandomFilteringValueRangeIterator<Solution_> extends AbstractFilteringValueRangeIterator<Solution_> {

        private final Random workingRandom;
        private final List<Object> reachableEntityList;
        private int maxBailoutSize = 1;
        private Object replayedEntity;

        private RandomFilteringValueRangeIterator(Supplier<Object> upcomingEntitySupplier, List<Object> allEntities,
                BasicVariableDescriptor<Solution_>[] basicVariableDescriptors, ReachableValues[] reachableValues,
                Random workingRandom) {
            super(upcomingEntitySupplier, basicVariableDescriptors, reachableValues);
            this.reachableEntityList = Objects.requireNonNull(allEntities);
            this.workingRandom = workingRandom;
        }

        @Override
        void processReplayedEntityChange(Object replayedEntity) {
            this.replayedEntity = replayedEntity;
            // The maximum number of attempts is equal to 20% of the number of available entities.
            // We won't spend too much time trying to generate a single move for the current selection.
            // If we are unable to generate, the move iterator can still be used in later iterations.
            this.maxBailoutSize = (int) Math.max(1, reachableEntityList.size() * 0.2);
        }

        @Override
        public boolean hasNext() {
            checkReplayedEntity();
            var hasNext = super.hasNext();
            if (!hasNext && !reachableEntityList.isEmpty()) {
                // If a valid move is not found with the given bailout size,
                // we can still use the iterator as long as the entity list is not empty
                this.upcomingCreated = true;
                this.hasUpcomingSelection = true;
                // We assigned the same entity from the left side, which will result in a non-doable move
                this.upcomingSelection = replayedEntity;
                return true;
            }
            return hasNext;
        }

        @Override
        protected Object createUpcomingSelection() {
            initialize();
            if (reachableEntityList.isEmpty()) {
                return noUpcomingSelection();
            }
            Object next;
            var bailoutSize = maxBailoutSize;
            do {
                bailoutSize--;
                var index = workingRandom.nextInt(Objects.requireNonNull(reachableEntityList).size());
                next = reachableEntityList.get(index);
                if (isReachable(next)) {
                    return next;
                }
            } while (bailoutSize > 0);
            return noUpcomingSelection();
        }

        @Override
        protected Object createPreviousSelection() {
            throw new UnsupportedOperationException();
        }
    }
}
