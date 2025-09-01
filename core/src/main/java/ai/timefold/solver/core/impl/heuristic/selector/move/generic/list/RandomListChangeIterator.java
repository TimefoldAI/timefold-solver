package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Iterator;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class RandomListChangeIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final ListVariableStateSupply<Solution_> listVariableStateSupply;
    private final Iterator<Object> valueIterator;
    private final Iterator<ElementPosition> destinationIterator;
    private final boolean checkValueRange;

    public RandomListChangeIterator(ListVariableStateSupply<Solution_> listVariableStateSupply,
            IterableValueSelector<Solution_> valueSelector, DestinationSelector<Solution_> destinationSelector,
            boolean checkValueRange) {
        this.listVariableStateSupply = listVariableStateSupply;
        this.valueIterator = valueSelector.iterator();
        this.destinationIterator = destinationSelector.iterator();
        this.checkValueRange = checkValueRange;
    }

    @Override
    protected Move<Solution_> createUpcomingSelection() {
        if (!valueIterator.hasNext()) {
            return noUpcomingSelection();
        }
        // The destination may depend on selecting the value before checking if it has a next value
        var upcomingValue = valueIterator.next();
        if (!destinationIterator.hasNext()) {
            return noUpcomingSelection();
        }
        var move = OriginalListChangeIterator.buildChangeMove(listVariableStateSupply, upcomingValue, destinationIterator,
                checkValueRange);
        if (move == null) {
            return noUpcomingSelection();
        } else {
            return move;
        }
    }
}
