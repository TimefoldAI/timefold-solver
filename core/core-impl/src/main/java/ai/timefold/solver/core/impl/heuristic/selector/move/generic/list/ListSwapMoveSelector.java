package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelector.filterPinnedListPlanningVariableValuesWithIndex;

import java.util.Iterator;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexVariableDemand;
import ai.timefold.solver.core.impl.domain.variable.index.IndexVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonListInverseVariableDemand;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.GenericMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public class ListSwapMoveSelector<Solution_> extends GenericMoveSelector<Solution_> {

    private final EntityIndependentValueSelector<Solution_> leftValueSelector;
    private final EntityIndependentValueSelector<Solution_> rightValueSelector;
    private final boolean randomSelection;

    private SingletonInverseVariableSupply inverseVariableSupply;
    private IndexVariableSupply indexVariableSupply;
    private EntityIndependentValueSelector<Solution_> movableLeftValueSelector;
    private EntityIndependentValueSelector<Solution_> movableRightValueSelector;

    public ListSwapMoveSelector(
            EntityIndependentValueSelector<Solution_> leftValueSelector,
            EntityIndependentValueSelector<Solution_> rightValueSelector,
            boolean randomSelection) {
        // TODO require not same
        this.leftValueSelector = leftValueSelector;
        this.rightValueSelector = rightValueSelector;
        this.randomSelection = randomSelection;
        phaseLifecycleSupport.addEventListener(leftValueSelector);
        if (leftValueSelector != rightValueSelector) {
            phaseLifecycleSupport.addEventListener(rightValueSelector);
        }
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        var listVariableDescriptor = (ListVariableDescriptor<Solution_>) leftValueSelector.getVariableDescriptor();
        var supplyManager = solverScope.getScoreDirector().getSupplyManager();
        inverseVariableSupply = supplyManager.demand(new SingletonListInverseVariableDemand<>(listVariableDescriptor));
        indexVariableSupply = supplyManager.demand(new IndexVariableDemand<>(listVariableDescriptor));
        movableLeftValueSelector =
                filterPinnedListPlanningVariableValuesWithIndex(leftValueSelector, inverseVariableSupply, indexVariableSupply);
        movableRightValueSelector =
                filterPinnedListPlanningVariableValuesWithIndex(rightValueSelector, inverseVariableSupply, indexVariableSupply);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        inverseVariableSupply = null;
        indexVariableSupply = null;
        movableLeftValueSelector = null;
        movableRightValueSelector = null;
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        if (randomSelection) {
            return new RandomListSwapIterator<>(
                    inverseVariableSupply,
                    indexVariableSupply,
                    movableLeftValueSelector,
                    movableRightValueSelector);
        } else {
            return new OriginalListSwapIterator<>(
                    inverseVariableSupply,
                    indexVariableSupply,
                    movableLeftValueSelector,
                    movableRightValueSelector);
        }
    }

    @Override
    public boolean isCountable() {
        return movableLeftValueSelector.isCountable() && movableRightValueSelector.isCountable();
    }

    @Override
    public boolean isNeverEnding() {
        return randomSelection || movableLeftValueSelector.isNeverEnding() || movableRightValueSelector.isNeverEnding();
    }

    @Override
    public long getSize() {
        return movableLeftValueSelector.getSize() * movableRightValueSelector.getSize();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + leftValueSelector + ", " + rightValueSelector + ")";
    }
}
