package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;

/**
 * Abstract superclass for every generic {@link MoveSelector}.
 *
 * @see MoveSelector
 */
public abstract class GenericMoveSelector<Solution_> extends AbstractMoveSelector<Solution_> {
    private boolean assertMoveDoable = true;

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);

    }

    public boolean isAssertMoveDoable() {
        return assertMoveDoable;
    }
}
