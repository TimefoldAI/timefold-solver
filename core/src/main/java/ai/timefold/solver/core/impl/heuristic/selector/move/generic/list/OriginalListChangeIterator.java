package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Collections;
import java.util.Iterator;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.move.NoChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.ElementLocation;
import ai.timefold.solver.core.impl.heuristic.selector.list.LocationInList;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;

/**
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class OriginalListChangeIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final ListVariableStateSupply<Solution_> listVariableStateSupply;
    private final Iterator<Object> valueIterator;
    private final DestinationSelector<Solution_> destinationSelector;
    private Iterator<ElementLocation> destinationIterator;

    private Object upcomingValue;

    public OriginalListChangeIterator(ListVariableStateSupply<Solution_> listVariableStateSupply,
            EntityIndependentValueSelector<Solution_> valueSelector, DestinationSelector<Solution_> destinationSelector) {
        this.listVariableStateSupply = listVariableStateSupply;
        this.valueIterator = valueSelector.iterator();
        this.destinationSelector = destinationSelector;
        this.destinationIterator = Collections.emptyIterator();
    }

    @Override
    protected Move<Solution_> createUpcomingSelection() {
        while (!destinationIterator.hasNext()) {
            if (!valueIterator.hasNext()) {
                return noUpcomingSelection();
            }
            upcomingValue = valueIterator.next();
            destinationIterator = destinationSelector.iterator();
        }
        var move = buildChangeMove(listVariableStateSupply, upcomingValue, destinationIterator);
        if (move == null) {
            return noUpcomingSelection();
        } else {
            return move;
        }
    }

    static <Solution_> Move<Solution_> buildChangeMove(ListVariableStateSupply<Solution_> listVariableStateSupply,
            Object upcomingLeftValue, Iterator<ElementLocation> destinationIterator) {
        var listVariableDescriptor = listVariableStateSupply.getSourceVariableDescriptor();
        var upcomingDestination = findUnpinnedDestination(destinationIterator, listVariableDescriptor);
        if (upcomingDestination == null) {
            return null;
        }
        var upcomingSource = listVariableStateSupply.getLocationInList(upcomingLeftValue);
        if (upcomingSource instanceof LocationInList sourceElement) {
            if (upcomingDestination instanceof LocationInList destinationElement) {
                return new ListChangeMove<>(listVariableDescriptor, sourceElement.entity(), sourceElement.index(),
                        destinationElement.entity(), destinationElement.index());
            } else {
                return new ListUnassignMove<>(listVariableDescriptor, sourceElement.entity(), sourceElement.index());
            }
        } else {
            if (upcomingDestination instanceof LocationInList destinationElement) {
                return new ListAssignMove<>(listVariableDescriptor, upcomingLeftValue, destinationElement.entity(),
                        destinationElement.index());
            } else {
                // Only used in construction heuristics to give the CH an option to leave the element unassigned.
                return NoChangeMove.getInstance();
            }
        }
    }

}
