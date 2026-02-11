package ai.timefold.solver.core.impl.domain.variable.listener.support;

import java.util.ArrayDeque;
import java.util.Collection;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.variable.BasicVariableListener;
import ai.timefold.solver.core.impl.domain.variable.ChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.ListVariableListener;
import ai.timefold.solver.core.impl.domain.variable.VariableListener;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.util.ListBasedScalingOrderedSet;

import org.jspecify.annotations.NullMarked;

/**
 * Generic notifiable that receives and triggers {@link Notification}s for a specific variable listener of the type {@code T}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <Listener_> the variable listener type
 */
@NullMarked
abstract class AbstractNotifiable<Solution_, ChangeEvent_ extends ChangeEvent, Listener_ extends VariableListener<Solution_, ChangeEvent_>>
        implements EntityNotifiable<Solution_> {

    private final InnerScoreDirector<Solution_, ?> scoreDirector;
    private final Listener_ variableListener;
    private final Collection<Notification<Solution_, ChangeEvent_, Listener_>> notificationQueue;
    private final int globalOrder;

    static <Solution_, ChangeEvent_ extends ChangeEvent, Listener_ extends VariableListener<Solution_, ChangeEvent_>>
            EntityNotifiable<Solution_> buildNotifiable(
                    InnerScoreDirector<Solution_, ?> scoreDirector,
                    Listener_ variableListener,
                    int globalOrder) {
        if (variableListener instanceof BasicVariableListener<?, ?> basicVariableListener) {
            return new VariableListenerNotifiable<>(
                    scoreDirector,
                    (BasicVariableListener<Solution_, Object>) basicVariableListener,
                    variableListener.requiresUniqueEntityEvents() ? new ListBasedScalingOrderedSet<>() : new ArrayDeque<>(),
                    globalOrder);
        } else if (variableListener instanceof ListVariableListener<?, ?, ?> listVariableListener) {
            return new ListVariableListenerNotifiable<>(
                    scoreDirector,
                    (ListVariableListener<Solution_, Object, Object>) listVariableListener,
                    new ArrayDeque<>(), globalOrder);
        } else {
            throw new IllegalArgumentException("Impossible state: InnerVariableListener (%s) must be an instance of %s or %s."
                    .formatted(variableListener.getClass().getCanonicalName(), BasicVariableListener.class.getSimpleName(),
                            ListVariableListener.class.getSimpleName()));
        }
    }

    AbstractNotifiable(InnerScoreDirector<Solution_, ?> scoreDirector,
            Listener_ variableListener,
            Collection<Notification<Solution_, ChangeEvent_, Listener_>> notificationQueue,
            int globalOrder) {
        this.scoreDirector = scoreDirector;
        this.variableListener = variableListener;
        this.notificationQueue = notificationQueue;
        this.globalOrder = globalOrder;
    }

    @Override
    public VariableListener<Solution_, ?> getVariableListener() {
        return variableListener;
    }

    protected boolean storeForLater(Notification<Solution_, ChangeEvent_, Listener_> notification) {
        return notificationQueue.add(notification);
    }

    protected void triggerBefore(Notification<Solution_, ChangeEvent_, Listener_> notification) {
        notification.triggerBefore(variableListener, scoreDirector);
    }

    @Override
    public void resetWorkingSolution() {
        variableListener.resetWorkingSolution(scoreDirector);
    }

    @Override
    public void closeVariableListener() {
        variableListener.close();
    }

    @Override
    public void clearAllNotifications() {
        notificationQueue.clear();
    }

    @Override
    public void triggerAllNotifications() {
        var notifiedCount = 0;
        for (var notification : notificationQueue) {
            notification.triggerAfter(variableListener, scoreDirector);
            notifiedCount++;
        }
        if (notifiedCount != notificationQueue.size()) {
            throw new IllegalStateException(
                    """
                            The variableListener (%s) has been notified with notifiedCount (%d) but after being triggered, its notificationCount (%d) is different.
                            Maybe that variableListener (%s) changed an upstream shadow variable (which is illegal)."""
                            .formatted(variableListener.getClass(), notifiedCount, notificationQueue.size(),
                                    variableListener.getClass()));
        }
        notificationQueue.clear();
    }

    @Override
    public String toString() {
        return "(%d) %s".formatted(globalOrder, variableListener);
    }
}
