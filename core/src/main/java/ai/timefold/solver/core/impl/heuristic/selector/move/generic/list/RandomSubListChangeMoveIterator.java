package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Iterator;
import java.util.Random;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.ElementLocation;
import ai.timefold.solver.core.impl.heuristic.selector.list.LocationInList;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubList;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubListSelector;

class RandomSubListChangeMoveIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final Iterator<SubList> subListIterator;
    private final Iterator<ElementLocation> destinationIterator;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final Random workingRandom;
    private final boolean selectReversingMoveToo;

    RandomSubListChangeMoveIterator(
            SubListSelector<Solution_> subListSelector,
            DestinationSelector<Solution_> destinationSelector,
            Random workingRandom,
            boolean selectReversingMoveToo) {
        this.subListIterator = subListSelector.iterator();
        this.destinationIterator = destinationSelector.iterator();
        this.listVariableDescriptor = subListSelector.getVariableDescriptor();
        this.workingRandom = workingRandom;
        this.selectReversingMoveToo = selectReversingMoveToo;
    }

    @Override
    protected Move<Solution_> createUpcomingSelection() {
        if (!subListIterator.hasNext() || !destinationIterator.hasNext()) {
            return noUpcomingSelection();
        }

        var subList = subListIterator.next();
        var destination = findUnpinnedDestination(destinationIterator, listVariableDescriptor);
        if (destination == null) {
            return noUpcomingSelection();
        } else if (destination instanceof LocationInList destinationElement) {
            var reversing = selectReversingMoveToo && workingRandom.nextBoolean();
            return new SubListChangeMove<>(listVariableDescriptor, subList, destinationElement.entity(),
                    destinationElement.index(), reversing);
        } else {
            // TODO add SubListAssignMove
            return new SubListUnassignMove<>(listVariableDescriptor, subList);
        }
    }
}
