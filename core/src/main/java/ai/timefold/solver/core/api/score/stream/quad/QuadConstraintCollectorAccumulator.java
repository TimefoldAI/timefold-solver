package ai.timefold.solver.core.api.score.stream.quad;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;

import org.jspecify.annotations.NullMarked;

/**
 * As defined by {@link UniConstraintCollectorAccumulator},
 * only for {@link QuadConstraintCollector}.
 */
@NullMarked
@FunctionalInterface
public interface QuadConstraintCollectorAccumulator<ResultContainer_, A, B, C, D> {

    /**
     * As defined by {@link UniConstraintCollectorAccumulator#intoGroup(Object)},
     * only for {@link QuadConstraintCollector}.
     */
    QuadConstraintCollectorAccumulatedValue<A, B, C, D> startGroup(ResultContainer_ resultContainer);

}
