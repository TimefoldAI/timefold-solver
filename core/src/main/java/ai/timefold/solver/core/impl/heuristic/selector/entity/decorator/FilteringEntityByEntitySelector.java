package ai.timefold.solver.core.impl.heuristic.selector.entity.decorator;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractDemandEnabledSelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.ReachableValues;
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
        // If there is only one basic variable, we load the reachable values structure,
        // as this process may take some time,
        // and it will be used by SingleVariableRandomFilteringValueRangeIterator
        if (basicVariableList.size() == 1) {
            valueRangeManager.getReachableValues(basicVariableDescriptors[0]);
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
            if (basicVariableDescriptors.length == 1) {
                return new SingleVariableRandomFilteringValueRangeIterator<>(this::selectReplayedEntity, allEntities,
                        basicVariableDescriptors[0], valueRangeManager.getReachableValues(basicVariableDescriptors[0]),
                        valueRangeManager, workingRandom);
            } else {
                // Experiments have shown that a large number of attempts do not scale well,
                // and 10 seems like an appropriate limit. 
                // So we won't spend excessive time trying to generate a single move for the current selection.
                // If we are unable to generate a move, the move iterator can still be used in later iterations.
                return new MultiVariableRandomFilteringValueRangeIterator<>(this::selectReplayedEntity, allEntities,
                        basicVariableDescriptors, valueRangeManager, workingRandom, 10);
            }
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

        boolean isReachable(Object entity, Object value, Object otherEntity, Object otherValue) {
            if (entity == otherEntity) {
                // Same entity cannot be swapped
                return false;
            }
            for (var basicVariableDescriptor : basicVariableDescriptors) {
                if (!isReachable(entity, value, otherEntity, otherValue, basicVariableDescriptor, valueRangeManager)) {
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

    /**
     * When there is only one basic single variable, the list of available entities can be optimized,
     * and the iterator will iterate only over the reachable entities of a given entity's assigned value.
     * 
     * @param <Solution_> the solution type
     */
    private static class SingleVariableRandomFilteringValueRangeIterator<Solution_>
            extends AbstractFilteringValueRangeIterator<Solution_> {

        private final BasicVariableDescriptor<Solution_> basicVariableDescriptor;
        private final List<Object> allEntities;
        private final ReachableValues reachableValues;
        private final Random workingRandom;
        private int maxBailoutSize;
        private Object currentReplayedEntity = null;
        private Object currentReplayedValue = null;
        private List<Object> entities;

        @SuppressWarnings("unchecked")
        private SingleVariableRandomFilteringValueRangeIterator(Supplier<Object> upcomingEntitySupplier,
                List<Object> allEntities, BasicVariableDescriptor<Solution_> basicVariableDescriptor,
                ReachableValues reachableValues, ValueRangeManager<Solution_> valueRangeManager, Random workingRandom) {
            super(upcomingEntitySupplier, new BasicVariableDescriptor[] { basicVariableDescriptor }, valueRangeManager);
            this.basicVariableDescriptor = basicVariableDescriptor;
            this.allEntities = allEntities;
            this.reachableValues = reachableValues;
            this.workingRandom = workingRandom;
        }

        @Override
        void load(Object replayedEntity) {
            this.currentReplayedEntity = replayedEntity;
            this.currentReplayedValue = basicVariableDescriptor.getValue(replayedEntity);
            if (currentReplayedValue == null) {
                // No value is assigned, then we test all available entities
                this.entities = allEntities;
                // Experiments have shown that a large number of attempts do not scale well,
                // and 10 seems like an appropriate limit.
                this.maxBailoutSize = 10;
            } else {
                // Only reachable entities to the values are evaluated
                this.entities = reachableValues.extractEntitiesAsList(currentReplayedValue);
                this.maxBailoutSize = entities.size();
            }
        }

        @Override
        public boolean hasNext() {
            initialize();
            return entities != null && !entities.isEmpty();
        }

        @Override
        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            countIterations++;
            var bailoutSize = maxBailoutSize;
            do {
                bailoutSize--;
                var index = workingRandom.nextInt(entities.size());
                var nextEntity = entities.get(index);
                var nextValue = basicVariableDescriptor.getValue(nextEntity);
                var isReachable = currentReplayedValue != null ? isOtherValueReachable(currentReplayedEntity, nextValue)
                        : isReachable(currentReplayedEntity, null, nextEntity, nextValue);
                if (isReachable) {
                    countAttempts += maxBailoutSize - bailoutSize;
                    countSuccess++;
                    return nextEntity;
                }
            } while (bailoutSize > 0);
            // If no reachable entity is found, we return the currently selected entity,
            // which will result in a non-doable move
            countAttempts += maxBailoutSize;
            countFails++;
            return currentReplayedEntity;
        }

        private boolean isOtherValueReachable(Object entity, Object otherValue) {
            return reachableValues.isEntityReachable(otherValue, entity);
        }
    }

    /**
     * The iterator will traverse all available entities
     * because building a list of reachable entities would require
     * reading the values of each variable to produce a unique list of entities.
     * Creating this list each time a new replayed entity is selected is not efficient.
     *
     * TODO - Develop an efficient structure to retrieve reachable entities from a specific entity.
     * 
     * @param <Solution_> the solution type
     */
    private static class MultiVariableRandomFilteringValueRangeIterator<Solution_>
            extends AbstractFilteringValueRangeIterator<Solution_> {

        private final List<Object> allEntities;
        private final Random workingRandom;
        private final int maxBailoutSize;
        private Object currentReplayedEntity = null;

        private MultiVariableRandomFilteringValueRangeIterator(Supplier<Object> upcomingEntitySupplier,
                List<Object> allEntities, BasicVariableDescriptor<Solution_>[] basicVariableDescriptors,
                ValueRangeManager<Solution_> valueRangeManager, Random workingRandom, int maxBailoutSize) {
            super(upcomingEntitySupplier, basicVariableDescriptors, valueRangeManager);
            this.allEntities = allEntities;
            this.workingRandom = workingRandom;
            this.maxBailoutSize = maxBailoutSize;
        }

        @Override
        void load(Object replayedEntity) {
            this.currentReplayedEntity = replayedEntity;
        }

        @Override
        public boolean hasNext() {
            initialize();
            return !allEntities.isEmpty();
        }

        @Override
        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            countIterations++;
            Object next;
            var bailoutSize = maxBailoutSize;
            do {
                bailoutSize--;
                var index = workingRandom.nextInt(allEntities.size());
                next = allEntities.get(index);
                if (isReachable(currentReplayedEntity, next)) {
                    countAttempts += maxBailoutSize - bailoutSize;
                    countSuccess++;
                    return next;
                }
            } while (bailoutSize > 0);
            // If no reachable entity is found, we return the currently selected entity,
            // which will result in a non-doable move
            countAttempts += maxBailoutSize;
            countFails++;
            return currentReplayedEntity;
        }
    }
}
