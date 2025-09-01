package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.OriginalListSwapIterator.buildSwapMove;

import java.util.Iterator;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class RandomListSwapIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final ListVariableStateSupply<Solution_> listVariableStateSupply;
    private final Iterator<Object> leftValueIterator;
    private final Iterator<Object> rightValueIterator;

    public RandomListSwapIterator(ListVariableStateSupply<Solution_> listVariableStateSupply,
            IterableValueSelector<Solution_> leftValueSelector,
            IterableValueSelector<Solution_> rightValueSelector) {
        this.listVariableStateSupply = listVariableStateSupply;
        this.leftValueIterator = leftValueSelector.iterator();
        this.rightValueIterator = rightValueSelector.iterator();
    }

    @Override
    protected Move<Solution_> createUpcomingSelection() {
        if (!leftValueIterator.hasNext()) {
            return noUpcomingSelection();
        }
        var upcomingLeftValue = leftValueIterator.next();
        // The right iterator may depend on a selected value from the left iterator
        if (!rightValueIterator.hasNext()) {
            return noUpcomingSelection();
        }
        var upcomingRightValue = rightValueIterator.next();
        return buildSwapMove(listVariableStateSupply, upcomingLeftValue, upcomingRightValue);
    }
}
