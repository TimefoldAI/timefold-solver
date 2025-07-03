package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Collections;
import java.util.Iterator;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.move.NoChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.score.director.ValueRangeResolver;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;

/**
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class OriginalListChangeIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final ListVariableStateSupply<Solution_> listVariableStateSupply;
    private final ValueRangeResolver<Solution_> valueRangeResolver;
    private final Iterator<Object> valueIterator;
    private final DestinationSelector<Solution_> destinationSelector;
    private final boolean filterValuePerEntityRange;
    private Iterator<ElementPosition> destinationIterator;

    private Object upcomingValue;

    public OriginalListChangeIterator(ListVariableStateSupply<Solution_> listVariableStateSupply,
            ValueRangeResolver<Solution_> valueRangeResolver, EntityIndependentValueSelector<Solution_> valueSelector,
            DestinationSelector<Solution_> destinationSelector, boolean filterValuePerEntityRange) {
        this.listVariableStateSupply = listVariableStateSupply;
        this.valueRangeResolver = valueRangeResolver;
        this.valueIterator = valueSelector.iterator();
        this.destinationSelector = destinationSelector;
        this.filterValuePerEntityRange = filterValuePerEntityRange;
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
        var move = buildChangeMove(listVariableStateSupply, valueRangeResolver, upcomingValue, destinationIterator,
                filterValuePerEntityRange);
        if (move == null) {
            return noUpcomingSelection();
        } else {
            return move;
        }
    }

    static <Solution_> Move<Solution_> buildChangeMove(ListVariableStateSupply<Solution_> listVariableStateSupply,
            ValueRangeResolver<Solution_> valueRangeResolver, Object upcomingLeftValue,
            Iterator<ElementPosition> destinationIterator,
            boolean filterValuePerEntityRange) {
        var listVariableDescriptor = listVariableStateSupply.getSourceVariableDescriptor();
        ElementPosition upcomingDestination = null;
        if (filterValuePerEntityRange) {
            upcomingDestination = findUnpinnedAndValidDestination(valueRangeResolver, upcomingLeftValue, destinationIterator,
                    listVariableDescriptor);
        } else {
            upcomingDestination = findUnpinnedDestination(destinationIterator, listVariableDescriptor);
        }
        if (upcomingDestination == null) {
            return null;
        }
        var upcomingSource = listVariableStateSupply.getElementPosition(upcomingLeftValue);
        if (upcomingSource instanceof PositionInList sourceElement) {
            if (upcomingDestination instanceof PositionInList destinationElement) {
                return new ListChangeMove<>(listVariableDescriptor, sourceElement.entity(), sourceElement.index(),
                        destinationElement.entity(), destinationElement.index());
            } else {
                return new ListUnassignMove<>(listVariableDescriptor, sourceElement.entity(), sourceElement.index());
            }
        } else {
            if (upcomingDestination instanceof PositionInList destinationElement) {
                return new ListAssignMove<>(listVariableDescriptor, upcomingLeftValue, destinationElement.entity(),
                        destinationElement.index());
            } else {
                // Only used in construction heuristics to give the CH an option to leave the element unassigned.
                return NoChangeMove.getInstance();
            }
        }
    }

    /**
     * This method looks for an element where the value matches the entity value range.
     * Filtering is necessary when using the entity value range for list variables.
     *
     * @param upcomingLeftValue never null
     * @param destinationIterator never null
     * @param listVariableDescriptor never null
     * @return null if no valid destination was found, at which point the iterator is exhausted.
     */
    private static <Solution_> ElementPosition findUnpinnedAndValidDestination(ValueRangeResolver<Solution_> valueRangeResolver,
            Object upcomingLeftValue,
            Iterator<ElementPosition> destinationIterator, ListVariableDescriptor<Solution_> listVariableDescriptor) {
        while (destinationIterator.hasNext()) {
            var destination = destinationIterator.next();
            if (!isPinned(destination, listVariableDescriptor) && destination instanceof PositionInList destinationElement) {
                var valueRange = valueRangeResolver.extractValueRange(listVariableDescriptor.getValueRangeDescriptor(), null,
                        destinationElement.entity());
                if (valueRange.contains(upcomingLeftValue)) {
                    return destination;
                }
            }
        }
        return null;
    }

}
