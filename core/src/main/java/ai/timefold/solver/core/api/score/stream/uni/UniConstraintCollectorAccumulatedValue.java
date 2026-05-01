package ai.timefold.solver.core.api.score.stream.uni;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents a handle for a single value in a single {@link UniConstraintCollectorAccumulator} group.
 * <ul>
 *  <li>{@link #add(Object)} will be called externally exactly once, when the value enters the group.
 *  An instance of {@link UniConstraintCollectorAccumulatedValue} will only be created if there is a value to add.</li>
 *  <li>{@link #update(Object)} will be called externally zero or more times.</li>
 *  <li>{@link #remove()} will be called externally at most once, if the value is ever removed from the group.</li>
 * </ul>
 * This contract guarantees that the user can keep internal caches between add, update and remove
 * to avoid some expensive operations; if the added object equals the updated or removed object,
 * these caches most likely remain valid because we are updating the same logical element (tuple).
 *
 * @param <A> the fact in the tuple
 */
@NullMarked
public interface UniConstraintCollectorAccumulatedValue<A> {

    /**
     * Add a value to the group.
     * Will be followed by zero or more external {@link #update(Object) updates}
     * and at most one external {@link #remove() removal}.
     *
     * @param a The component of the tuple.
     */
    void add(@Nullable A a);

    /**
     * Update a previously {@link #add(Object) added} value.
     * This method will never be called externally before {@link #add(Object)} or after {@link #remove()}.
     * In some cases, the default implementation (remove+add) will be enough,
     * but this method exists for advanced cases where updates can be done more efficiently.
     *
     * @param a The component of the tuple.
     *        It is expected to be the component of the very same tuple that was used to {@link #add(Object)}.
     *        The component may be an entirely different object instance than was added.
     *        For other tuples, a fresh {@link UniConstraintCollectorAccumulatedValue} will be obtained;
     *        an instance of this interface is guaranteed to work on one and only one tuple.
     */
    default void update(@Nullable A a) {
        remove();
        add(a);
    }

    /**
     * Remove a previously {@link #add(Object) added} value from the group.
     * After this method is called, this instance of {@link UniConstraintCollectorAccumulatedValue} will never be used again.
     */
    void remove();

}
