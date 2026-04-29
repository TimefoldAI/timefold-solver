package ai.timefold.solver.core.api.score.stream.uni;

import org.jspecify.annotations.NullMarked;

/**
 * Used by {@link UniConstraintCollector#incrementalAccumulator()}.
 * Allows high-performance accumulation by providing an update operation
 * as well as by letting the accumulator decide whether the group was changed as a result of its operations,
 * but it is more difficult to implement.
 *
 * @param <ResultContainer_>
 * @param <A>
 */
@NullMarked
@FunctionalInterface
public interface UniConstraintCollectorAccumulator<ResultContainer_, A> {

    /**
     * Created every time a new value enters the group.
     *
     * @param resultContainer The container which represents the accumulated state of the group.
     *        If the group is empty,
     *        {@link UniConstraintCollector#supplier()} will be used to supply a fresh instance of the container;
     *        otherwise a pre-existing container will be used, carrying the accumulated state.
     *        {@link UniConstraintCollector#finisher()} will be used to extract the final accumulated value from the container.
     *        The container is the only state that the accumulator may rely on;
     *        any other state will result in subtle score corruption issues.
     * @return the accumulator for the value, which will be used to insert the value to the group,
     *         to update it while in the group, and to remove it from the group.
     */
    UniConstraintCollectorAccumulatedValue<A> intoGroup(ResultContainer_ resultContainer);

}
