package ai.timefold.solver.core.impl.domain.variable.listener.support;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.AbstractVariableListener;
import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

/**
 * A notification represents some kind of change of a planning variable. When a score director is notified about a change,
 * one notification is created for each {@link Notifiable} registered for the subject of the change.
 *
 * <p>
 * Each implementation is tailored to a specific {@link AbstractVariableListener} and triggers on the listener
 * the pair of "before/after" methods corresponding to the type of change it represents.
 *
 * <p>
 * For example, if there is a shadow variable sourced on the {@code Process.computer} genuine planning variable,
 * then there is a notifiable {@code F} registered for the {@code Process.computer} planning variable, and it holds a basic
 * variable listener {@code L}.
 * When {@code Process X} is moved from {@code Computer A} to {@code Computer B}, a notification {@code N} is created and added
 * to notifiable {@code F}'s queue. The notification {@code N} triggers
 * {@link VariableListener#beforeVariableChanged L.beforeVariableChanged(scoreDirector, Process X)} immediately.
 * Later, when {@link Notifiable#triggerAllNotifications() F.triggerAllNotifications()} is called, {@code N} is taken from
 * the queue and triggers {@link VariableListener#afterVariableChanged}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @param <T> the variable listener type
 */
public interface Notification<Solution_, T extends AbstractVariableListener<Solution_, Object>> {

    /**
     * The {@code entity} was added.
     */
    static <Solution_> EntityNotification<Solution_> entityAdded(Object entity) {
        return new EntityAddedNotification<>(entity);
    }

    /**
     * The {@code entity} was removed.
     */
    static <Solution_> EntityNotification<Solution_> entityRemoved(Object entity) {
        return new EntityRemovedNotification<>(entity);
    }

    /**
     * Basic genuine or shadow planning variable changed on {@code entity}.
     */
    static <Solution_> BasicVariableNotification<Solution_> variableChanged(Object entity) {
        return new VariableChangedNotification<>(entity);
    }

    /**
     * An element was unassigned from a list variable.
     */
    static <Solution_> ListVariableNotification<Solution_> elementUnassigned(Object element) {
        return new ElementUnassignedNotification<>(element);
    }

    /**
     * A list variable change occurs on {@code entity} between {@code fromIndex} and {@code toIndex}.
     */
    static <Solution_> ListVariableChangedNotification<Solution_> listVariableChanged(Object entity, int fromIndex,
            int toIndex) {
        return new ListVariableChangedNotification<>(entity, fromIndex, toIndex);
    }

    /**
     * Trigger {@code variableListener}'s before method corresponding to this notification.
     */
    void triggerBefore(T variableListener, ScoreDirector<Solution_> scoreDirector);

    /**
     * Trigger {@code variableListener}'s after method corresponding to this notification.
     */
    void triggerAfter(T variableListener, ScoreDirector<Solution_> scoreDirector);
}
