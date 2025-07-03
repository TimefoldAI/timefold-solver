package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Iterator;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.score.director.ValueRangeResolver;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class RandomListChangeIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final ListVariableStateSupply<Solution_> listVariableStateSupply;
    private final ValueRangeResolver<Solution_> valueRangeResolver;
    private final Iterator<Object> valueIterator;
    private final Iterator<ElementPosition> destinationIterator;
    private final boolean filterValuePerEntityRange;

    public RandomListChangeIterator(ListVariableStateSupply<Solution_> listVariableStateSupply,
            ValueRangeResolver<Solution_> valueRangeResolver, EntityIndependentValueSelector<Solution_> valueSelector,
            DestinationSelector<Solution_> destinationSelector, boolean filterValuePerEntityRange) {
        this.listVariableStateSupply = listVariableStateSupply;
        this.valueRangeResolver = valueRangeResolver;
        this.valueIterator = valueSelector.iterator();
        this.destinationIterator = destinationSelector.iterator();
        this.filterValuePerEntityRange = filterValuePerEntityRange;
    }

    @Override
    protected Move<Solution_> createUpcomingSelection() {
        if (!valueIterator.hasNext() || !destinationIterator.hasNext()) {
            return noUpcomingSelection();
        }
        var upcomingValue = valueIterator.next();
        var move = OriginalListChangeIterator.buildChangeMove(listVariableStateSupply, valueRangeResolver, upcomingValue,
                destinationIterator, filterValuePerEntityRange);
        if (move == null) {
            return noUpcomingSelection();
        } else {
            return move;
        }
    }
}
