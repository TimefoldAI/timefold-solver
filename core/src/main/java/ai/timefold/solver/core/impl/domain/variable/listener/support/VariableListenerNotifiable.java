package ai.timefold.solver.core.impl.domain.variable.listener.support;

import java.util.Collection;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.impl.domain.variable.BasicVariableChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.InnerVariableListener;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

/**
 * A notifiable specialized to receive {@link BasicVariableNotification}s and trigger them on a given {@link VariableListener}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
final class VariableListenerNotifiable<Solution_> extends
        AbstractNotifiable<Solution_, BasicVariableChangeEvent<Object>, InnerVariableListener<Solution_, BasicVariableChangeEvent<Object>>> {

    VariableListenerNotifiable(
            InnerScoreDirector<Solution_, ?> scoreDirector,
            InnerVariableListener<Solution_, BasicVariableChangeEvent<Object>> variableListener,
            Collection<Notification<Solution_, BasicVariableChangeEvent<Object>, InnerVariableListener<Solution_, BasicVariableChangeEvent<Object>>>> notificationQueue,
            int globalOrder) {
        super(scoreDirector, variableListener, notificationQueue, globalOrder);
    }

    public void notifyBefore(BasicVariableNotification<Solution_> notification) {
        if (storeForLater(notification)) {
            triggerBefore(notification);
        }
    }
}
