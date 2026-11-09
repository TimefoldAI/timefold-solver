package ai.timefold.solver.core.preview.api.neighborhood.selection;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

import org.jspecify.annotations.NullMarked;

/**
 * Only the solver implements this interface.
 * The instance is valid only during the call that receives it; do not keep it.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see MoveSelectionStrategy#phaseStarted(PhaseContext)
 */
@NullMarked
public interface PhaseContext<Solution_> {

    /**
     * @return all move sources of the phase, in registration order; the strategy may keep this list for the phase run
     */
    List<MoveSource<Solution_>> moveSources();

    /**
     * @return the working solution, with its {@link PlanningSolution} score field up to date; read it, never change it
     */
    Solution_ solution();

}
