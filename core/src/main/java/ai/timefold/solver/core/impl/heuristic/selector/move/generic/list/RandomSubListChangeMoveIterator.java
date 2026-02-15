package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Iterator;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubList;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubListSelector;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.Move;

class RandomSubListChangeMoveIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final Iterator<SubList> subListIterator;
    private final Iterator<ElementPosition> destinationIterator;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final RandomGenerator workingRandom;
    private final boolean selectReversingMoveToo;

    RandomSubListChangeMoveIterator(
            SubListSelector<Solution_> subListSelector,
            DestinationSelector<Solution_> destinationSelector,
            RandomGenerator workingRandom,
            boolean selectReversingMoveToo) {
        this.subListIterator = subListSelector.iterator();
        this.destinationIterator = destinationSelector.iterator();
        this.listVariableDescriptor = subListSelector.getVariableDescriptor();
        this.workingRandom = workingRandom;
        this.selectReversingMoveToo = selectReversingMoveToo;
    }

    @Override
    protected Move<Solution_> createUpcomingSelection() {
        if (!subListIterator.hasNext()) {
            return noUpcomingSelection();
        }
        // The inner node may need the outer iterator to select the next value first
        var subList = subListIterator.next();
        if (!destinationIterator.hasNext()) {
            return noUpcomingSelection();
        }
        var destination = findUnpinnedDestination(destinationIterator, listVariableDescriptor);
        if (destination == null) {
            return noUpcomingSelection();
        } else if (destination instanceof PositionInList destinationElement) {
            var reversing = selectReversingMoveToo && workingRandom.nextBoolean();
            return new SelectorBasedSubListChangeMove<>(listVariableDescriptor, subList, destinationElement.entity(),
                    destinationElement.index(), reversing);
        } else {
            // TODO add SubListAssignMove
            return new SelectorBasedSubListUnassignMove<>(listVariableDescriptor, subList);
        }
    }
}
