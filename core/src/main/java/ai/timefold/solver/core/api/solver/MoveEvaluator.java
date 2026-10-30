package ai.timefold.solver.core.api.solver;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Executes {@link Move}s on a working solution and evaluates their impact on the {@link Score},
 * without running the solver.
 * <p>
 * To create a {@link MoveEvaluator} instance, use {@link SolutionManager#moveEvaluator(Object)}.
 * The evaluator works on its own working solution, which is based on the solution it was created for.
 * <p>
 * Moves can be executed in two ways:
 * <ul>
 * <li>Permanently, using {@link #executeMove(Move)}, {@link #executeMoveAndCalculate(Move)}
 * or {@link #executeMoveAndAnalyze(Move, ScoreAnalysisFetchPolicy)}.
 * The working solution keeps the changes, and subsequent moves build on top of them.</li>
 * <li>Temporarily, using {@link #executeTemporaryMove(Move)}
 * or {@link #executeTemporaryMoveAndAnalyze(Move, ScoreAnalysisFetchPolicy)}.
 * The move is executed, its score is calculated, and the move is then undone,
 * returning the working solution to the state it was in before the move.
 * Use this to compare several alternative moves against the same starting point.
 * To inspect the working solution while the move is still applied,
 * use {@link #executeTemporaryMove(Move, Function)}
 * or {@link #executeTemporaryMoveAndAnalyze(Move, ScoreAnalysisFetchPolicy, BiFunction)}.</li>
 * </ul>
 * <p>
 * The evaluator holds resources and must be {@link #close() closed} when no longer needed,
 * preferably using a try-with-resources block.
 * <p>
 * This class is not thread-safe.
 * <p>
 * Note: The move evaluator is exclusive to Timefold Solver Enterprise Edition.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public interface MoveEvaluator<Solution_> extends Lookup, AutoCloseable {

    /**
     * Describes the structure of the planning solution,
     * useful for building {@link Move}s that refer to planning variables generically.
     */
    PlanningSolutionMetaModel<Solution_> getSolutionMetaModel();

    /**
     * Returns the solution on which moves are executed.
     * It reflects all moves executed permanently so far;
     * temporary moves are no longer visible once their execution method returns.
     * <p>
     * Do not modify the returned solution directly; only change it through {@link Move}s.
     */
    Solution_ getWorkingSolution();

    /**
     * Calculates the score of the {@link #getWorkingSolution() working solution} in its current state,
     * which reflects all moves executed permanently so far.
     * Does not execute any move and does not change the working solution.
     *
     * @param <Score_> the actual score type
     * @return the score of the working solution
     */
    <Score_ extends Score<Score_>> Score_ calculateScore();

    /**
     * Executes the move on the working solution and keeps its changes.
     * Shadow variables are updated, but the score is not calculated.
     * Use this when the score is not needed, as it is the fastest of the execution methods.
     *
     * @param move must only reference instances of the working solution
     */
    void executeMove(Move<Solution_> move);

    /**
     * As defined by {@link #executeMove(Move)},
     * then calculates the score of the working solution after the move,
     * as defined by {@link #calculateScore()}.
     *
     * @param move must only reference instances of the working solution
     * @param <Score_> the actual score type
     * @return the score of the working solution after the move
     */
    default <Score_ extends Score<Score_>> Score_ executeMoveAndCalculate(Move<Solution_> move) {
        executeMove(move);
        return calculateScore();
    }

    /**
     * As defined by {@link #executeMove(Move)},
     * then analyzes the score of the working solution after the move.
     * This is slower than {@link #executeMoveAndCalculate(Move)};
     * how much slower depends on {@code fetchPolicy}.
     *
     * @param move must only reference instances of the working solution
     * @param fetchPolicy how much detail the analysis includes
     * @param <Score_> the actual score type
     * @return the analysis of the score of the working solution after the move
     */
    <Score_ extends Score<Score_>> ScoreAnalysis<Score_> executeMoveAndAnalyze(Move<Solution_> move,
            ScoreAnalysisFetchPolicy fetchPolicy);

    /**
     * Executes the move on the working solution, calculates the resulting score, and then undoes the move.
     * When this method returns, the working solution is in the same state as before it was called.
     *
     * @param move must only reference instances of the working solution
     * @param <Score_> the actual score type
     * @return the score the working solution would have if the move was executed
     */
    <Score_ extends Score<Score_>> Score_ executeTemporaryMove(Move<Solution_> move);

    /**
     * As defined by {@link #executeTemporaryMove(Move)},
     * but instead of returning the score, passes the working solution to {@code postprocessor}
     * while the move is still applied, and returns whatever the postprocessor returns.
     * The working solution passed to the postprocessor has its score set to the score after the move.
     * <p>
     * The move is undone after the postprocessor returns, so the postprocessor must not modify the working solution,
     * and its result must not reference the temporary state of the working solution,
     * such as entities whose planning variables the move changed.
     * Extract the information needed instead.
     *
     * @param move must only reference instances of the working solution
     * @param postprocessor called once, with the working solution after the move
     * @param <Result_> the type of the result produced by the postprocessor
     * @return the result of {@code postprocessor}
     */
    <Result_> @Nullable Result_ executeTemporaryMove(Move<Solution_> move,
            Function<Solution_, @Nullable Result_> postprocessor);

    /**
     * As defined by {@link #executeTemporaryMove(Move)},
     * but returns the analysis of the score instead of just the score.
     * This is slower than {@link #executeTemporaryMove(Move)};
     * how much slower depends on {@code fetchPolicy}.
     * <p>
     * The analysis is computed before the move is undone,
     * but it does not reference the temporary state of the working solution,
     * so it remains valid after this method returns.
     *
     * @param move must only reference instances of the working solution
     * @param fetchPolicy how much detail the analysis includes
     * @param <Score_> the actual score type
     * @return the analysis of the score the working solution would have if the move was executed
     */
    @SuppressWarnings("unchecked")
    default <Score_ extends Score<Score_>> ScoreAnalysis<Score_> executeTemporaryMoveAndAnalyze(Move<Solution_> move,
            ScoreAnalysisFetchPolicy fetchPolicy) {
        return Objects.requireNonNull(executeTemporaryMoveAndAnalyze(move, fetchPolicy,
                (solution, scoreScoreAnalysis) -> (ScoreAnalysis<Score_>) scoreScoreAnalysis));
    }

    /**
     * As defined by {@link #executeTemporaryMove(Move, Function)},
     * but also passes the analysis of the score after the move to {@code postprocessor}.
     * This is slower than {@link #executeTemporaryMove(Move, Function)};
     * how much slower depends on {@code fetchPolicy}.
     * <p>
     * Unlike the working solution, the analysis does not reference the temporary state of the working solution,
     * so the postprocessor may return it, or parts of it.
     *
     * @param move must only reference instances of the working solution
     * @param fetchPolicy how much detail the analysis includes
     * @param postprocessor called once, with the working solution after the move
     *        and the analysis of its score
     * @param <Result_> the type of the result produced by the postprocessor
     * @param <Score_> the actual score type
     * @return the result of {@code postprocessor}
     */
    <Result_, Score_ extends Score<Score_>> @Nullable Result_ executeTemporaryMoveAndAnalyze(Move<Solution_> move,
            ScoreAnalysisFetchPolicy fetchPolicy,
            BiFunction<Solution_, ScoreAnalysis<Score_>, @Nullable Result_> postprocessor);

    /**
     * Translates an entity or fact instance, such as one from the solution given to
     * {@link SolutionManager#moveEvaluator(Object)},
     * to its counterpart in the {@link #getWorkingSolution() working solution}.
     * <p>
     * Otherwise as defined by {@link Lookup#lookUpWorkingObject(Object)}.
     */
    @Override
    <T> @Nullable T lookUpWorkingObject(@Nullable T problemFactOrPlanningEntity);

    /**
     * Releases all resources held by this evaluator.
     * The evaluator must not be used after it has been closed.
     */
    @Override
    void close();

}
