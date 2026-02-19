package ai.timefold.solver.core.impl.heuristic.selector.move;

import ai.timefold.solver.core.impl.heuristic.selector.IterableSelector;
import ai.timefold.solver.core.preview.api.move.Move;

/**
 * Generates {@link Move}s.
 *
 * @see AbstractMoveSelector
 */
public interface MoveSelector<Solution_> extends IterableSelector<Solution_, Move<Solution_>> {

    default boolean supportsPhaseAndSolverCaching() {
        return false;
    }

}
