package ai.timefold.solver.core.impl.constructionheuristic.placer;

import java.util.Collections;
import java.util.Iterator;

import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveIteratorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.EntityIndependentFilteringValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.FilteringValueSelector;

public class QueuedValuePlacer<Solution_> extends AbstractEntityPlacer<Solution_> implements EntityPlacer<Solution_> {

    protected final EntityIndependentValueSelector<Solution_> valueSelector;
    protected final MoveSelector<Solution_> moveSelector;

    public QueuedValuePlacer(EntityPlacerFactory<Solution_> factory, HeuristicConfigPolicy<Solution_> configPolicy,
            EntityIndependentValueSelector<Solution_> valueSelector, MoveSelector<Solution_> moveSelector) {
        super(factory, configPolicy);
        this.valueSelector = valueSelector;
        this.moveSelector = moveSelector;
        phaseLifecycleSupport.addEventListener(valueSelector);
        phaseLifecycleSupport.addEventListener(moveSelector);
    }

    @Override
    public Iterator<Placement<Solution_>> iterator() {
        return new QueuedValuePlacingIterator();
    }

    public boolean hasListChangeMoveSelector() {
        return moveSelector instanceof ListChangeMoveSelector<Solution_>;
    }

    private class QueuedValuePlacingIterator extends UpcomingSelectionIterator<Placement<Solution_>> {

        private Iterator<Object> valueIterator;

        private QueuedValuePlacingIterator() {
            valueIterator = Collections.emptyIterator();
        }

        @Override
        protected Placement<Solution_> createUpcomingSelection() {
            // If all values are used, there can still be entities uninitialized
            if (!valueIterator.hasNext()) {
                valueIterator = valueSelector.iterator();
                if (!valueIterator.hasNext()) {
                    return noUpcomingSelection();
                }
            }
            valueIterator.next();
            var moveIterator = moveSelector.iterator();
            // Because the valueSelector is entity independent, there is always a move if there's still an entity
            if (!moveIterator.hasNext()) {
                return noUpcomingSelection();
            }
            return new Placement<>(MoveIteratorFactory.adaptIterator(moveIterator));
        }

    }

    @Override
    public EntityPlacer<Solution_> rebuildWithFilter(SelectionFilter<Solution_, Object> filter) {
        return new QueuedValuePlacer<>(factory, configPolicy,
                (EntityIndependentFilteringValueSelector<Solution_>) FilteringValueSelector.of(valueSelector, filter),
                moveSelector);
    }

}
