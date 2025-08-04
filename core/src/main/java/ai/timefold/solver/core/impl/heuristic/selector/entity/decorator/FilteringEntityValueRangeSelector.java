package ai.timefold.solver.core.impl.heuristic.selector.entity.decorator;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Random;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractCachingEnabledSelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.ReachableValueMatrix;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.jspecify.annotations.NonNull;

/**
 * The decorator returns a list of reachable entities for a specific value.
 * It enables the creation of a filtering tier when using entity value selectors,
 * ensuring only valid and reachable entities are returned.
 *
 * e1 = entity_range[v1, v2, v3]
 * e2 = entity_range[v1, v4]
 *
 * v1 = [e1, e2]
 * v2 = [e1]
 * v3 = [e1]
 * v4 = [e2]
 *
 * @param <Solution_> the solution type
 */
public final class FilteringEntityValueRangeSelector<Solution_>
        extends AbstractCachingEnabledSelector<Solution_, ReachableValueMatrix> implements EntitySelector<Solution_> {

    private final IterableValueSelector<Solution_> replayingValueSelector;
    private final EntitySelector<Solution_> childEntitySelector;
    private final boolean randomSelection;

    private long entitiesSize;

    public FilteringEntityValueRangeSelector(EntitySelector<Solution_> childEntitySelector,
            IterableValueSelector<Solution_> replayingValueSelector, boolean randomSelection) {
        super(SelectionCacheType.PHASE, SelectionCacheType.STEP);
        this.replayingValueSelector = replayingValueSelector;
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
        this.entitiesSize = childEntitySelector.getEntityDescriptor().extractEntities(phaseScope.getWorkingSolution()).size();
        this.childEntitySelector.phaseStarted(phaseScope);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        resetCacheItem();
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public EntitySelector<Solution_> getChildEntitySelector() {
        return childEntitySelector;
    }

    @Override
    public @NonNull ReachableValueMatrix buildCacheItem(@NonNull InnerScoreDirector<Solution_, ?> scoreDirector) {
        return scoreDirector.getValueRangeManager()
                .getReachableValeMatrix(childEntitySelector.getEntityDescriptor().getGenuineListVariableDescriptor());
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

    @Override
    public Iterator<Object> endingIterator() {
        return new OriginalFilteringValueRangeIterator(replayingValueSelector.iterator(), getCachedItem());
    }

    @Override
    public Iterator<Object> iterator() {
        if (randomSelection) {
            return new EntityRandomFilteringValueRangeIterator(replayingValueSelector.iterator(), getCachedItem(),
                    workingRandom);
        } else {
            return new OriginalFilteringValueRangeIterator(replayingValueSelector.iterator(), getCachedItem());
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
        return other instanceof FilteringEntityValueRangeSelector<?> that
                && Objects.equals(childEntitySelector, that.childEntitySelector)
                && Objects.equals(replayingValueSelector, that.replayingValueSelector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(childEntitySelector, replayingValueSelector);
    }

    private static class OriginalFilteringValueRangeIterator extends UpcomingSelectionIterator<Object> {

        private final Iterator<Object> valueIterator;
        private final ReachableValueMatrix reachableValueMatrix;
        private Iterator<Object> otherIterator;

        private OriginalFilteringValueRangeIterator(Iterator<Object> valueIterator, ReachableValueMatrix reachableValueMatrix) {
            this.valueIterator = valueIterator;
            this.reachableValueMatrix = Objects.requireNonNull(reachableValueMatrix);
        }

        private void initialize() {
            if (otherIterator != null) {
                return;
            }
            var allValues = reachableValueMatrix.extractReachableEntitiesAsList(Objects.requireNonNull(valueIterator.next()));
            this.otherIterator = Objects.requireNonNull(allValues).iterator();
        }

        @Override
        protected Object createUpcomingSelection() {
            initialize();
            if (!otherIterator.hasNext()) {
                return noUpcomingSelection();
            }
            return otherIterator.next();
        }
    }

    private static class EntityRandomFilteringValueRangeIterator extends UpcomingSelectionIterator<Object> {

        private final Iterator<Object> valueIterator;
        private final ReachableValueMatrix reachableValueMatrix;
        private final Random workingRandom;
        private Object currentUpcomingValue;
        private List<Object> entityList;

        private EntityRandomFilteringValueRangeIterator(Iterator<Object> valueIterator,
                ReachableValueMatrix reachableValueMatrix, Random workingRandom) {
            this.valueIterator = valueIterator;
            this.reachableValueMatrix = Objects.requireNonNull(reachableValueMatrix);
            this.workingRandom = workingRandom;
        }

        private void initialize() {
            if (entityList != null) {
                return;
            }
            this.currentUpcomingValue = Objects.requireNonNull(valueIterator.next());
            loadValues();
        }

        private void loadValues() {
            upcomingCreated = false;
            this.entityList = reachableValueMatrix.extractReachableEntitiesAsList(currentUpcomingValue);
        }

        @Override
        public boolean hasNext() {
            if (valueIterator.hasNext() && currentUpcomingValue != null) {
                var updatedUpcomingValue = valueIterator.next();
                if (updatedUpcomingValue != currentUpcomingValue) {
                    // The iterator is reused in the ElementPositionRandomIterator,
                    // even if the value has changed.
                    // Therefore,
                    // we need to update the value list to ensure it is consistent.
                    this.currentUpcomingValue = updatedUpcomingValue;
                    loadValues();
                }
            }
            return super.hasNext();
        }

        @Override
        protected Object createUpcomingSelection() {
            initialize();
            if (entityList.isEmpty()) {
                return noUpcomingSelection();
            }
            var index = workingRandom.nextInt(entityList.size());
            return entityList.get(index);
        }
    }

}
