package ai.timefold.solver.core.api.score.calculator;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;

import org.jspecify.annotations.NullMarked;

/**
 * Used for incremental java {@link Score} calculation with support for {@link ScoreAnalysis}
 * Any implementation is naturally stateful.
 * <p>
 * Note: Both incremental score calculation and score analysis are exclusive to Timefold Solver Enterprise Edition.
 * They are not available in the open-source version of Timefold Solver,
 * and attempts to use it without a valid license will throw exceptions at runtime.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Score_> the score type to go with the solution
 */
@NullMarked
public interface AnalyzableIncrementalScoreCalculator<Solution_, Score_ extends Score<Score_>>
        extends IncrementalScoreCalculator<Solution_, Score_> {

    /**
     * Tells this implementation whether constraint matching should be enabled or not.
     * Will be called by the solver before the first call to {@link #resetWorkingSolution}
     * and not again.
     *
     * @param constraintMatchRegistry use for registering constraint matches
     */
    void enableConstraintMatch(ConstraintMatchRegistry<Score_> constraintMatchRegistry);

}
