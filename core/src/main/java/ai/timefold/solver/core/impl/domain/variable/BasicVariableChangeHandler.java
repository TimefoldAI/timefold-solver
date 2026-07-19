package ai.timefold.solver.core.impl.domain.variable;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;

/**
 * Flattened, event-free dispatch shape for shadow variable updates that need to be notified
 * eagerly of basic variable changes, without allocating a change event for every notification.
 *
 * @param <Solution_> the solution type, the class with the {@link ai.timefold.solver.core.api.domain.solution.PlanningSolution}
 *        annotation
 */
@NullMarked
public interface BasicVariableChangeHandler<Solution_> {

    VariableDescriptor<Solution_> getSourceVariableDescriptor();

    /**
     * Called when the entire working solution changes.
     * At this point, implementations should clear state, if any.
     */
    void resetWorkingSolution(InnerScoreDirector<Solution_, ?> scoreDirector);

    /**
     * Called before this {@link BasicVariableChangeHandler} is thrown away and not used anymore.
     */
    default void close() {
        // No need to do anything for stateless implementations.
    }

    void beforeVariableChanged(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity);

    void afterVariableChanged(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity);

}
