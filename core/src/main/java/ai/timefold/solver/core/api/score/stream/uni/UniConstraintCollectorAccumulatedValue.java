package ai.timefold.solver.core.api.score.stream.uni;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Allows to modify a particular group of values.
 *
 * @param <A>
 */
@NullMarked
public interface UniConstraintCollectorAccumulatedValue<A> {

    /**
     * Add a value to the group.
     *
     * @param a The component of the tuple.
     */
    void add(@Nullable A a);

    /**
     * Update a previously {@link #add(Object) added} value.
     * In some cases, the default implementation (remove+add) will be enough,
     * but this method exists for advanced cases where updates can be done more efficiently.
     *
     * @param a The component of the tuple.
     *        It is expected to be the component of the very same tuple that was used to {@link #add(Object)}.
     *        For other tuples, a fresh {@link UniConstraintCollectorAccumulatedValue} should be obtained
     *        by calling {@link UniConstraintCollectorAccumulator#intoGroup(Object)}.
     */
    default void update(@Nullable A a) {
        remove();
        add(a);
    }

    /**
     * Remove a previously {@link #add(Object) added} value from the group.
     */
    void remove();

}
