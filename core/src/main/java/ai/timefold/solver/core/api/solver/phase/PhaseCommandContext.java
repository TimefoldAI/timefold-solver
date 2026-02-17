package ai.timefold.solver.core.api.solver.phase;

import java.util.function.Function;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;

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
public interface PhaseCommandContext<Solution_>
        extends Lookup {

    /**
     * Returns the meta-model of the {@link #getWorkingSolution() working solution}.
     *
     * @return the meta-model of the working solution
     */
    PlanningSolutionMetaModel<Solution_> getSolutionMetaModel();

    /**
     * Returns the current working solution.
     * It must not be modified directly,
     * but only through {@link #executeAndCalculateScore(Move)} or {@link #executeTemporarily(Move, Function)}.
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
     * Executes the given move and updates the working solution
     * without recalculating the score for performance reasons.
     *
     * @param move the move to execute
     */
    void execute(Move<Solution_> move);

    /**
     * Executes the given move and updates the working solution,
     * and returns the new score of the working solution.
     *
     * @param move the move to execute
     * @return the new score of the working solution after executing the move
     */
    <Score_ extends Score<Score_>> Score_ executeAndCalculateScore(Move<Solution_> move);

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

    @Override
    <T> @Nullable T lookUpWorkingObject(@Nullable T problemFactOrPlanningEntity);

}
