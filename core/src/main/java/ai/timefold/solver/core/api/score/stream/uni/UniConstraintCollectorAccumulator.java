package ai.timefold.solver.core.api.score.stream.uni;

import java.util.function.BiFunction;

import org.jspecify.annotations.NullMarked;

/**
 * Used by {@link UniConstraintCollector#accumulator()} for incremental collectors.
 * Extends {@link BiFunction} for backward compatibility, but {@link #apply(Object, Object)} always throws;
 * use {@link #intoGroup(Object)} instead.
 * Allows high-performance accumulation by providing an {@link UniConstraintCollectorValueHandle#replaceWith update} operation.
 *
 * @param <ResultContainer_>
 * @param <A>
 */
@NullMarked
@FunctionalInterface
public interface UniConstraintCollectorAccumulator<ResultContainer_, A>
        extends BiFunction<ResultContainer_, A, Runnable> {

    /**
     * Created every time a new value enters the group.
     *
     * @param resultContainer The container which represents the accumulated state of the group.
     *        If the group is empty,
     *        {@link UniConstraintCollector#supplier()} will be used to supply a fresh instance of the container;
     *        otherwise a pre-existing container will be used, carrying the accumulated state.
     *        {@link UniConstraintCollector#finisher()} will be used to extract the final accumulated value from the container.
     *        The method itself must not maintain any state;
     *        any container-level state must live in the container,
     *        and any state required to update or remove the value must live in the handle.
     * @return the handle for the value, which will be used to insert the value to the group,
     *         to update it while in the group, and to remove it from the group.
     */
    UniConstraintCollectorValueHandle<A> intoGroup(ResultContainer_ resultContainer);

    /**
     * @deprecated Use {@link #intoGroup(Object)} instead.
     * @throws UnsupportedOperationException always
     */
    @Deprecated(since = "2.2.0", forRemoval = true)
    @Override
    default Runnable apply(ResultContainer_ resultContainer, A a) {
        throw new UnsupportedOperationException("Use intoGroup() instead.");
    }

}
