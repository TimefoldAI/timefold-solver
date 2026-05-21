package ai.timefold.solver.core.api.score.stream.bi;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;

import org.jspecify.annotations.NullMarked;

/**
 * As defined by {@link UniConstraintCollectorAccumulator},
 * only for {@link BiConstraintCollector}.
 */
@NullMarked
@FunctionalInterface
public interface BiConstraintCollectorAccumulator<ResultContainer_, A, B>
        extends TriFunction<ResultContainer_, A, B, Runnable> {

    /**
     * As defined by {@link UniConstraintCollectorAccumulator#intoGroup(Object)},
     * only for {@link BiConstraintCollector}.
     */
    BiConstraintCollectorValueHandle<A, B> intoGroup(ResultContainer_ resultContainer);

    /**
     * @deprecated Use {@link #intoGroup(Object)} instead.
     * @throws UnsupportedOperationException always
     */
    @Deprecated(since = "2.2.0", forRemoval = true)
    @Override
    default Runnable apply(ResultContainer_ resultContainer, A a, B b) {
        throw new UnsupportedOperationException("Use intoGroup() instead.");
    }

}
