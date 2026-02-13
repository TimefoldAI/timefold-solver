package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.util.List;

import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;

/**
 * Abstract superclass for every composite {@link MoveSelector}.
 *
 * @see MoveSelector
 */
public abstract class CompositeMoveSelector<Solution_> extends AbstractMoveSelector<Solution_> {

    protected final List<MoveSelector<Solution_>> childMoveSelectorList;
    protected final boolean randomSelection;

    protected CompositeMoveSelector(List<MoveSelector<Solution_>> childMoveSelectorList, boolean randomSelection) {
        this.childMoveSelectorList = childMoveSelectorList;
        this.randomSelection = randomSelection;
        for (MoveSelector<Solution_> childMoveSelector : childMoveSelectorList) {
            phaseLifecycleSupport.addEventListener(childMoveSelector);
        }
        if (!randomSelection) {
            // Only the last childMoveSelector can be neverEnding
            if (!childMoveSelectorList.isEmpty()) {
                for (MoveSelector<Solution_> childMoveSelector : childMoveSelectorList.subList(0,
                        childMoveSelectorList.size() - 1)) {
                    if (childMoveSelector.isNeverEnding()) {
                        throw new IllegalStateException(
                                "The selector (%s)'s non-last childMoveSelector (%s) has neverEnding (%s) with randomSelection (%s)."
                                        .formatted(this, childMoveSelector, childMoveSelector.isNeverEnding(),
                                                randomSelection));
                    }
                }
            }
        }
    }

    public List<MoveSelector<Solution_>> getChildMoveSelectorList() {
        return childMoveSelectorList;
    }

    @Override
    public boolean supportsPhaseAndSolverCaching() {
        return true;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + childMoveSelectorList + ")";
    }

}
