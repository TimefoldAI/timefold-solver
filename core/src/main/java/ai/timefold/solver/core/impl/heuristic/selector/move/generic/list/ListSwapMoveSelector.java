package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelector.filterPinnedListPlanningVariableValuesWithIndex;

import java.util.Iterator;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.GenericMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public class ListSwapMoveSelector<Solution_> extends GenericMoveSelector<Solution_> {

    private final EntityIndependentValueSelector<Solution_> leftValueSelector;
    private final EntityIndependentValueSelector<Solution_> rightValueSelector;
    private final boolean randomSelection;

    private ListVariableStateSupply<Solution_> listVariableStateSupply;

    public ListSwapMoveSelector(EntityIndependentValueSelector<Solution_> leftValueSelector,
            EntityIndependentValueSelector<Solution_> rightValueSelector, boolean randomSelection) {
        this.leftValueSelector =
                filterPinnedListPlanningVariableValuesWithIndex(leftValueSelector, this::getListVariableStateSupply);
        this.rightValueSelector =
                filterPinnedListPlanningVariableValuesWithIndex(rightValueSelector, this::getListVariableStateSupply);
        this.randomSelection = randomSelection;

        phaseLifecycleSupport.addEventListener(this.leftValueSelector);
        phaseLifecycleSupport.addEventListener(this.rightValueSelector);
    }

    private ListVariableStateSupply<Solution_> getListVariableStateSupply() {
        return Objects.requireNonNull(listVariableStateSupply,
                "Impossible state: The listVariableStateSupply is not initialized yet.");
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        var listVariableDescriptor = (ListVariableDescriptor<Solution_>) leftValueSelector.getVariableDescriptor();
        var supplyManager = solverScope.getScoreDirector().getSupplyManager();
        listVariableStateSupply = supplyManager.demand(listVariableDescriptor.getStateDemand());
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        listVariableStateSupply = null;
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        if (randomSelection) {
            return new RandomListSwapIterator<>(listVariableStateSupply, leftValueSelector, rightValueSelector);
        } else {
            return new OriginalListSwapIterator<>(listVariableStateSupply, leftValueSelector, rightValueSelector);
        }
    }

    @Override
    public boolean isCountable() {
        return leftValueSelector.isCountable() && rightValueSelector.isCountable();
    }

    @Override
    public boolean isNeverEnding() {
        return randomSelection || leftValueSelector.isNeverEnding() || rightValueSelector.isNeverEnding();
    }

    @Override
    public long getSize() {
        return leftValueSelector.getSize() * rightValueSelector.getSize();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + leftValueSelector + ", " + rightValueSelector + ")";
    }
}
