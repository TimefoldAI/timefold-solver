package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Collections;
import java.util.Iterator;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.variable.ListVariableState;
import ai.timefold.solver.core.impl.heuristic.move.SelectorBasedNoChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.Move;

/**
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class OriginalListChangeIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final ListVariableState<Solution_, Object, Object> listVariableState;
    private final Iterator<Object> valueIterator;
    private final DestinationSelector<Solution_> destinationSelector;
    private Iterator<ElementPosition> destinationIterator;

    private Object upcomingValue;

    public OriginalListChangeIterator(ListVariableState<Solution_, Object, Object> listVariableState,
            IterableValueSelector<Solution_> valueSelector, DestinationSelector<Solution_> destinationSelector) {
        this.listVariableState = listVariableState;
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
        var move = buildChangeMove(listVariableState, upcomingValue, destinationIterator);
        if (move == null) {
            return noUpcomingSelection();
        } else {
            return move;
        }
    }

    static <Solution_> Move<Solution_> buildChangeMove(ListVariableState<Solution_, Object, Object> listVariableState,
            Object upcomingLeftValue, Iterator<ElementPosition> destinationIterator) {
        var listVariableDescriptor = listVariableState.getSourceVariableDescriptor();
        var upcomingDestination = findUnpinnedDestination(destinationIterator, listVariableDescriptor);
        if (upcomingDestination == null) {
            return null;
        }
        var upcomingSource = listVariableState.getElementPosition(upcomingLeftValue);
        if (upcomingSource instanceof PositionInList sourceElement) {
            if (upcomingDestination instanceof PositionInList destinationElement) {
                return new SelectorBasedListChangeMove<>(listVariableDescriptor, sourceElement.entity(), sourceElement.index(),
                        destinationElement.entity(), destinationElement.index());
            } else {
                return new SelectorBasedListUnassignMove<>(listVariableDescriptor, sourceElement.entity(),
                        sourceElement.index());
            }
        } else {
            if (upcomingDestination instanceof PositionInList destinationElement) {
                return new SelectorBasedListAssignMove<>(listVariableDescriptor, upcomingLeftValue, destinationElement.entity(),
                        destinationElement.index());
            } else {
                // Only used in construction heuristics to give the CH an option to leave the element unassigned.
                return SelectorBasedNoChangeMove.getInstance();
            }
        }
    }

}
