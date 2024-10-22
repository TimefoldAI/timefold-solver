package ai.timefold.solver.core.api.score.calculator;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.score.Score;

import org.jspecify.annotations.NonNull;

/**
 * Used for incremental java {@link Score} calculation.
 * This is much faster than {@link EasyScoreCalculator} but requires much more code to implement too.
 * <p>
 * Any implementation is naturally stateful.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Score_> the score type to go with the solution
 */
public interface IncrementalScoreCalculator<Solution_, Score_ extends Score<Score_>> {

    /**
     * There are no {@link #beforeEntityAdded(Object)} and {@link #afterEntityAdded(Object)} calls
     * for entities that are already present in the workingSolution.
     */
    void resetWorkingSolution(@NonNull Solution_ workingSolution);

    /**
     * @param entity an instance of a {@link PlanningEntity} class
     */
    void beforeEntityAdded(@NonNull Object entity);

    /**
     * @param entity an instance of a {@link PlanningEntity} class
     */
    void afterEntityAdded(@NonNull Object entity);

    /**
     * @param entity an instance of a {@link PlanningEntity} class
     * @param variableName either a genuine or shadow {@link PlanningVariable}
     */
    void beforeVariableChanged(@NonNull Object entity, @NonNull String variableName);

    /**
     * @param entity an instance of a {@link PlanningEntity} class
     * @param variableName either a genuine or shadow {@link PlanningVariable}
     */
    void afterVariableChanged(@NonNull Object entity, @NonNull String variableName);

    default void beforeListVariableElementAssigned(@NonNull String variableName, @NonNull Object element) {
    }

    default void afterListVariableElementAssigned(@NonNull String variableName, @NonNull Object element) {
    }

    default void beforeListVariableElementUnassigned(@NonNull String variableName, @NonNull Object element) {
    }

    default void afterListVariableElementUnassigned(@NonNull String variableName, @NonNull Object element) {
    }

    default void beforeListVariableChanged(@NonNull Object entity, @NonNull String variableName, int fromIndex, int toIndex) {
    }

    default void afterListVariableChanged(@NonNull Object entity, @NonNull String variableName, int fromIndex, int toIndex) {
    }

    /**
     * @param entity an instance of a {@link PlanningEntity} class
     */
    void beforeEntityRemoved(@NonNull Object entity);

    /**
     * @param entity an instance of a {@link PlanningEntity} class
     */
    void afterEntityRemoved(@NonNull Object entity);

    /**
     * This method is only called if the {@link Score} cannot be predicted.
     * The {@link Score} can be predicted for example after an undo move.
     */
    @NonNull
    Score_ calculateScore();

}
