package ai.timefold.solver.core.impl.domain.variable.listener.support;

import java.util.Collection;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.variable.ListElementsChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.ListVariableListener;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;

/**
 * A notifiable specialized to receive {@link ListVariableNotification}s and trigger them on a given
 * {@link ListVariableListener}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
final class ListVariableListenerNotifiable<Solution_, Listener_ extends ListVariableListener<Solution_, Object, Object>>
        extends
        AbstractNotifiable<Solution_, ListElementsChangeEvent<Object>, ListVariableListener<Solution_, Object, Object>> {

    ListVariableListenerNotifiable(
            InnerScoreDirector<Solution_, ?> scoreDirector,
            Listener_ variableListener,
            Collection<Notification<Solution_, ListElementsChangeEvent<Object>, Listener_>> notificationQueue,
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
