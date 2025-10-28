package ai.timefold.solver.core.impl.heuristic.selector.entity.decorator;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Objects;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public final class SortingEntitySelector<Solution_> extends AbstractCachingEntitySelector<Solution_> {

    private final SelectionSorter<Solution_, Object> sorter;
    private SolverScope<Solution_> solverScope;

    public SortingEntitySelector(EntitySelector<Solution_> childEntitySelector, SelectionCacheType cacheType,
            SelectionSorter<Solution_, Object> sorter) {
        super(childEntitySelector, cacheType);
        this.sorter = sorter;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    /**
     * The method ensures that cached items are loaded and sorted when the cache is set to STEP.
     * This logic is necessary
     * for making the node compatible with sorting elements at the STEP level when using entity-range.
     * For this specific use case,
     * we will fetch and sort the data after the phase has started but before the step begins.
     */
    private void ensureStepCacheIsLoaded() {
        if (cacheType != SelectionCacheType.STEP || cachedEntityList != null) {
            return;
        }
        // At this stage,
        // we attempt to load the entity list
        // since the iterator may have been requested prior to the start of the step.
        constructCache(solverScope);
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        this.solverScope = phaseScope.getSolverScope();
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        this.solverScope = null;
    }

    @Override
    public void constructCache(SolverScope<Solution_> solverScope) {
        if (cachedEntityList != null) {
            return;
        }
        super.constructCache(solverScope);
        sorter.sort(solverScope.getScoreDirector().getWorkingSolution(), cachedEntityList);
        logger.trace("    Sorted cachedEntityList: size ({}), entitySelector ({}).",
                cachedEntityList.size(), this);
    }

    @Override
    public boolean isNeverEnding() {
        return false;
    }

    @Override
    public long getSize() {
        ensureStepCacheIsLoaded();
        return super.getSize();
    }

    @Override
    public Iterator<Object> iterator() {
        ensureStepCacheIsLoaded();
        return cachedEntityList.iterator();
    }

    @Override
    public ListIterator<Object> listIterator() {
        ensureStepCacheIsLoaded();
        return cachedEntityList.listIterator();
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        ensureStepCacheIsLoaded();
        return cachedEntityList.listIterator(index);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        SortingEntitySelector<?> that = (SortingEntitySelector<?>) o;
        return Objects.equals(sorter, that.sorter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sorter);
    }

    @Override
    public String toString() {
        return "Sorting(" + childEntitySelector + ")";
    }

}
