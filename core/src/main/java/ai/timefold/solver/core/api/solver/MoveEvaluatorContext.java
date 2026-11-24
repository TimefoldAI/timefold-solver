package ai.timefold.solver.core.api.solver;

import java.util.function.Function;

import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;

/**
 * Gives access to the state of the working solution while a temporary move is applied.
 * Passed to the postprocessor of {@link MoveEvaluator#executeTemporaryMove(Move, Function)}.
 * <p>
 * The score and the score analysis are only calculated when requested,
 * so the postprocessor only pays for what it uses.
 * <p>
 * The context is only valid while the postprocessor runs;
 * the move is undone as soon as the postprocessor returns,
 * and any further use of the context throws {@link IllegalStateException}.
 * The documentation of each method describes what remains valid once the move is undone.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Score_> the actual score type
 */
@NullMarked
public interface MoveEvaluatorContext<Solution_, Score_ extends Score<Score_>> {

    /**
     * Returns the {@link MoveEvaluator#getWorkingSolution() working solution} with the move applied,
     * including updated shadow variables.
     * <p>
     * The {@link PlanningScore score} field of the returned solution
     * is not guaranteed to hold the score after the move; use {@link #getScore()} instead.
     * <p>
     * Do not modify the returned solution.
     * Its planning variables revert once the move is undone,
     * so do not keep references to its state beyond the postprocessor;
     * extract the information needed instead.
     *
     * @return the working solution with the move applied
     * @throws IllegalStateException if called after the postprocessor returned
     */
    Solution_ getWorkingSolution();

    /**
     * Returns the score of the working solution with the move applied.
     * The score is calculated on the first call;
     * later calls, and {@link #getScoreAnalysis(ScoreAnalysisFetchPolicy)}, reuse it.
     * <p>
     * The score is a value object; it remains valid after the move is undone.
     *
     * @return the score after the move
     * @throws IllegalStateException if called after the postprocessor returned
     */
    Score_ getScore();

    /**
     * Returns the analysis of the score of the working solution with the move applied.
     * This is slower than {@link #getScore()};
     * how much slower depends on {@code fetchPolicy}.
     * Only call it when the breakdown per constraint is needed.
     * <p>
     * The score and match counts of the analysis do not change when the move is undone,
     * and so the analysis may be returned from the postprocessor.
     * However, its constraint justifications and indictments may reference planning entities and problem facts
     * of the working solution, whose planning variables revert once the move is undone;
     * after that, they no longer show the state which the analysis describes.
     * Extract what is needed from them inside the postprocessor.
     *
     * @param fetchPolicy how much detail the analysis includes
     * @return the analysis of the score after the move
     * @throws IllegalStateException if called after the postprocessor returned
     */
    ScoreAnalysis<Score_> getScoreAnalysis(ScoreAnalysisFetchPolicy fetchPolicy);

    /**
     * As defined by {@link #getScoreAnalysis(ScoreAnalysisFetchPolicy)},
     * using {@link ScoreAnalysisFetchPolicy#FETCH_JUSTIFICATIONS},
     * the same default as {@link SolutionManager#analyze(Object)}.
     *
     * @return the analysis of the score after the move
     * @throws IllegalStateException if called after the postprocessor returned
     */
    default ScoreAnalysis<Score_> getScoreAnalysis() {
        return getScoreAnalysis(ScoreAnalysisFetchPolicy.FETCH_JUSTIFICATIONS);
    }

}
