package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Collections;
import java.util.Iterator;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.variable.ListVariableDataSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.CompositeMove;
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

    private final ListVariableDataSupply<Solution_> listVariableDataSupply;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final Iterator<Object> valueIterator;
    private final DestinationSelector<Solution_> destinationSelector;
    private Iterator<ElementLocation> destinationIterator;

    private Object upcomingValue;

    public OriginalListChangeIterator(ListVariableDataSupply<Solution_> listVariableDataSupply,
            EntityIndependentValueSelector<Solution_> valueSelector, DestinationSelector<Solution_> destinationSelector) {
        this.listVariableDataSupply = listVariableDataSupply;
        this.listVariableDescriptor = (ListVariableDescriptor<Solution_>) valueSelector.getVariableDescriptor();
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
        var move = buildChangeMove(listVariableDescriptor, listVariableDataSupply, upcomingValue, destinationIterator);
        if (move == null) {
            return noUpcomingSelection();
        } else {
            return move;
        }
    }

    static <Solution_> Move<Solution_> buildChangeMove(ListVariableDescriptor<Solution_> listVariableDescriptor,
            ListVariableDataSupply<Solution_> listVariableDataSupply, Object upcomingLeftValue,
            Iterator<ElementLocation> destinationIterator) {
        var upcomingDestination = findUnpinnedDestination(destinationIterator, listVariableDescriptor);
        if (upcomingDestination == null) {
            return null;
        }
        var upcomingSource = listVariableDataSupply.getLocationInList(upcomingLeftValue);
        if (upcomingSource == null) {
            if (upcomingDestination instanceof LocationInList destinationElement) {
                return CompositeMove.buildMove(
                        new ListInitializeMove<>(listVariableDescriptor, upcomingLeftValue),
                        new ListAssignMove<>(listVariableDescriptor, upcomingLeftValue, destinationElement.entity(),
                                destinationElement.index()));
            } else {
                return new ListInitializeMove<>(listVariableDescriptor, upcomingLeftValue);
            }
        } else if (upcomingSource instanceof LocationInList sourceElement) {
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
                return NoChangeMove.getInstance();
            }
        }
    }

}
