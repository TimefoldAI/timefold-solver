package ai.timefold.solver.core.preview.api.neighborhood.selection;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

import org.jspecify.annotations.NullMarked;

/**
 * Only the solver implements this interface.
 * The instance is valid only during the call that receives it; do not keep it.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see MoveSelectionStrategy#stepStarted(StepContext)
 */
@NullMarked
public interface StepContext<Solution_> {

    /**
     * @return the working solution, with its {@link PlanningSolution} score field up to date; read it, never change it
     */
    Solution_ solution();

}
