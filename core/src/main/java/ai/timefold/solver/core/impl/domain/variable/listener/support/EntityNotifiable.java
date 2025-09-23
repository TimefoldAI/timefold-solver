package ai.timefold.solver.core.impl.domain.variable.listener.support;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.variable.InnerVariableListener;

/**
 * A notifiable listening for {@link EntityNotification}s. Every variable listener's notifiable is not only registered for
 * the listener's source variable notifications but also for the planning entity declaring the source variable.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public interface EntityNotifiable<Solution_> extends Notifiable {
    InnerVariableListener<Solution_, ?> getVariableListener();
}
