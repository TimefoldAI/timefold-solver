package ai.timefold.solver.core.api.solver.change;

import java.util.Optional;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningId;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Allows external changes to the {@link PlanningSolution working solution}. If the changes are not applied through
 * the ProblemChangeDirector,
 * {@link ShadowVariable both internal and custom shadow variables} are
 * never notified about them, resulting to inconsistencies in the {@link PlanningSolution working solution}.
 * Should be used only from a {@link ProblemChange} implementation.
 * To see an example implementation, please refer to the {@link ProblemChange} Javadoc.
 */
@NullMarked
public interface ProblemChangeDirector {

    /**
     * Add a new {@link PlanningEntity} instance into the {@link PlanningSolution working solution}.
     *
     * @param entity the {@link PlanningEntity} instance
     * @param entityConsumer adds the entity to the {@link PlanningSolution working solution}
     * @param <Entity> the planning entity object type
     */
    <Entity> void addEntity(Entity entity, Consumer<Entity> entityConsumer);

    /**
     * Remove an existing {@link PlanningEntity} instance from the {@link PlanningSolution working solution}.
     * Translates the entity to a working planning entity by performing a lookup as defined by
     * {@link #lookUpWorkingObjectOrFail(Object)}.
     *
     * @param entity the {@link PlanningEntity} instance
     * @param entityConsumer removes the working entity from the {@link PlanningSolution working solution}
     * @param <Entity> the planning entity object type
     */
    <Entity> void removeEntity(Entity entity, Consumer<Entity> entityConsumer);

    /**
     * Change a {@link PlanningVariable} value of a {@link PlanningEntity}. Translates the entity to a working
     * planning entity by performing a lookup as defined by {@link #lookUpWorkingObjectOrFail(Object)}.
     *
     * @param entity the {@link PlanningEntity} instance
     * @param variableName name of the {@link PlanningVariable}
     * @param entityConsumer updates the value of the {@link PlanningVariable} inside the {@link PlanningEntity}
     * @param <Entity> the planning entity object type
     */
    <Entity> void changeVariable(Entity entity, String variableName, Consumer<Entity> entityConsumer);

    /**
     * Add a new problem fact into the {@link PlanningSolution working solution}.
     *
     * @param problemFact the problem fact instance
     * @param problemFactConsumer removes the working problem fact from the
     *        {@link PlanningSolution working solution}
     * @param <ProblemFact> the problem fact object type
     */
    <ProblemFact> void addProblemFact(ProblemFact problemFact, Consumer<ProblemFact> problemFactConsumer);

    /**
     * Remove an existing problem fact from the {@link PlanningSolution working solution}. Translates the problem fact
     * to a working problem fact by performing a lookup as defined by {@link #lookUpWorkingObjectOrFail(Object)}.
     *
     * @param problemFact the problem fact instance
     * @param problemFactConsumer removes the working problem fact from the
     *        {@link PlanningSolution working solution}
     * @param <ProblemFact> the problem fact object type
     */
    <ProblemFact> void removeProblemFact(ProblemFact problemFact, Consumer<ProblemFact> problemFactConsumer);

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
    <EntityOrProblemFact> void changeProblemProperty(EntityOrProblemFact problemFactOrEntity,
            Consumer<EntityOrProblemFact> problemFactOrEntityConsumer);

    /**
     * Translate an entity or fact instance (often from another {@link Thread} or JVM)
     * to this {@link ProblemChangeDirector}'s internal working instance.
     * <p>
     * Matching uses {@link PlanningId}.
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
     * @return {@link Optional#empty()} if there is no workingObject for externalObject, or if externalObject is null
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
