package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Collections;
import java.util.Iterator;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.heuristic.move.CompositeMove;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.move.NoChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.preview.api.domain.metamodel.UnassignedElement;

/**
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class OriginalListSwapIterator<Solution_> extends UpcomingSelectionIterator<Move<Solution_>> {

    private final ListVariableStateSupply<Solution_> listVariableStateSupply;
    private final Iterator<Object> leftValueIterator;
    private final EntityIndependentValueSelector<Solution_> rightValueSelector;
    private Iterator<Object> rightValueIterator;
    private Object upcomingLeftValue;

    public OriginalListSwapIterator(ListVariableStateSupply<Solution_> listVariableStateSupply,
            EntityIndependentValueSelector<Solution_> leftValueSelector,
            EntityIndependentValueSelector<Solution_> rightValueSelector) {
        this.listVariableStateSupply = listVariableStateSupply;
        this.leftValueIterator = leftValueSelector.iterator();
        this.rightValueSelector = rightValueSelector;
        this.rightValueIterator = Collections.emptyIterator();
    }

    @Override
    protected Move<Solution_> createUpcomingSelection() {
        while (!rightValueIterator.hasNext()) {
            if (!leftValueIterator.hasNext()) {
                return noUpcomingSelection();
            }
            upcomingLeftValue = leftValueIterator.next();
            rightValueIterator = rightValueSelector.iterator();
        }

        var upcomingRightValue = rightValueIterator.next();
        return buildSwapMove(listVariableStateSupply, upcomingLeftValue, upcomingRightValue);
    }

    static <Solution_> Move<Solution_> buildSwapMove(ListVariableStateSupply<Solution_> listVariableStateSupply,
            Object upcomingLeftValue, Object upcomingRightValue) {
        if (upcomingLeftValue == upcomingRightValue) {
            return NoChangeMove.getInstance();
        }
        var listVariableDescriptor = listVariableStateSupply.getSourceVariableDescriptor();
        var upcomingLeft = listVariableStateSupply.getElementPosition(upcomingLeftValue);
        var upcomingRight = listVariableStateSupply.getElementPosition(upcomingRightValue);
        var leftUnassigned = upcomingLeft instanceof UnassignedElement;
        var rightUnassigned = upcomingRight instanceof UnassignedElement;
        if (leftUnassigned && rightUnassigned) { // No need to swap two unassigned elements.
            return NoChangeMove.getInstance();
        } else if (leftUnassigned) { // Unassign right, put left where right used to be.
            var rightDestination = upcomingRight.ensureAssigned();
            var unassignMove =
                    new ListUnassignMove<>(listVariableDescriptor, rightDestination.entity(), rightDestination.index());
            var assignMove = new ListAssignMove<>(listVariableDescriptor, upcomingLeftValue, rightDestination.entity(),
                    rightDestination.index());
            return CompositeMove.buildMove(unassignMove, assignMove);
        } else if (rightUnassigned) { // Unassign left, put right where left used to be.
            var leftDestination = upcomingLeft.ensureAssigned();
            var unassignMove =
                    new ListUnassignMove<>(listVariableDescriptor, leftDestination.entity(), leftDestination.index());
            var assignMove = new ListAssignMove<>(listVariableDescriptor, upcomingRightValue, leftDestination.entity(),
                    leftDestination.index());
            return CompositeMove.buildMove(unassignMove, assignMove);
        } else {
            var leftDestination = upcomingLeft.ensureAssigned();
            var rightDestination = upcomingRight.ensureAssigned();
            return new ListSwapMove<>(listVariableDescriptor, leftDestination.entity(), leftDestination.index(),
                    rightDestination.entity(), rightDestination.index());
        }
    }
}
