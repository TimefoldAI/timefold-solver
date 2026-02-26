package ai.timefold.solver.core.api.solver.change;

import java.util.function.Consumer;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

import org.jspecify.annotations.NullMarked;

/**
 * Allows external changes to the {@link PlanningSolution working solution}. If the changes are not applied through
 * the ProblemChangeDirector,
 * {@link ShadowVariable both internal and custom shadow variables} are
 * never notified about them, resulting to inconsistencies in the {@link PlanningSolution working solution}.
 * Should be used only from a {@link ProblemChange} implementation.
 * To see an example implementation, please refer to the {@link ProblemChange} Javadoc.
 *
 * @see Lookup You may need to perform lookups of working objects from external objects.
 * @see MockProblemChangeDirector You may need to use this for testing purposes.
 */
@NullMarked
public interface ProblemChangeDirector
        extends Lookup {

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
     * {@link #lookUpWorkingObject(Object)}.
     *
     * @param entity the {@link PlanningEntity} instance
     * @param entityConsumer removes the working entity from the {@link PlanningSolution working solution}
     * @param <Entity> the planning entity object type
     */
    <Entity> void removeEntity(Entity entity, Consumer<Entity> entityConsumer);

    /**
     * Change a {@link PlanningVariable} value of a {@link PlanningEntity}. Translates the entity to a working
     * planning entity by performing a lookup as defined by {@link #lookUpWorkingObject(Object)}.
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
     * to a working problem fact by performing a lookup as defined by {@link #lookUpWorkingObject(Object)}.
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
     * {@link #lookUpWorkingObject(Object)}.
     *
     * @param problemFactOrEntity the {@link PlanningEntity} or the problem fact instance
     * @param problemFactOrEntityConsumer updates the property of the {@link PlanningEntity}
     *        or the problem fact
     * @param <EntityOrProblemFact> the planning entity or problem fact object type
     */
    <EntityOrProblemFact> void changeProblemProperty(EntityOrProblemFact problemFactOrEntity,
            Consumer<EntityOrProblemFact> problemFactOrEntityConsumer);

    /**
     * Calls variable listeners on the external changes submitted so far.
     *
     * <p>
     * This happens automatically after the entire {@link ProblemChange} has been processed,
     * but this method allows the user to specifically request it in the middle of the {@link ProblemChange}.
     */
    void updateShadowVariables();

}
