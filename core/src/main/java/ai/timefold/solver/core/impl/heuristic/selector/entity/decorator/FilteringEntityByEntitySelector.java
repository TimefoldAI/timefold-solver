package ai.timefold.solver.core.impl.heuristic.selector.entity.decorator;

import java.util.Iterator;
import java.util.List;
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
 *
 * e1 = entity_range[v1, v2, v3]
 *
 * e2 = entity_range[v1, v4]
 *
 * e3 = entity_range[v1, v4, v5]
 *
 * </code>
 * <p>
 * Let's consider the following use-cases:
 * <ol>
 * <li>e1(null) - e2(null): e2 is reachable by e1 because both assigned values are null.</li>
 * <li>e1(v2) - e2(v1): e2 is not reachable by e1 because its value range does not accept v2.</li>
 * <li>e2(v1) - e3(v4): e3 is reachable by e2 because e2 accepts v4 and e3 accepts v1.</li>
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
    private List<Object> allEntities;

    private static long countIterations = 0;
    private static long countAttempts = 0;
    private static long countSuccess = 0;
    private static long countFails = 0;

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
        this.allEntities = childEntitySelector.getEntityDescriptor().extractEntities(phaseScope.getWorkingSolution());
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
        logger.warn(
                "Iterations ({}), total attempts ({}), average attempts ({}), succeed count ({}), succeed rate ({}), fails count({}), fails rate ({})",
                countIterations, countAttempts, countAttempts / (double) countIterations, countSuccess,
                countSuccess / (double) countIterations, countFails,
                countFails / (double) countIterations);
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
            this.replayedEntity = iterator.next();
        }
        return replayedEntity;
    }

    @Override
    public Iterator<Object> endingIterator() {
        return new OriginalFilteringValueRangeIterator<>(this::selectReplayedEntity, childEntitySelector.iterator(),
                basicVariableDescriptors, valueRangeManager);
    }

    @Override
    public Iterator<Object> iterator() {
        if (randomSelection) {
            if (!childEntitySelector.isNeverEnding()) {
                throw new IllegalArgumentException(
                        "Impossible state: childEntitySelector must provide a never-ending approach.");
            }
            // Experiments have shown that a large number of attempts do not scale well,
            // and 10 seems like an appropriate limit. 
            // So we won't spend excessive time trying to generate a single move for the current selection.
            // If we are unable to generate a move, the move iterator can still be used in later iterations.
            return new RandomFilteringValueRangeIterator<>(this::selectReplayedEntity, childEntitySelector.iterator(),
                    basicVariableDescriptors, valueRangeManager, 10);
        } else {
            return new OriginalFilteringValueRangeIterator<>(this::selectReplayedEntity, childEntitySelector.iterator(),
                    basicVariableDescriptors, valueRangeManager);
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
            var newReplayedEntity = upcomingEntitySupplier.get();
            if (newReplayedEntity != replayedEntity) {
                replayedEntity = newReplayedEntity;
                load(replayedEntity);
            }
        }

        abstract void load(Object replayedEntity);

        /**
         * The other entity is reachable if it accepts all assigned values from the replayed entity, and vice versa.
         */
        boolean isReachable(Object otherEntity) {
            return isReachable(replayedEntity, otherEntity);
        }

        boolean isReachable(Object entity, Object otherEntity) {
            if (entity == otherEntity) {
                // Same entity cannot be swapped
                return false;
            }
            for (var basicVariableDescriptor : basicVariableDescriptors) {
                if (!isReachable(entity, basicVariableDescriptor.getValue(entity), otherEntity,
                        basicVariableDescriptor.getValue(otherEntity), basicVariableDescriptor, valueRangeManager)) {
                    return false;
                }
            }
            return true;
        }

        private boolean isReachable(Object entity, Object value, Object otherEntity, Object otherValue,
                BasicVariableDescriptor<Solution_> variableDescriptor, ValueRangeManager<Solution_> valueRangeManager) {
            return valueRangeManager.getFromEntity(variableDescriptor.getValueRangeDescriptor(), entity).contains(otherValue)
                    && valueRangeManager.getFromEntity(variableDescriptor.getValueRangeDescriptor(), otherEntity)
                            .contains(value);
        }
    }

    private static class OriginalFilteringValueRangeIterator<Solution_> extends AbstractFilteringValueRangeIterator<Solution_> {

        private final Iterator<Object> entityIterator;
        private Object selected = null;

        private OriginalFilteringValueRangeIterator(Supplier<Object> upcomingEntitySupplier,
                Iterator<Object> entityIterator, BasicVariableDescriptor<Solution_>[] basicVariableDescriptors,
                ValueRangeManager<Solution_> valueRangeManager) {
            super(upcomingEntitySupplier, basicVariableDescriptors, valueRangeManager);
            this.entityIterator = entityIterator;
        }

        @Override
        void load(Object replayedEntity) {
            this.selected = null;
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

    }

    private static class RandomFilteringValueRangeIterator<Solution_>
            extends AbstractFilteringValueRangeIterator<Solution_> {

        private final Iterator<Object> entityIterator;
        private final int maxBailoutSize;
        private Object currentReplayedEntity = null;

        private RandomFilteringValueRangeIterator(Supplier<Object> upcomingEntitySupplier,
                Iterator<Object> entityIterator, BasicVariableDescriptor<Solution_>[] basicVariableDescriptors,
                ValueRangeManager<Solution_> valueRangeManager, int maxBailoutSize) {
            super(upcomingEntitySupplier, basicVariableDescriptors, valueRangeManager);
            this.entityIterator = entityIterator;
            this.maxBailoutSize = maxBailoutSize;
        }

        @Override
        void load(Object replayedEntity) {
            this.currentReplayedEntity = replayedEntity;
        }

        @Override
        public boolean hasNext() {
            initialize();
            return entityIterator.hasNext();
        }

        @Override
        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var bailoutSize = maxBailoutSize;
            do {
                bailoutSize--;
                // We expect the iterator to apply a random selection
                var next = entityIterator.next();
                if (isReachable(currentReplayedEntity, next)) {
                    return next;
                }
            } while (bailoutSize > 0);
            // If no reachable entity is found, we return the currently selected entity,
            // which will result in a non-doable move
            return currentReplayedEntity;
        }
    }
}
