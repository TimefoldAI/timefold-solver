package ai.timefold.solver.core.impl.move;

import java.util.IdentityHashMap;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Tracks, per entity, the {@link ListVariableBeforeChangeAction}
 * awaiting its matching {@code afterListVariableChanged} call,
 * so the two can be merged into a single undo step instead of two.
 * <p>
 * The common case is exactly one entity pending at a time
 * (every single-entity list move, such as plain change/assign/unassign).
 * Only lesser used moves (such as k-opt) ever have more than one pending concurrently.
 * This starts as a zero-allocation single slot and escalates to a real map only once a second,
 * distinct entity's bracket opens while the first is still pending.
 */
@NullMarked
final class PendingListChangeTracker {

    private @Nullable Object singleEntity;
    private @Nullable ListVariableBeforeChangeAction<?, ?, ?> singleAction;
    private @Nullable Map<Object, ListVariableBeforeChangeAction<?, ?, ?>> overflowMap;

    void put(Object entity, ListVariableBeforeChangeAction<?, ?, ?> action) {
        if (overflowMap != null) {
            overflowMap.put(entity, action);
            return;
        }
        if (singleEntity == null || singleEntity == entity) {
            // Either the slot is free, or the same entity is re-opening a fresh bracket after its
            // previous one was already resolved (or, defensively, left stale by an aborted move) -
            // either way, this is not a second concurrent entity, so no escalation is needed.
            singleEntity = entity;
            singleAction = action;
            return;
        }
        // A second, distinct entity opened a bracket while the first is still pending: escalate.
        var map = new IdentityHashMap<Object, ListVariableBeforeChangeAction<?, ?, ?>>(4);
        map.put(singleEntity, singleAction);
        map.put(entity, action);
        overflowMap = map;
        singleEntity = null;
        singleAction = null;
    }

    @Nullable
    ListVariableBeforeChangeAction<?, ?, ?> remove(Object entity) {
        if (overflowMap != null) {
            return overflowMap.remove(entity);
        }
        if (singleEntity == entity) {
            var action = singleAction;
            singleEntity = null;
            singleAction = null;
            return action;
        }
        return null;
    }

    void clear() {
        singleEntity = null;
        singleAction = null;
        if (overflowMap != null) {
            overflowMap.clear();
        }
    }

}
