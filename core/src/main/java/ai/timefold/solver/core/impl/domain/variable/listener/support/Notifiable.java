package ai.timefold.solver.core.impl.domain.variable.listener.support;

import ai.timefold.solver.core.api.domain.variable.AbstractVariableListener;

/**
 * A notifiable’s purpose is to execute variable listener methods. This interface is the most
 * generalized form of a notifiable. It covers variable listener methods that are executed immediately
 * ({@link AbstractVariableListener#resetWorkingSolution} and {@link AbstractVariableListener#close}.
 *
 * <p>
 * Specialized notifiables use {@link Notification}s to record planing variable changes and defer triggering of "after" methods
 * so that dependent variable listeners can be executed in the correct order.
 */
public interface Notifiable {

    /**
     * Notify the variable listener about working solution reset.
     */
    void resetWorkingSolution();

    /**
     * Clear all notifications without triggering any related event logic.
     */
    void clearAllNotifications();

    /**
     * Trigger all queued notifications.
     */
    void triggerAllNotifications();

    /**
     * Close the variable listener.
     */
    void closeVariableListener();
}
