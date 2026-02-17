package ai.timefold.solver.core.api.solver.phase;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;

/**
 * Runs a custom algorithm as a {@link Phase} of the {@link Solver} that changes the planning variables.
 * To change problem facts and to add or remove entities, use {@link Solver#addProblemChange(ProblemChange)} instead.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public interface PhaseCommand<Solution_> {

    /**
     * Changes the current {@link PhaseCommandContext#getWorkingSolution() working solution}.
     * The solver is notified of the changes through {@link PhaseCommandContext},
     * specifically through {@link PhaseCommandContext#executeAndCalculateScore(Move)}.
     * Any other modifications to the working solution are strictly forbidden
     * and will likely cause the solver to be in an inconsistent state and throw an exception later on.
     * <p>
     * Don't forget to check {@link PhaseCommandContext#isPhaseTerminated() termination status} frequently
     * to allow the solver to gracefully terminate when necessary.
     *
     * @param context the context of the command, providing access to the working solution and allowing move execution
     */
    void changeWorkingSolution(PhaseCommandContext<Solution_> context);

}
