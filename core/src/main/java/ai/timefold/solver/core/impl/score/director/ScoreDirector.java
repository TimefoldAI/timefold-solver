package ai.timefold.solver.core.impl.score.director;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;

import org.jspecify.annotations.NullMarked;

/**
 * The ScoreDirector holds the {@link PlanningSolution working solution}
 * and calculates the {@link Score} for it.
 * This is not public API and the users should refrain from using it or its implementations directly.
 * There are no backward compatibility guarantees for this API, and it may change without warning.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public interface ScoreDirector<Solution_>
        extends Lookup {

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

}
