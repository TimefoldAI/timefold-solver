package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.OriginalListSwapIterator.buildSwapMove;

import java.util.Iterator;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.variable.ListVariableDataSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class RandomListSwapIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final ListVariableDataSupply<Solution_> listVariableDataSupply;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final Iterator<Object> leftValueIterator;
    private final Iterator<Object> rightValueIterator;

    public RandomListSwapIterator(ListVariableDataSupply<Solution_> listVariableDataSupply,
            EntityIndependentValueSelector<Solution_> leftValueSelector,
            EntityIndependentValueSelector<Solution_> rightValueSelector) {
        this.listVariableDataSupply = listVariableDataSupply;
        this.listVariableDescriptor = (ListVariableDescriptor<Solution_>) leftValueSelector.getVariableDescriptor();
        this.leftValueIterator = leftValueSelector.iterator();
        this.rightValueIterator = rightValueSelector.iterator();
    }

    @Override
    protected Move<Solution_> createUpcomingSelection() {
        if (!leftValueIterator.hasNext() || !rightValueIterator.hasNext()) {
            return noUpcomingSelection();
        }
        var upcomingLeftValue = leftValueIterator.next();
        var upcomingRightValue = rightValueIterator.next();
        return buildSwapMove(listVariableDescriptor, listVariableDataSupply, upcomingLeftValue, upcomingRightValue);
    }
}
