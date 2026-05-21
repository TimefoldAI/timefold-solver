package ai.timefold.solver.core.api.score.stream.tri;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;

import org.jspecify.annotations.NullMarked;

/**
 * As defined by {@link UniConstraintCollectorAccumulator},
 * only for {@link TriConstraintCollector}.
 */
@NullMarked
@FunctionalInterface
public interface TriConstraintCollectorAccumulator<ResultContainer_, A, B, C>
        extends QuadFunction<ResultContainer_, A, B, C, Runnable> {

    /**
     * As defined by {@link UniConstraintCollectorAccumulator#intoGroup(Object)},
     * only for {@link TriConstraintCollector}.
     */
    TriConstraintCollectorValueHandle<A, B, C> intoGroup(ResultContainer_ resultContainer);

    /**
     * @deprecated Use {@link #intoGroup(Object)} instead.
     * @throws UnsupportedOperationException always
     */
    @Deprecated(since = "2.2.0", forRemoval = true)
    @Override
    default Runnable apply(ResultContainer_ resultContainer, A a, B b, C c) {
        throw new UnsupportedOperationException("Use intoGroup() instead.");
    }

}
