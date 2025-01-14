package ai.timefold.solver.core.impl.constructionheuristic.placer;

import java.util.Iterator;

import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.decorator.FilteringMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveIteratorFactory;

public class PooledEntityPlacer<Solution_> extends AbstractEntityPlacer<Solution_> implements EntityPlacer<Solution_> {

    protected final MoveSelector<Solution_> moveSelector;

    public PooledEntityPlacer(EntityPlacerFactory<Solution_> factory, HeuristicConfigPolicy<Solution_> configPolicy,
            MoveSelector<Solution_> moveSelector) {
        super(factory, configPolicy);
        this.moveSelector = moveSelector;
        phaseLifecycleSupport.addEventListener(moveSelector);
    }

    @Override
    public Iterator<Placement<Solution_>> iterator() {
        return new PooledEntityPlacingIterator();
    }

    @Override
    public EntityPlacer<Solution_> rebuildWithFilter(SelectionFilter<Solution_, Object> filter) {
        return new PooledEntityPlacer<>(factory, configPolicy, FilteringMoveSelector.of(moveSelector, filter::accept));
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
            return new Placement<>(MoveIteratorFactory.adaptIterator(moveIterator));
        }

    }

}
