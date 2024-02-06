package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Iterator;

import ai.timefold.solver.core.impl.domain.variable.ListVariableDataSupply;
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

    private ListVariableDataSupply<Solution_> listVariableDataSupply;

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
        listVariableDataSupply = supplyManager.demand(listVariableDescriptor.getProvidedDemand());
        movableSourceValueSelector =
                filterPinnedListPlanningVariableValuesWithIndex(sourceValueSelector, listVariableDataSupply);
    }

    public static <Solution_> EntityIndependentValueSelector<Solution_> filterPinnedListPlanningVariableValuesWithIndex(
            EntityIndependentValueSelector<Solution_> sourceValueSelector,
            ListVariableDataSupply<Solution_> listVariableDataSupply) {
        var entityDescriptor = sourceValueSelector.getVariableDescriptor().getEntityDescriptor();
        var supportsPinning = entityDescriptor.supportsPinning();
        if (!supportsPinning) {
            // Don't incur the overhead of filtering values if there is no pinning support.
            return sourceValueSelector;
        }
        return (EntityIndependentValueSelector<Solution_>) FilteringValueSelector.of(sourceValueSelector,
                (scoreDirector, selection) -> {
                    var elementLocation = listVariableDataSupply.getLocationInList(selection);
                    if (elementLocation == null || elementLocation instanceof UnassignedLocation) {
                        return true;
                    }
                    var elementDestination = (LocationInList) elementLocation;
                    var entity = elementDestination.entity();
                    var pinningStatus = entityDescriptor.extractPinningStatus(scoreDirector, entity);
                    if (!pinningStatus.hasPin()) {
                        return true;
                    } else if (pinningStatus.entireEntityPinned()) {
                        return false;
                    }
                    var firstUnpinnedIndex = pinningStatus.firstUnpinnedIndex();
                    var index = elementDestination.index();
                    return index >= firstUnpinnedIndex;
                });
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        listVariableDataSupply = null;
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
                    listVariableDataSupply,
                    movableSourceValueSelector,
                    destinationSelector);
        } else {
            return new OriginalListChangeIterator<>(
                    listVariableDataSupply,
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
