package ai.timefold.solver.core.api.solver;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.calculator.AnalyzableIncrementalScoreCalculator;
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
 * <li>Permanently, using {@link #executeMove(Move)}.
 * The working solution keeps the changes, and subsequent moves build on top of them.
 * Call {@link #calculateScore()} afterwards if the resulting score is needed.</li>
 * <li>Temporarily, using {@link #executeTemporaryMove(Move)}.
 * The move is executed, its score is calculated, and the move is then undone,
 * returning the working solution to the state it was in before the move.
 * Use this to compare several alternative moves against the same starting point.
 * To get more than the score, such as the {@link ScoreAnalysis}
 * or information from the working solution while the move is still applied,
 * use {@link #executeTemporaryMove(Move, Function)}, which exposes a {@link MoveEvaluatorContext}.</li>
 * </ul>
 * <p>
 * If executing a move, its postprocessor, or a score calculation fails,
 * the working solution may be left half-changed or the score calculation only partly updated.
 * The evaluator then can no longer be used, and every further call throws {@link IllegalStateException};
 * create a new evaluator from the original solution instead.
 * The only exception is a temporary move which makes the working solution structurally flawed:
 * it is undone, and the evaluator remains usable; see {@link #executeTemporaryMove(Move, Function)}.
 * <p>
 * The evaluator needs a score calculator that supports constraint matching, such as Constraint Streams,
 * or an Incremental score calculator which implements {@link AnalyzableIncrementalScoreCalculator},
 * because {@link MoveEvaluatorContext#getScoreAnalysis()} relies on it.
 * <p>
 * The evaluator holds resources and must be {@link #close() closed} when no longer needed,
 * preferably using a try-with-resources block.
 * <p>
 * This class is not thread-safe.
 * <p>
 * Note: The move evaluator is exclusive to Timefold Solver Enterprise Edition.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Score_> the actual score type
 */
@NullMarked
public interface MoveEvaluator<Solution_, Score_ extends Score<Score_>> extends Lookup, AutoCloseable {

    /**
     * Describes the structure of the planning solution,
     * useful for building {@link Move}s that refer to planning variables generically.
     *
     * @return the meta model of the planning solution
     * @throws IllegalStateException if this evaluator has been {@link #close() closed},
     *         or can no longer be used because an earlier move or score calculation failed
     */
    PlanningSolutionMetaModel<Solution_> getSolutionMetaModel();

    /**
     * Returns the solution on which moves are executed.
     * It reflects all moves executed permanently so far;
     * temporary moves are no longer visible once their execution method returns.
     * <p>
     * Do not modify the returned solution directly; only change it through {@link Move}s.
     *
     * @return the working solution
     * @throws IllegalStateException if this evaluator has been {@link #close() closed},
     *         or can no longer be used because an earlier move or score calculation failed
     */
    Solution_ getWorkingSolution();

    /**
     * Calculates the score of the {@link #getWorkingSolution() working solution} in its current state,
     * which reflects all moves executed permanently so far.
     * Does not execute any move and does not change any planning variable;
     * only the {@link PlanningScore score} field is updated.
     *
     * @return the score of the working solution
     * @throws IllegalStateException if this evaluator has been {@link #close() closed},
     *         or can no longer be used because an earlier move or score calculation failed
     */
    Score_ calculateScore();

    /**
     * Executes the move on the working solution and keeps its changes.
     * Shadow variables are updated, but the score is not calculated.
     * This is the fastest way of executing a move.
     * To obtain the score after the move, call {@link #calculateScore()}.
     * <p>
     * A permanent move cannot be undone.
     * If this method throws for any reason, including a structurally flawed working solution,
     * the evaluator can no longer be used.
     * If the move may make the working solution structurally flawed,
     * execute it with {@link #executeTemporaryMove(Move)} first:
     * a temporary move is undone when it does, and the evaluator remains usable.
     *
     * @param move must only reference instances of the working solution
     * @throws IllegalArgumentException if the move makes the working solution structurally flawed,
     *         for example by creating a loop in shadow variables;
     *         the evaluator can no longer be used afterwards
     * @throws IllegalStateException if this evaluator has been {@link #close() closed},
     *         or can no longer be used because an earlier move or score calculation failed
     */
    void executeMove(Move<Solution_> move);

    /**
     * Executes the move on the working solution, calculates the resulting score, and then undoes the move.
     * When this method returns, the working solution is in the same state as before it was called.
     * <p>
     * Shorthand for {@link #executeTemporaryMove(Move, Function)}
     * with {@link MoveEvaluatorContext#getScore()} as the postprocessor.
     *
     * @param move must only reference instances of the working solution
     * @return the score the working solution would have if the move was executed
     * @throws IllegalArgumentException if the move makes the working solution structurally flawed,
     *         for example by creating a loop in shadow variables
     * @throws IllegalStateException if this evaluator has been {@link #close() closed},
     *         or can no longer be used because an earlier move or score calculation failed
     */
    default Score_ executeTemporaryMove(Move<Solution_> move) {
        return Objects.requireNonNull(executeTemporaryMove(move, MoveEvaluatorContext::getScore),
                () -> "Impossible state: the score after the move (%s) is null.".formatted(move));
    }

    /**
     * Executes the move on the working solution, calls {@code postprocessor} while the move is still applied,
     * and then undoes the move.
     * When this method returns, the working solution is in the same state as before it was called.
     * <p>
     * The postprocessor receives a {@link MoveEvaluatorContext},
     * through which it reads the state after the move:
     * the working solution, its score, and its score analysis.
     * The score and the score analysis are only calculated when the postprocessor asks for them;
     * for example, a postprocessor which does not request the score analysis does not pay for building it.
     * Constraint matches are always tracked, so that the score analysis is available when requested.
     * <p>
     * The move is undone after the postprocessor returns. Therefore:
     * <ul>
     * <li>The postprocessor must not modify the working solution, nor execute other moves on this evaluator.</li>
     * <li>The context must not be used after the postprocessor returns.</li>
     * <li>The result must not depend on the temporary state of the working solution,
     * such as the planning variable values of entities which the move changed,
     * because that state is reverted once the move is undone.
     * Extract the information needed into the result instead.
     * The score obtained from the context, and the score and match counts of the score analysis,
     * do not change when the move is undone, and so they may be returned.
     * The constraint justifications and indictments of the score analysis, however,
     * reference entities and facts of the working solution, whose planning variables revert;
     * see {@link MoveEvaluatorContext#getScoreAnalysis(ScoreAnalysisFetchPolicy)}.</li>
     * </ul>
     * <p>
     * When an exception is thrown:
     * <ul>
     * <li>If the move makes the working solution structurally flawed,
     * the move is undone, the postprocessor is not called, and {@link IllegalArgumentException} is thrown.
     * The evaluator remains usable, unless undoing the move fails as well.</li>
     * <li>If the move itself throws, the postprocessor throws, or undoing the move fails,
     * the exception is rethrown, and the evaluator can no longer be used.</li>
     * </ul>
     *
     * @param move must only reference instances of the working solution
     * @param postprocessor called at most once, with the context of the working solution after the move;
     *        not called if the move throws or makes the working solution structurally flawed
     * @param <Result_> the type of the result produced by the postprocessor
     * @return the result of {@code postprocessor}
     * @throws IllegalArgumentException if the move makes the working solution structurally flawed,
     *         for example by creating a loop in shadow variables
     * @throws IllegalStateException if this evaluator has been {@link #close() closed},
     *         or can no longer be used because an earlier move or score calculation failed
     */
    <Result_> @Nullable Result_ executeTemporaryMove(Move<Solution_> move,
            Function<MoveEvaluatorContext<Solution_, Score_>, @Nullable Result_> postprocessor);

    /**
     * Translates an entity or fact instance, such as one from the solution given to
     * {@link SolutionManager#moveEvaluator(Object)},
     * to its counterpart in the {@link #getWorkingSolution() working solution}.
     * <p>
     * Otherwise as defined by {@link Lookup#lookUpWorkingObject(Object)}.
     *
     * @throws IllegalStateException if this evaluator has been {@link #close() closed},
     *         or can no longer be used because an earlier move or score calculation failed
     */
    @Override
    <T> @Nullable T lookUpWorkingObject(@Nullable T problemFactOrPlanningEntity);

    /**
     * Releases all resources held by this evaluator.
     * The evaluator must not be used after it has been closed.
     * Calling this method more than once has no effect.
     */
    @Override
    void close();

}
