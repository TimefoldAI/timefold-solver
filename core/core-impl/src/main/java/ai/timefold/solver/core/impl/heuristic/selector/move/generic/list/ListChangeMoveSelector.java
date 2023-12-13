package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Iterator;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexVariableDemand;
import ai.timefold.solver.core.impl.domain.variable.index.IndexVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonListInverseVariableDemand;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.GenericMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.FilteringValueSelector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public class ListChangeMoveSelector<Solution_> extends GenericMoveSelector<Solution_> {

    private final EntityIndependentValueSelector<Solution_> sourceValueSelector;
    private EntityIndependentValueSelector<Solution_> movableSourceValueSelector;
    private final DestinationSelector<Solution_> destinationSelector;
    private final boolean randomSelection;

    private SingletonInverseVariableSupply inverseVariableSupply;
    private IndexVariableSupply indexVariableSupply;

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
        inverseVariableSupply = supplyManager.demand(new SingletonListInverseVariableDemand<>(listVariableDescriptor));
        movableSourceValueSelector = filterPinnedListPlanningVariableValues(sourceValueSelector, inverseVariableSupply);
        indexVariableSupply = supplyManager.demand(new IndexVariableDemand<>(listVariableDescriptor));
    }

    public static <Solution_> EntityIndependentValueSelector<Solution_> filterPinnedListPlanningVariableValues(
            EntityIndependentValueSelector<Solution_> sourceValueSelector,
            SingletonInverseVariableSupply inverseVariableSupply) {
        var entityDescriptor = sourceValueSelector.getVariableDescriptor().getEntityDescriptor();
        var hasMovableSelectionFilter = entityDescriptor.hasEffectiveMovableEntitySelectionFilter();
        if (!hasMovableSelectionFilter) {
            // Don't incur the overhead of filtering movable entities if there is no movable entity selection filter.
            return sourceValueSelector;
        }
        return (EntityIndependentValueSelector<Solution_>) FilteringValueSelector.create(sourceValueSelector,
                (scoreDirector, selection) -> {
                    var entity = inverseVariableSupply.getInverseSingleton(selection);
                    if (entity == null) { // Unassigned.
                        return true;
                    }
                    System.out.println("Selection " + selection + " " + entity + " " + entityDescriptor.isMovable(scoreDirector, entity));
                    return entityDescriptor.isMovable(scoreDirector, entity);
                });
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        inverseVariableSupply = null;
        movableSourceValueSelector = null;
        indexVariableSupply = null;
    }

    @Override
    public long getSize() {
        return movableSourceValueSelector.getSize() * destinationSelector.getSize();
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        if (randomSelection) {
            return new RandomListChangeIterator<>(
                    inverseVariableSupply,
                    indexVariableSupply,
                    movableSourceValueSelector,
                    destinationSelector);
        } else {
            return new OriginalListChangeIterator<>(
                    inverseVariableSupply,
                    indexVariableSupply,
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
