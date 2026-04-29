package ai.timefold.solver.core.api.score.stream.tri;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;

import org.jspecify.annotations.NullMarked;

/**
 * As defined by {@link UniConstraintCollectorAccumulator},
 * only for {@link TriConstraintCollector}.
 */
@NullMarked
@FunctionalInterface
public interface TriConstraintCollectorAccumulator<ResultContainer_, A, B, C> {

    /**
     * As defined by {@link UniConstraintCollectorAccumulator#intoGroup(Object)},
     * only for {@link TriConstraintCollector}.
     */
    TriConstraintCollectorAccumulatedValue<A, B, C> startGroup(ResultContainer_ resultContainer);

}
