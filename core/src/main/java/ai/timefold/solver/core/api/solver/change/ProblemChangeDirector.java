package ai.timefold.solver.core.api.solver.change;

import java.util.Optional;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.LookUpStrategyType;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Allows external changes to the {@link PlanningSolution working solution}. If the changes are not applied through
 * the ProblemChangeDirector,
 * {@link ai.timefold.solver.core.api.domain.variable.VariableListener both internal and custom variable listeners} are
 * never notified about them, resulting to inconsistencies in the {@link PlanningSolution working solution}.
 * Should be used only from a {@link ProblemChange} implementation.
 * To see an example implementation, please refer to the {@link ProblemChange} Javadoc.
 */
public interface ProblemChangeDirector {

    /**
     * Add a new {@link PlanningEntity} instance into the {@link PlanningSolution working solution}.
     *
     * @param entity the {@link PlanningEntity} instance
     * @param entityConsumer adds the entity to the {@link PlanningSolution working solution}
     * @param <Entity> the planning entity object type
     */
    <Entity> void addEntity(@NonNull Entity entity, @NonNull Consumer<Entity> entityConsumer);

    /**
     * Remove an existing {@link PlanningEntity} instance from the {@link PlanningSolution working solution}.
     * Translates the entity to a working planning entity by performing a lookup as defined by
     * {@link #lookUpWorkingObjectOrFail(Object)}.
     *
     * @param entity the {@link PlanningEntity} instance
     * @param entityConsumer removes the working entity from the {@link PlanningSolution working solution}
     * @param <Entity> the planning entity object type
     */
    <Entity> void removeEntity(@NonNull Entity entity, @NonNull Consumer<Entity> entityConsumer);

    /**
     * Change a {@link PlanningVariable} value of a {@link PlanningEntity}. Translates the entity to a working
     * planning entity by performing a lookup as defined by {@link #lookUpWorkingObjectOrFail(Object)}.
     *
     * @param entity the {@link PlanningEntity} instance
     * @param variableName name of the {@link PlanningVariable}
     * @param entityConsumer updates the value of the {@link PlanningVariable} inside the {@link PlanningEntity}
     * @param <Entity> the planning entity object type
     */
    <Entity> void changeVariable(@NonNull Entity entity, @NonNull String variableName,
            @NonNull Consumer<Entity> entityConsumer);

    /**
     * Add a new problem fact into the {@link PlanningSolution working solution}.
     *
     * @param problemFact the problem fact instance
     * @param problemFactConsumer removes the working problem fact from the
     *        {@link PlanningSolution working solution}
     * @param <ProblemFact> the problem fact object type
     */
    <ProblemFact> void addProblemFact(@NonNull ProblemFact problemFact, @NonNull Consumer<ProblemFact> problemFactConsumer);

    /**
     * Remove an existing problem fact from the {@link PlanningSolution working solution}. Translates the problem fact
     * to a working problem fact by performing a lookup as defined by {@link #lookUpWorkingObjectOrFail(Object)}.
     *
     * @param problemFact the problem fact instance
     * @param problemFactConsumer removes the working problem fact from the
     *        {@link PlanningSolution working solution}
     * @param <ProblemFact> the problem fact object type
     */
    <ProblemFact> void removeProblemFact(@NonNull ProblemFact problemFact, @NonNull Consumer<ProblemFact> problemFactConsumer);

    /**
     * Change a property of either a {@link PlanningEntity} or a problem fact. Translates the entity or the problem fact
     * to its {@link PlanningSolution working solution} counterpart by performing a lookup as defined by
     * {@link #lookUpWorkingObjectOrFail(Object)}.
     *
     * @param problemFactOrEntity the {@link PlanningEntity} or the problem fact instance
     * @param problemFactOrEntityConsumer updates the property of the {@link PlanningEntity}
     *        or the problem fact
     * @param <EntityOrProblemFact> the planning entity or problem fact object type
     */
    <EntityOrProblemFact> void changeProblemProperty(@NonNull EntityOrProblemFact problemFactOrEntity,
            @NonNull Consumer<EntityOrProblemFact> problemFactOrEntityConsumer);

    /**
     * Translate an entity or fact instance (often from another {@link Thread} or JVM)
     * to this {@link ProblemChangeDirector}'s internal working instance.
     * <p>
     * Matching is determined by the {@link LookUpStrategyType} on {@link PlanningSolution}.
     * Matching uses a {@link PlanningId} by default.
     *
     * @return null if externalObject is null
     * @throws IllegalArgumentException if there is no workingObject for externalObject, if it cannot be looked up
     *         or if the externalObject's class is not supported
     * @throws IllegalStateException if it cannot be looked up
     * @param <EntityOrProblemFact> the object type
     */
    <EntityOrProblemFact> @Nullable EntityOrProblemFact lookUpWorkingObjectOrFail(@Nullable EntityOrProblemFact externalObject);

    /**
     * As defined by {@link #lookUpWorkingObjectOrFail(Object)},
     * but doesn't fail fast if no workingObject was ever added for the externalObject.
     * It's recommended to use {@link #lookUpWorkingObjectOrFail(Object)} instead.
     *
     * @return {@link Optional#empty()} if externalObject is null or if there is no workingObject for externalObject
     * @throws IllegalArgumentException if it cannot be looked up or if the externalObject's class is not supported
     * @throws IllegalStateException if it cannot be looked up
     * @param <EntityOrProblemFact> the object type
     */
    <EntityOrProblemFact> Optional<EntityOrProblemFact> lookUpWorkingObject(@Nullable EntityOrProblemFact externalObject);

    /**
     * Calls variable listeners on the external changes submitted so far.
     *
     * <p>
     * This happens automatically after the entire {@link ProblemChange} has been processed,
     * but this method allows the user to specifically request it in the middle of the {@link ProblemChange}.
     */
    void updateShadowVariables();

}
