package ai.timefold.solver.core.api.score.stream.uni;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Allows to modify a particular group of values.
 *
 * <p>
 * The methods return true when the group's finished value
 * (as defined by {@link UniConstraintCollector#finisher()})
 * has changed.
 * If they return false instead, the solver will not propagate this change to downstream tuples,
 * which will significantly improve performance of Constraint Streams,
 * but may also trigger subtle bugs.
 * If you're unsure of what to return, true is the safe choice.
 *
 * <p>
 * Example: consider accumulating a minimum of any group of values.
 * <ul>
 * <li>Adding, updating or removing a value greater than the current minimum will not change the minimum,
 * and therefore it is safe to return false.</li>
 * <li>Adding or removing a value that is the current minimum will change the minimum,
 * and therefore true must be returned.</li>
 * <li>Updating a value is a special case; when updating the value which is the minimum,
 * you still need to return true unless you are absolutely certain that the value's internal state remains identical
 * to when it was added.
 * Downstream nodes may depend on those internals and if Constraint Streams do not propagate this update,
 * a subtle data consistency bug will be introduced.</li>
 * </ul>
 * 
 * @param <A>
 */
@NullMarked
public interface UniConstraintCollectorAccumulatedValue<A> {

    /**
     * Add a value to the group.
     *
     * @param a The component of the tuple.
     * @return True if the group's result (when {@link UniConstraintCollector#finisher() the finished} is called)
     *         will have changed, false otherwise.
     */
    boolean add(@Nullable A a);

    /**
     * Update a previously {@link #add(Object) added} value.
     *
     * @param a The component of the tuple.
     *        It is expected to be the component of the very same tuple that was used to {@link #add(Object)}.
     *        For other tuples, a fresh {@link UniConstraintCollectorAccumulatedValue} should be obtained
     *        by calling {@link UniConstraintCollectorAccumulator#intoGroup(Object)}.
     * @return True if the group's result (when {@link UniConstraintCollector#finisher() the finished} is called)
     *         will have changed, false otherwise.
     */
    boolean update(@Nullable A a);

    /**
     * Remove a previously {@link #add(Object) added} value from the group.
     *
     * @return True if the group's result (when {@link UniConstraintCollector#finisher() the finished} is called)
     *         will have changed, false otherwise.
     */
    boolean remove();

}
