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
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SwapMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SwapMoveSelectorFactory;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
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
    private final List<SelectionFilter<Solution_, Object>> filterList;
    private final boolean randomSelection;

    private Object replayedEntity;
    private BasicVariableDescriptor<Solution_>[] basicVariableDescriptors;
    private InnerScoreDirector<Solution_, ?> innerScoreDirector;
    private List<Object> allEntities;

    private static long countIterations = 0;
    private static long countAttempts = 0;
    private static long countSuccess = 0;
    private static long countFails = 0;

    public FilteringEntityByEntitySelector(EntitySelector<Solution_> childEntitySelector,
            EntitySelector<Solution_> replayingEntitySelector, List<SelectionFilter<Solution_, Object>> filterList,
            boolean randomSelection) {
        this.childEntitySelector = childEntitySelector;
        this.replayingEntitySelector = replayingEntitySelector;
        this.filterList = filterList;
        this.randomSelection = randomSelection;
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        this.childEntitySelector.solvingStarted(solverScope);
        this.innerScoreDirector = solverScope.getScoreDirector();
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
        this.innerScoreDirector = null;
        this.replayedEntity = null;
        this.allEntities = null;
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
        return new OriginalFilteringValueRangeIterator<>(innerScoreDirector, this::selectReplayedEntity,
                childEntitySelector.listIterator(), filterList, basicVariableDescriptors);
    }

    @Override
    public Iterator<Object> iterator() {
        if (randomSelection) {
            if (basicVariableDescriptors.length == 1) {
                return new SingleVariableRandomFilteringValueRangeIterator<>(innerScoreDirector, this::selectReplayedEntity,
                        allEntities, filterList, basicVariableDescriptors[0], workingRandom);
            } else {
                return new MultiVariableRandomFilteringValueRangeIterator<>(innerScoreDirector, this::selectReplayedEntity,
                        allEntities, filterList, basicVariableDescriptors, workingRandom);
            }
        } else {
            return new OriginalFilteringValueRangeIterator<>(innerScoreDirector, this::selectReplayedEntity,
                    childEntitySelector.listIterator(), filterList, basicVariableDescriptors);
        }
    }

    @Override
    public ListIterator<Object> listIterator() {
        if (!randomSelection) {
            return new OriginalFilteringValueRangeIterator<>(innerScoreDirector, this::selectReplayedEntity,
                    childEntitySelector.listIterator(), filterList, basicVariableDescriptors);
        } else {
            throw new IllegalStateException("The selector (%s) does not support a ListIterator with randomSelection (%s)."
                    .formatted(this, randomSelection));
        }
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        if (!randomSelection) {
            return new OriginalFilteringValueRangeIterator<>(innerScoreDirector, this::selectReplayedEntity,
                    childEntitySelector.listIterator(index), filterList, basicVariableDescriptors);
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
        private final InnerScoreDirector<Solution_, ?> innerScoreDirector;
        private final Supplier<Object> upcomingEntitySupplier;
        private final List<SelectionFilter<Solution_, Object>> filterList;
        private final BasicVariableDescriptor<Solution_>[] basicVariableDescriptors;
        private final ValueRangeManager<Solution_> valueRangeManager;
        private boolean initialized = false;
        private Object replayedEntity;

        private AbstractFilteringValueRangeIterator(InnerScoreDirector<Solution_, ?> innerScoreDirector,
                Supplier<Object> upcomingEntitySupplier, List<SelectionFilter<Solution_, Object>> filterList,
                BasicVariableDescriptor<Solution_>[] basicVariableDescriptors) {
            this.innerScoreDirector = innerScoreDirector;
            this.upcomingEntitySupplier = upcomingEntitySupplier;
            this.filterList = filterList;
            this.basicVariableDescriptors = basicVariableDescriptors;
            this.valueRangeManager = innerScoreDirector.getValueRangeManager();
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
         * Apply entity selector filtering
         * to potentially filter out entities before analyzing if they are reachable to the replayed one.
         */
        boolean isFilterValid(Object entity) {
            if (filterList.isEmpty()) {
                return true;
            }
            for (var filter : filterList) {
                if (!filter.accept(innerScoreDirector, entity)) {
                    return false;
                }
            }
            return true;
        }

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

        private OriginalFilteringValueRangeIterator(InnerScoreDirector<Solution_, ?> innerScoreDirector,
                Supplier<Object> upcomingEntitySupplier, ListIterator<Object> entityIterator,
                List<SelectionFilter<Solution_, Object>> filterList,
                BasicVariableDescriptor<Solution_>[] basicVariableDescriptors) {
            super(innerScoreDirector, upcomingEntitySupplier, filterList, basicVariableDescriptors);
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
                if (isFilterValid(entity) && isReachable(entity)) {
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
        private final ReachableValues<Solution_> reachableValues;
        private final Random workingRandom;
        private int maxBailoutSize;
        private Object currentReplayedEntity = null;
        private Object currentReplayedValue = null;
        private List<Object> entities;

        @SuppressWarnings("unchecked")
        private SingleVariableRandomFilteringValueRangeIterator(InnerScoreDirector<Solution_, ?> innerScoreDirector,
                Supplier<Object> upcomingEntitySupplier, List<Object> allEntities,
                List<SelectionFilter<Solution_, Object>> filterList, BasicVariableDescriptor<Solution_> basicVariableDescriptor,
                Random workingRandom) {
            super(innerScoreDirector, upcomingEntitySupplier, filterList,
                    new BasicVariableDescriptor[] { basicVariableDescriptor });
            this.basicVariableDescriptor = basicVariableDescriptor;
            this.allEntities = allEntities;
            this.reachableValues = innerScoreDirector.getValueRangeManager().getReachableValues(basicVariableDescriptor);
            this.workingRandom = workingRandom;
        }

        @Override
        void load(Object replayedEntity) {
            this.currentReplayedEntity = replayedEntity;
            this.currentReplayedValue = basicVariableDescriptor.getValue(replayedEntity);
            if (currentReplayedValue == null) {
                // No value is assigned, then we test all available entities
                this.entities = allEntities;
            } else {
                // Only reachable entities to the values are evaluated
                this.entities = reachableValues.extractEntitiesAsList(currentReplayedValue);
            }
            this.maxBailoutSize = entities.size();
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
            while (bailoutSize > 0) {
                bailoutSize--;
                var index = workingRandom.nextInt(entities.size());
                var nextEntity = entities.get(index);
                if (!isFilterValid(nextEntity)) {
                    continue;
                }
                var nextValue = basicVariableDescriptor.getValue(nextEntity);
                var isReachable = currentReplayedValue != null ? isOtherValueReachable(currentReplayedEntity, nextValue)
                        : isReachable(currentReplayedEntity, null, nextEntity, nextValue);
                if (isReachable) {
                    countAttempts += maxBailoutSize - bailoutSize;
                    countSuccess++;
                    return nextEntity;
                }
            }
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
     * @param <Solution_> the solution type
     */
    private static class MultiVariableRandomFilteringValueRangeIterator<Solution_>
            extends AbstractFilteringValueRangeIterator<Solution_> {

        private final List<Object> allEntities;
        private final Random workingRandom;
        private final int maxBailoutSize;
        private Object currentReplayedEntity = null;

        private MultiVariableRandomFilteringValueRangeIterator(InnerScoreDirector<Solution_, ?> innerScoreDirector,
                Supplier<Object> upcomingEntitySupplier, List<Object> allEntities,
                List<SelectionFilter<Solution_, Object>> filterList,
                BasicVariableDescriptor<Solution_>[] basicVariableDescriptors, Random workingRandom) {
            super(innerScoreDirector, upcomingEntitySupplier, filterList, basicVariableDescriptors);
            this.allEntities = allEntities;
            this.workingRandom = workingRandom;
            this.maxBailoutSize = allEntities.size();
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
                if (isFilterValid(next) && isReachable(currentReplayedEntity, next)) {
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
