package ai.timefold.solver.core.api.score.calculator;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;

import org.jspecify.annotations.NullMarked;

/**
 * Used for incremental java {@link Score} calculation.
 * This is much faster than {@link EasyScoreCalculator} but requires much more code to implement too.
 * <p>
 * Any implementation is naturally stateful.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Score_> the score type to go with the solution
 * @see AnalyzableIncrementalScoreCalculator See incremental calculator with support for {@link ScoreAnalysis}.
 */
@NullMarked
public interface IncrementalScoreCalculator<Solution_, Score_ extends Score<Score_>> {

    /**
     * Resets the internal caches and score to match the given working solution.
     * It is recommended to build the internal caches lazily as the before/after events come in,
     * as this method may be called several times in a row with the same working solution,
     * and building the internal caches eagerly can be expensive.
     *
     * @param workingSolution the working solution to operate on
     */
    void resetWorkingSolution(Solution_ workingSolution);

    /**
     * @param entity an instance of a {@link PlanningEntity} class
     * @param variableName either a genuine or shadow {@link PlanningVariable}
     */
    void beforeVariableChanged(Object entity, String variableName);

    /**
     * @param entity an instance of a {@link PlanningEntity} class
     * @param variableName either a genuine or shadow {@link PlanningVariable}
     */
    void afterVariableChanged(Object entity, String variableName);

    default void beforeListVariableElementAssigned(String variableName, Object element) {
    }

    default void afterListVariableElementAssigned(String variableName, Object element) {
    }

    default void beforeListVariableElementUnassigned(String variableName, Object element) {
    }

    default void afterListVariableElementUnassigned(String variableName, Object element) {
    }

    default void beforeListVariableChanged(Object entity, String variableName, int fromIndex, int toIndex) {
    }

    default void afterListVariableChanged(Object entity, String variableName, int fromIndex, int toIndex) {
    }

    /**
     * This method is only called if the {@link Score} cannot be predicted.
     * The {@link Score} can be predicted for example after an undo move.
     */
    Score_ calculateScore();

}
