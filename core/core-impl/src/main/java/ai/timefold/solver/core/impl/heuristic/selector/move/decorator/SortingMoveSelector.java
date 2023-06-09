package ai.timefold.solver.core.impl.heuristic.selector.move.decorator;

import java.util.Iterator;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public class SortingMoveSelector<Solution_> extends AbstractCachingMoveSelector<Solution_> {

    protected final SelectionSorter<Solution_, Move<Solution_>> sorter;

    public SortingMoveSelector(MoveSelector<Solution_> childMoveSelector, SelectionCacheType cacheType,
            SelectionSorter<Solution_, Move<Solution_>> sorter) {
        super(childMoveSelector, cacheType);
        this.sorter = sorter;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void constructCache(SolverScope<Solution_> solverScope) {
        super.constructCache(solverScope);
        sorter.sort(solverScope.getScoreDirector(), cachedMoveList);
        logger.trace("    Sorted cachedMoveList: size ({}), moveSelector ({}).",
                cachedMoveList.size(), this);
    }

    @Override
    public boolean isNeverEnding() {
        return false;
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        return cachedMoveList.iterator();
    }

    @Override
    public String toString() {
        return "Sorting(" + childMoveSelector + ")";
    }

}
