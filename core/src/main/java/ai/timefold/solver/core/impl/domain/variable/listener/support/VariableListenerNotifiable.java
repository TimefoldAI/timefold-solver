package ai.timefold.solver.core.impl.domain.variable.listener.support;

import java.util.Collection;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.variable.BasicVariableChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.InnerBasicVariableListener;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

/**
 * A notifiable specialized to receive {@link BasicVariableNotification}s and trigger them on a given
 * {@link InnerBasicVariableListener}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
final class VariableListenerNotifiable<Solution_> extends
        AbstractNotifiable<Solution_, BasicVariableChangeEvent<Object>, InnerBasicVariableListener<Solution_, Object>> {

    VariableListenerNotifiable(
            InnerScoreDirector<Solution_, ?> scoreDirector,
            InnerBasicVariableListener<Solution_, Object> variableListener,
            Collection<Notification<Solution_, BasicVariableChangeEvent<Object>, InnerBasicVariableListener<Solution_, Object>>> notificationQueue,
            int globalOrder) {
        super(scoreDirector, variableListener, notificationQueue, globalOrder);
    }

    public void notifyBefore(BasicVariableNotification<Solution_> notification) {
        if (storeForLater(notification)) {
            triggerBefore(notification);
        }
    }
}
