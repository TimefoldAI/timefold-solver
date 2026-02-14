package ai.timefold.solver.core.api.score.director;

import ai.timefold.solver.core.api.domain.entity.PlanningId;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.change.ProblemChange;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The ScoreDirector holds the {@link PlanningSolution working solution}
 * and calculates the {@link Score} for it.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public interface ScoreDirector<Solution_> {

    /**
     * The {@link PlanningSolution} that is used to calculate the {@link Score}.
     * <p>
     * Because a {@link Score} is best calculated incrementally (by deltas),
     * the {@link ScoreDirector} needs to be notified when its {@link PlanningSolution working solution} changes.
     */
    Solution_ getWorkingSolution();

    void beforeVariableChanged(Object entity, String variableName);

    void afterVariableChanged(Object entity, String variableName);

    void beforeListVariableElementAssigned(Object entity, String variableName, Object element);

    void afterListVariableElementAssigned(Object entity, String variableName, Object element);

    void beforeListVariableElementUnassigned(Object entity, String variableName, Object element);

    void afterListVariableElementUnassigned(Object entity, String variableName, Object element);

    void beforeListVariableChanged(Object entity, String variableName, int fromIndex, int toIndex);

    void afterListVariableChanged(Object entity, String variableName, int fromIndex, int toIndex);

    void triggerVariableListeners();

    /**
     * Translates an entity or fact instance (often from another {@link Thread} or JVM)
     * to this {@link ScoreDirector}'s internal working instance.
     * Useful for move rebasing and in a {@link ProblemChange}.
     * <p>
     * Matching uses {@link PlanningId}.
     *
     * @return null if externalObject is null
     * @throws IllegalArgumentException if there is no workingObject for externalObject, if it cannot be looked up
     *         or if the externalObject's class is not supported
     * @throws IllegalStateException if it cannot be looked up
     * @param <E> the object type
     */
    <E> @Nullable E lookUpWorkingObject(@Nullable E externalObject);

    /**
     * As defined by {@link #lookUpWorkingObject(Object)},
     * but doesn't fail fast if no workingObject was ever added for the externalObject.
     * It's recommended to use {@link #lookUpWorkingObject(Object)} instead,
     * especially in move rebasing code.
     *
     * @return null if externalObject is null, or if there is no workingObject for externalObject
     * @throws IllegalArgumentException if it cannot be looked up or if the externalObject's class is not supported
     * @throws IllegalStateException if it cannot be looked up
     * @param <E> the object type
     */
    <E> @Nullable E lookUpWorkingObjectOrReturnNull(@Nullable E externalObject);

}
