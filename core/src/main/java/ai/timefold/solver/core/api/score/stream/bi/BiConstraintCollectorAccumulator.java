package ai.timefold.solver.core.api.score.stream.bi;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;

import org.jspecify.annotations.NullMarked;

/**
 * As defined by {@link UniConstraintCollectorAccumulator},
 * only for {@link BiConstraintCollector}.
 */
@NullMarked
@FunctionalInterface
public interface BiConstraintCollectorAccumulator<ResultContainer_, A, B> {

    /**
     * As defined by {@link UniConstraintCollectorAccumulator#intoGroup(Object)},
     * only for {@link BiConstraintCollector}.
     */
    BiConstraintCollectorAccumulatedValue<A, B> startGroup(ResultContainer_ resultContainer);

}
