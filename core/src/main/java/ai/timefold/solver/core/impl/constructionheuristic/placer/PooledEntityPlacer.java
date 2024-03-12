package ai.timefold.solver.core.impl.constructionheuristic.placer;

import java.util.Iterator;

import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.decorator.FilteringMoveSelector;

public class PooledEntityPlacer<Solution_> extends AbstractEntityPlacer<Solution_> implements EntityPlacer<Solution_> {

    protected final MoveSelector<Solution_> moveSelector;

    public PooledEntityPlacer(MoveSelector<Solution_> moveSelector) {
        this.moveSelector = moveSelector;
        phaseLifecycleSupport.addEventListener(moveSelector);
    }

    @Override
    public Iterator<Placement<Solution_>> iterator() {
        return new PooledEntityPlacingIterator();
    }

    @Override
    public EntityPlacer<Solution_> rebuildWithFilter(SelectionFilter<Solution_, Object> filter) {
        return new PooledEntityPlacer<>(FilteringMoveSelector.of(moveSelector, filter::accept));
    }

    private class PooledEntityPlacingIterator extends UpcomingSelectionIterator<Placement<Solution_>> {

        private PooledEntityPlacingIterator() {
        }

        @Override
        protected Placement<Solution_> createUpcomingSelection() {
            Iterator<Move<Solution_>> moveIterator = moveSelector.iterator();
            if (!moveIterator.hasNext()) {
                return noUpcomingSelection();
            }
            return new Placement<>(moveIterator);
        }

    }

}
