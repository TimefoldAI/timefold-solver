package ai.timefold.solver.core.impl.constructionheuristic.placer;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.decorator.FilteringEntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;

public class QueuedEntityPlacer<Solution_> extends AbstractEntityPlacer<Solution_> implements EntityPlacer<Solution_> {

    protected final EntitySelector<Solution_> entitySelector;
    protected final List<MoveSelector<Solution_>> moveSelectorList;

    public QueuedEntityPlacer(EntitySelector<Solution_> entitySelector, List<MoveSelector<Solution_>> moveSelectorList) {
        this.entitySelector = entitySelector;
        this.moveSelectorList = moveSelectorList;
        phaseLifecycleSupport.addEventListener(entitySelector);
        for (MoveSelector<Solution_> moveSelector : moveSelectorList) {
            phaseLifecycleSupport.addEventListener(moveSelector);
        }
    }

    @Override
    public Iterator<Placement<Solution_>> iterator() {
        return new QueuedEntityPlacingIterator(entitySelector.iterator());
    }

    @Override
    public EntityPlacer<Solution_> rebuildWithFilter(SelectionFilter<Solution_, Object> filter) {
        return new QueuedEntityPlacer<>(FilteringEntitySelector.of(entitySelector, filter), moveSelectorList);
    }

    private class QueuedEntityPlacingIterator extends UpcomingSelectionIterator<Placement<Solution_>> {

        private final Iterator<Object> entityIterator;
        private Iterator<MoveSelector<Solution_>> moveSelectorIterator;

        private QueuedEntityPlacingIterator(Iterator<Object> entityIterator) {
            this.entityIterator = entityIterator;
            moveSelectorIterator = Collections.emptyIterator();
        }

        @Override
        protected Placement<Solution_> createUpcomingSelection() {
            Iterator<Move<Solution_>> moveIterator = null;
            // Skip empty placements to avoid no-operation steps
            while (moveIterator == null || !moveIterator.hasNext()) {
                // If a moveSelector's iterator is empty, it might not be empty the next time
                // (because the entity changes)
                while (!moveSelectorIterator.hasNext()) {
                    if (!entityIterator.hasNext()) {
                        return noUpcomingSelection();
                    }
                    entityIterator.next();
                    moveSelectorIterator = moveSelectorList.iterator();
                }
                MoveSelector<Solution_> moveSelector = moveSelectorIterator.next();
                moveIterator = moveSelector.iterator();
            }
            return new Placement<>(moveIterator);
        }

    }

}
