package ai.timefold.solver.core.api.solver.phase;

import java.util.function.Function;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The context of a command that is executed during a custom phase.
 * It provides access to the working solution and allows executing moves.
 *
 * @param <Solution_> the type of the solution
 * @see PhaseCommand
 */
@NullMarked
public interface PhaseCommandContext<Solution_> {

    /**
     * Returns the meta-model of the {@link #getWorkingSolution() working solution}.
     *
     * @return the meta-model of the working solution
     */
    PlanningSolutionMetaModel<Solution_> getSolutionMetaModel();

    /**
     * Returns the current working solution.
     * It must not be modified directly,
     * but only through {@link #execute(Move)} or {@link #executeTemporarily(Move, Function)}.
     * Direct modifications will cause the solver to be in an inconsistent state and likely throw an exception later on.
     *
     * @return the current working solution
     */
    Solution_ getWorkingSolution();

    /**
     * Long-running command implementations should check this periodically and terminate early if it returns true.
     * Otherwise the terminations configured by the user will have no effect,
     * as the solver can only terminate itself when a command has ended.
     *
     * @return true if the solver has requested the phase to terminate,
     *         for example because the time limit has been reached.
     */
    boolean isPhaseTerminated();

    /**
     * As defined by {@link #execute(Move, boolean)},
     * but with the guarantee of a fresh score.
     */
    default void execute(Move<Solution_> move) {
        execute(move, true);
    }

    /**
     * Executes the given move and updates the working solution,
     * optionally without recalculating the score for performance reasons.
     *
     * @param move the move to execute
     * @param guaranteeFreshScore if true, the score of {@link #getWorkingSolution()} after this method returns
     *        is guaranteed to be up-to-date;
     *        otherwise it may be stale as the solver will skip recalculating it for performance reasons.
     */
    void execute(Move<Solution_> move, boolean guaranteeFreshScore);

    /**
     * As defined by {@link #executeTemporarily(Move, Function, boolean)},
     * with the guarantee of a fresh score.
     */
    default <Result_> @Nullable Result_ executeTemporarily(Move<Solution_> move,
            Function<Solution_, @Nullable Result_> temporarySolutionConsumer) {
        return executeTemporarily(move, temporarySolutionConsumer, true);
    }

    /**
     * Executes the given move temporarily and returns the result of the given consumer.
     * The working solution is reverted to its original state after the consumer has been executed,
     * optionally without recalculating the score for performance reasons.
     *
     * @param move the move to execute temporarily
     * @param temporarySolutionConsumer the consumer to execute with the temporarily modified solution;
     *        this solution must not be modified any further.
     * @param guaranteeFreshScore if true, the score of {@link #getWorkingSolution()} after this method returns
     *        is guaranteed to be up-to-date;
     *        otherwise it may be stale as the solver will skip recalculating it for performance reasons.
     * @return the result of the consumer
     */
    <Result_> @Nullable Result_ executeTemporarily(Move<Solution_> move,
            Function<Solution_, @Nullable Result_> temporarySolutionConsumer, boolean guaranteeFreshScore);

    /**
     * As defined by {@link Rebaser#rebase(Object)}, but for the working solution of this context.
     */
    <T> @Nullable T lookupWorkingObject(@Nullable T original);

}
