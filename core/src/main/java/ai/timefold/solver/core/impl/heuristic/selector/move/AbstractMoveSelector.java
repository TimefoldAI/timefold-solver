package ai.timefold.solver.core.impl.heuristic.selector.move;

import ai.timefold.solver.core.impl.heuristic.selector.AbstractSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;

/**
 * Abstract superclass for {@link MoveSelector}.
 *
 * @see MoveSelector
 */
public abstract class AbstractMoveSelector<Solution_> extends AbstractSelector<Solution_>
        implements MoveSelector<Solution_> {

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        phaseScope.setMoveSelectorSize(getSize());
    }
}
