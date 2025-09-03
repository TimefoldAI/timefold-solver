package ai.timefold.solver.core.impl.heuristic.selector.entity.decorator;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractDemandEnabledSelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SwapMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SwapMoveSelectorFactory;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * The decorator returns a list of reachable entities for a specific entity.
 * It enables the creation of a filtering tier when using entity-provided value ranges,
 * ensuring only valid and reachable entities are returned.
 * An entity is considered reachable to another entity
 * if the assigned values exist within their respective entity value ranges.
 * <p>
 * The decorator can only be applied to basic variables.
 * <code>
 * <p>
 * e1 = entity_range[v1, v2, v3]
 * <p>
 * e2 = entity_range[v1, v4]
 * <p>
 * e3 = entity_range[v1, v4, v5]
 * <p>
 * </code>
 * <p>
 * Let's consider the following use-cases:
 * <ol>
 * <li>e1(null) <-> e2(null): e2 is reachable by e1 because both assigned values are null.</li>
 * <li>e1(v2) <-> e2(v1): e2 is not reachable by e1 because its value range does not accept v2.</li>
 * <li>e2(v1) <-> e3(v4): e3 is reachable by e2 because e2 accepts v4 and e3 accepts v1.</li>
 * </ol>
 * <p>
 * This node is currently used by the {@link SwapMoveSelector} selector.
 * To explain its functionality, let's consider how moves are generated for the basic swap type.
 * Initially, the swap move selector employs a left entity selector to choose one entity.
 * Then, it uses a right entity selector to select another entity, with the goal of swapping their values.
 * <p>
 * Based on the previously described process and the current goal of this node,
 * we can observe that once an entity is selected using the left selector,
 * the right node can filter out all non-reachable entities and generate a valid move.
 * A move is considered valid only if both entities accept each other's values.
 * The filtering process of invalid entities allows the solver to explore the solution space more efficiently.
 * 
 * @see SwapMoveSelectorFactory
 * @see EntitySelectorFactory
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
            this.replayedEntity = iterator.next();
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
                        "Impossible state: childEntitySelector must provide a never-ending approach.");
            }
            return new RandomFilteringValueRangeIterator<>(this::selectReplayedEntity, childEntitySelector.iterator(),
                    basicVariableDescriptors, valueRangeManager, (int) childEntitySelector.getSize());
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

    private abstract static class AbstractFilteringValueRangeIterator<Solution_> implements Iterator<Object> {
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
            replayedEntity = upcomingEntitySupplier.get();
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
            for (var basicVariableDescriptor : basicVariableDescriptors) {
                if (!isReachable(replayedEntity, otherEntity, basicVariableDescriptor, valueRangeManager)) {
                    return false;
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

    private static class OriginalFilteringValueRangeIterator<Solution_> extends AbstractFilteringValueRangeIterator<Solution_>
            implements ListIterator<Object> {

        private final ListIterator<Object> entityIterator;
        private Object selected = null;

        private OriginalFilteringValueRangeIterator(Supplier<Object> upcomingEntitySupplier,
                ListIterator<Object> entityIterator, BasicVariableDescriptor<Solution_>[] basicVariableDescriptors,
                ValueRangeManager<Solution_> valueRangeManager) {
            super(upcomingEntitySupplier, basicVariableDescriptors, valueRangeManager);
            this.entityIterator = entityIterator;
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
            while (entityIterator.hasNext()) {
                var entity = entityIterator.next();
                if (isReachable(entity)) {
                    return entity;
                }
            }
            return null;
        }

        private Object pickSelected() {
            if (selected == null) {
                throw new NoSuchElementException();
            }
            var result = selected;
            this.selected = null;
            return result;
        }

        @Override
        public Object next() {
            return pickSelected();
        }

        @Override
        public boolean hasPrevious() {
            this.selected = pickPrevious();
            return selected != null;
        }

        private Object pickPrevious() {
            if (selected != null) {
                throw new IllegalStateException("The next value has already been picked.");
            }
            initialize();
            this.selected = null;
            while (entityIterator.hasPrevious()) {
                var entity = entityIterator.previous();
                if (isReachable(entity)) {
                    return entity;
                }
            }
            return null;
        }

        @Override
        public Object previous() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int nextIndex() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int previousIndex() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(Object o) {
            throw new UnsupportedOperationException();
        }
    }

    private static class RandomFilteringValueRangeIterator<Solution_> extends AbstractFilteringValueRangeIterator<Solution_> {

        private final Iterator<Object> entityIterator;
        private final int maxBailoutSize;

        private RandomFilteringValueRangeIterator(Supplier<Object> upcomingEntitySupplier, Iterator<Object> entityIterator,
                BasicVariableDescriptor<Solution_>[] basicVariableDescriptors, ValueRangeManager<Solution_> valueRangeManager,
                int maxBailoutSize) {
            super(upcomingEntitySupplier, basicVariableDescriptors, valueRangeManager);
            this.entityIterator = entityIterator;
            this.maxBailoutSize = maxBailoutSize;
        }

        @Override
        public boolean hasNext() {
            initialize();
            return entityIterator.hasNext();
        }

        @Override
        public Object next() {
            if (!hasNext()) {
                // If no reachable entity is found, we return the currently selected entity,
                // which will result in a non-doable move
                return currentReplayedEntity();
            }
            Object next;
            var bailoutSize = maxBailoutSize;
            do {
                bailoutSize--;
                // We expect the entity iterator to apply a never-ending random selection approach
                next = entityIterator.next();
                if (isReachable(next)) {
                    return next;
                }
            } while (bailoutSize > 0);
            // If no reachable entity is found, we return the currently selected entity,
            // which will result in a non-doable move
            return currentReplayedEntity();
        }
    }
}
