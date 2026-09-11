package ai.timefold.solver.core.impl.move;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Tracks, per entity, the {@link ListVariableBeforeChangeAction}
 * awaiting its matching {@code afterListVariableChanged} call,
 * so the two can be merged into a single undo step instead of two.
 * <p>
 * At most one bracket per entity may be open at a time.
 * A second {@code beforeListVariableChanged} for an entity whose bracket is still open is rejected:
 * such a sequence cannot be undone (the two ranges shift each other),
 * and silently keeping only one of the two brackets corrupts the list variable instead.
 * Sequential brackets for the same entity are fine - only concurrently open ones are not.
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
            // Map.put returns the entry it replaced; a non-null one is a bracket that was still open.
            var previousAction = overflowMap.put(entity, action);
            if (previousAction != null) {
                throw bracketAlreadyOpen(entity, previousAction, action);
            }
            return;
        }
        if (singleEntity == null) { // The slot is free.
            singleEntity = entity;
            singleAction = action;
            return;
        }
        if (singleEntity == entity) {
            throw bracketAlreadyOpen(entity, Objects.requireNonNull(singleAction), action);
        }
        // A second, distinct entity opened a bracket while the first is still pending: escalate.
        var map = new IdentityHashMap<Object, ListVariableBeforeChangeAction<?, ?, ?>>(4);
        map.put(singleEntity, Objects.requireNonNull(singleAction));
        map.put(entity, action);
        overflowMap = map;
        singleEntity = null;
        singleAction = null;
    }

    private static IllegalArgumentException bracketAlreadyOpen(Object entity,
            ListVariableBeforeChangeAction<?, ?, ?> openAction, ListVariableBeforeChangeAction<?, ?, ?> newAction) {
        return new IllegalArgumentException("""
                The entity (%s) already has an open beforeListVariableChanged (%d, %d), \
                so another beforeListVariableChanged (%d, %d) for it must not be opened.
                Maybe close each beforeListVariableChanged with its afterListVariableChanged \
                before changing the same entity again."""
                .formatted(entity, openAction.fromIndex(), openAction.originalToIndex(),
                        newAction.fromIndex(), newAction.originalToIndex()));
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
        overflowMap = null;
    }

}
