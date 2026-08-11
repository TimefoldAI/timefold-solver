package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelector.filterPinnedListPlanningVariableValuesWithIndex;

import java.util.Iterator;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.GenericMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.preview.api.move.Move;

public class ListSwapMoveSelector<Solution_> extends GenericMoveSelector<Solution_> {

    private final IterableValueSelector<Solution_> leftValueSelector;
    private final IterableValueSelector<Solution_> rightValueSelector;
    private final boolean randomSelection;

    private ListVariableStateSupply<Solution_, Object, Object> listVariableStateSupply;

    public ListSwapMoveSelector(IterableValueSelector<Solution_> leftValueSelector,
            IterableValueSelector<Solution_> rightValueSelector, boolean randomSelection) {
        this.leftValueSelector =
                filterPinnedListPlanningVariableValuesWithIndex(leftValueSelector, this::getListVariableStateSupply);
        this.rightValueSelector =
                filterPinnedListPlanningVariableValuesWithIndex(rightValueSelector, this::getListVariableStateSupply);
        this.randomSelection = randomSelection;

        phaseLifecycleSupport.addEventListener(this.leftValueSelector);
        phaseLifecycleSupport.addEventListener(this.rightValueSelector);
    }

    private ListVariableStateSupply<Solution_, Object, Object> getListVariableStateSupply() {
        return Objects.requireNonNull(listVariableStateSupply,
                "Impossible state: The listVariableStateSupply is not initialized yet.");
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        // The phase may operate in a different environment mode, which uses a new score director.
        // We must ensure that the list variable state supply remains up to date.
        var listVariableDescriptor = (ListVariableDescriptor<Solution_>) leftValueSelector.getVariableDescriptor();
        var supplyManager = phaseScope.getScoreDirector().getSupplyManager();
        listVariableStateSupply = supplyManager.demand(listVariableDescriptor.getStateDemand());
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
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
