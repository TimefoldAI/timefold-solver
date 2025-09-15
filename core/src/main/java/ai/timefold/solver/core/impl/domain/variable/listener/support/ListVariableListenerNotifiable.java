package ai.timefold.solver.core.impl.domain.variable.listener.support;

import java.util.Collection;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.impl.domain.variable.InnerVariableListener;
import ai.timefold.solver.core.impl.domain.variable.ListVariableChangeEvent;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;

/**
 * A notifiable specialized to receive {@link ListVariableNotification}s and trigger them on a given
 * {@link ListVariableListener}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
final class ListVariableListenerNotifiable<Solution_, Listener_ extends InnerVariableListener<Solution_, ListVariableChangeEvent<Object, Object>>>
        extends
        AbstractNotifiable<Solution_, ListVariableChangeEvent<Object, Object>, InnerVariableListener<Solution_, ListVariableChangeEvent<Object, Object>>> {

    ListVariableListenerNotifiable(
            InnerScoreDirector<Solution_, ?> scoreDirector,
            Listener_ variableListener,
            Collection<Notification<Solution_, ListVariableChangeEvent<Object, Object>, Listener_>> notificationQueue,
            int globalOrder) {
        super(scoreDirector, variableListener, (Collection) notificationQueue, globalOrder);
    }

    public void notifyBefore(ListVariableNotification<Solution_> notification) {
        triggerBefore(notification);
    }

    public void notifyAfter(ListVariableNotification<Solution_> notification) {
        storeForLater(notification);
    }
}
