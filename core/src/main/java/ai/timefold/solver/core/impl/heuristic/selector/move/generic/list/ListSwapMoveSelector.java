package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import static ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelector.filterPinnedListPlanningVariableValuesWithIndex;

import java.util.Iterator;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.preview.api.move.Move;

public final class ListSwapMoveSelector<Solution_> extends GenericListMoveSelector<Solution_> {

    private final IterableValueSelector<Solution_> leftValueSelector;
    private final IterableValueSelector<Solution_> rightValueSelector;
    private final boolean randomSelection;

    public ListSwapMoveSelector(IterableValueSelector<Solution_> leftValueSelector,
            IterableValueSelector<Solution_> rightValueSelector, boolean randomSelection) {
        super((ListVariableDescriptor<Solution_>) leftValueSelector.getVariableDescriptor());
        this.leftValueSelector =
                filterPinnedListPlanningVariableValuesWithIndex(leftValueSelector, this::getListVariableStateSupply);
        this.rightValueSelector =
                filterPinnedListPlanningVariableValuesWithIndex(rightValueSelector, this::getListVariableStateSupply);
        this.randomSelection = randomSelection;

        phaseLifecycleSupport.addEventListener(this.leftValueSelector);
        phaseLifecycleSupport.addEventListener(this.rightValueSelector);
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
