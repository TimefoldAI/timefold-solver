package ai.timefold.solver.core.api.score.director;

import ai.timefold.solver.core.api.domain.lookup.LookUpStrategyType;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.change.ProblemChange;

/**
 * The ScoreDirector holds the {@link PlanningSolution working solution}
 * and calculates the {@link Score} for it.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public interface ScoreDirector<Solution_> {

    /**
     * The {@link PlanningSolution} that is used to calculate the {@link Score}.
     * <p>
     * Because a {@link Score} is best calculated incrementally (by deltas),
     * the {@link ScoreDirector} needs to be notified when its {@link PlanningSolution working solution} changes.
     *
     * @return never null
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
     * @deprecated Calling this method by user code is not recommended and will lead to unforeseen consequences.
     *             Use {@link ProblemChange} instead.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default void beforeEntityAdded(Object entity) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Calling this method by user code is not recommended and will lead to unforeseen consequences.
     *             Use {@link ProblemChange} instead.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default void afterEntityAdded(Object entity) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Calling this method by user code is not recommended and will lead to unforeseen consequences.
     *             Use {@link ProblemChange} instead.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default void beforeEntityRemoved(Object entity) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Calling this method by user code is not recommended and will lead to unforeseen consequences.
     *             Use {@link ProblemChange} instead.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default void afterEntityRemoved(Object entity) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Calling this method by user code is not recommended and will lead to unforeseen consequences.
     *             Use {@link ProblemChange} instead.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default void beforeProblemFactAdded(Object problemFact) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Calling this method by user code is not recommended and will lead to unforeseen consequences.
     *             Use {@link ProblemChange} instead.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default void afterProblemFactAdded(Object problemFact) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Calling this method by user code is not recommended and will lead to unforeseen consequences.
     *             Use {@link ProblemChange} instead.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default void beforeProblemPropertyChanged(Object problemFactOrEntity) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Calling this method by user code is not recommended and will lead to unforeseen consequences.
     *             Use {@link ProblemChange} instead.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default void afterProblemPropertyChanged(Object problemFactOrEntity) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Calling this method by user code is not recommended and will lead to unforeseen consequences.
     *             Use {@link ProblemChange} instead.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default void beforeProblemFactRemoved(Object problemFact) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Calling this method by user code is not recommended and will lead to unforeseen consequences.
     *             Use {@link ProblemChange} instead.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default void afterProblemFactRemoved(Object problemFact) {
        throw new UnsupportedOperationException();
    }

    /**
     * Translates an entity or fact instance (often from another {@link Thread} or JVM)
     * to this {@link ScoreDirector}'s internal working instance.
     * Useful for move rebasing and in a {@link ProblemChange}.
     * <p>
     * Matching is determined by the {@link LookUpStrategyType} on {@link PlanningSolution}.
     * Matching uses a {@link PlanningId} by default.
     *
     * @param externalObject sometimes null
     * @return null if externalObject is null
     * @throws IllegalArgumentException if there is no workingObject for externalObject, if it cannot be looked up
     *         or if the externalObject's class is not supported
     * @throws IllegalStateException if it cannot be looked up
     * @param <E> the object type
     */
    <E> E lookUpWorkingObject(E externalObject);

    /**
     * As defined by {@link #lookUpWorkingObject(Object)},
     * but doesn't fail fast if no workingObject was ever added for the externalObject.
     * It's recommended to use {@link #lookUpWorkingObject(Object)} instead,
     * especially in move rebasing code.
     *
     * @param externalObject sometimes null
     * @return null if externalObject is null or if there is no workingObject for externalObject
     * @throws IllegalArgumentException if it cannot be looked up or if the externalObject's class is not supported
     * @throws IllegalStateException if it cannot be looked up
     * @param <E> the object type
     */
    <E> E lookUpWorkingObjectOrReturnNull(E externalObject);

}
