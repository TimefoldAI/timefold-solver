package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Iterator;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.LocationInList;
import ai.timefold.solver.core.impl.heuristic.selector.list.UnassignedLocation;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.GenericMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.FilteringValueSelector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public class ListChangeMoveSelector<Solution_> extends GenericMoveSelector<Solution_> {

    private final EntityIndependentValueSelector<Solution_> sourceValueSelector;
    private EntityIndependentValueSelector<Solution_> movableSourceValueSelector;
    private final DestinationSelector<Solution_> destinationSelector;
    private final boolean randomSelection;

    private ListVariableStateSupply<Solution_> listVariableStateSupply;

    public ListChangeMoveSelector(
            EntityIndependentValueSelector<Solution_> sourceValueSelector,
            DestinationSelector<Solution_> destinationSelector,
            boolean randomSelection) {
        this.sourceValueSelector = sourceValueSelector;
        this.destinationSelector = destinationSelector;
        this.randomSelection = randomSelection;

        phaseLifecycleSupport.addEventListener(sourceValueSelector);
        phaseLifecycleSupport.addEventListener(destinationSelector);
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        var listVariableDescriptor = (ListVariableDescriptor<Solution_>) sourceValueSelector.getVariableDescriptor();
        var supplyManager = solverScope.getScoreDirector().getSupplyManager();
        listVariableStateSupply = supplyManager.demand(listVariableDescriptor.getStateDemand());
        movableSourceValueSelector =
                filterPinnedListPlanningVariableValuesWithIndex(sourceValueSelector, listVariableStateSupply);
    }

    public static <Solution_> EntityIndependentValueSelector<Solution_> filterPinnedListPlanningVariableValuesWithIndex(
            EntityIndependentValueSelector<Solution_> sourceValueSelector,
            ListVariableStateSupply<Solution_> listVariableStateSupply) {
        var listVariableDescriptor = listVariableStateSupply.getSourceVariableDescriptor();
        var supportsPinning = listVariableDescriptor.supportsPinning();
        if (!supportsPinning) {
            // Don't incur the overhead of filtering values if there is no pinning support.
            return sourceValueSelector;
        }
        return (EntityIndependentValueSelector<Solution_>) FilteringValueSelector.of(sourceValueSelector,
                (scoreDirector, selection) -> {
                    var elementLocation = listVariableStateSupply.getLocationInList(selection);
                    if (elementLocation instanceof UnassignedLocation) {
                        return true;
                    }
                    var elementDestination = (LocationInList) elementLocation;
                    var entity = elementDestination.entity();
                    return !listVariableDescriptor.isElementPinned(scoreDirector, entity, elementDestination.index());
                });
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        listVariableStateSupply = null;
        movableSourceValueSelector = null;
    }

    @Override
    public long getSize() {
        return movableSourceValueSelector.getSize() * destinationSelector.getSize();
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        if (randomSelection) {
            return new RandomListChangeIterator<>(
                    listVariableStateSupply,
                    movableSourceValueSelector,
                    destinationSelector);
        } else {
            return new OriginalListChangeIterator<>(
                    listVariableStateSupply,
                    movableSourceValueSelector,
                    destinationSelector);
        }
    }

    @Override
    public boolean isCountable() {
        return movableSourceValueSelector.isCountable() && destinationSelector.isCountable();
    }

    @Override
    public boolean isNeverEnding() {
        return randomSelection || movableSourceValueSelector.isNeverEnding() || destinationSelector.isNeverEnding();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + sourceValueSelector + ", " + destinationSelector + ")";
    }
}
