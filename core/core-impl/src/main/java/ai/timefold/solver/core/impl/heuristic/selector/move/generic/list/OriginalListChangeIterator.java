package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.RandomListChangeIterator.findUnpinnedDestination;

import java.util.Collections;
import java.util.Iterator;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.ElementRef;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;

/**
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class OriginalListChangeIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final SingletonInverseVariableSupply inverseVariableSupply;
    private final IndexVariableSupply indexVariableSupply;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final Iterator<Object> valueIterator;
    private final DestinationSelector<Solution_> destinationSelector;
    private Iterator<ElementRef> destinationIterator;

    private Object upcomingSourceEntity;
    private Integer upcomingSourceIndex;
    private Object upcomingValue;

    public OriginalListChangeIterator(
            SingletonInverseVariableSupply inverseVariableSupply,
            IndexVariableSupply indexVariableSupply,
            EntityIndependentValueSelector<Solution_> valueSelector,
            DestinationSelector<Solution_> destinationSelector) {
        this.inverseVariableSupply = inverseVariableSupply;
        this.indexVariableSupply = indexVariableSupply;
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
            upcomingSourceEntity = inverseVariableSupply.getInverseSingleton(upcomingValue);
            upcomingSourceIndex = indexVariableSupply.getIndex(upcomingValue);

            destinationIterator = destinationSelector.iterator();
        }

        ElementRef destination = findUnpinnedDestination(destinationIterator, listVariableDescriptor);
        if (destination == null) {
            return noUpcomingSelection();
        }

        if (upcomingSourceEntity == null && upcomingSourceIndex == null) {
            return new ListAssignMove<>(
                    listVariableDescriptor,
                    upcomingValue,
                    destination.entity(),
                    destination.index());
        }

        // No need to generate ListUnassignMove because they are only used as undo moves.

        return new ListChangeMove<>(
                listVariableDescriptor,
                upcomingSourceEntity,
                upcomingSourceIndex,
                destination.entity(),
                destination.index());
    }
}
