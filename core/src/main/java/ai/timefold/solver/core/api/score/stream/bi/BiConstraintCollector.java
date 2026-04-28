package ai.timefold.solver.core.api.score.stream.bi;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintStream;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * As described by {@link UniConstraintCollector}, only for {@link BiConstraintStream}.
 *
 * @param <A> the type of the first fact of the tuple in the source {@link BiConstraintStream}
 * @param <B> the type of the second fact of the tuple in the source {@link BiConstraintStream}
 * @param <ResultContainer_> the mutable accumulation type (often hidden as an implementation detail)
 * @param <Result_> the type of the fact of the tuple in the destination {@link ConstraintStream}
 *        It is recommended that this type be deeply immutable.
 *        Not following this recommendation may lead to hard-to-debug hashing issues down the stream,
 *        especially if this value is ever used as a group key.
 * @see ConstraintCollectors
 */
@NullMarked
public interface BiConstraintCollector<A, B, ResultContainer_, Result_> {

    /**
     * As defined by {@link UniConstraintCollector#supplier()}, but for {@link BiConstraintStream}.
     */
    Supplier<ResultContainer_> supplier();

    /**
     * As defined by {@link UniConstraintCollector#accumulator()}, but for {@link BiConstraintStream}.
     */
    TriFunction<ResultContainer_, A, B, Runnable> accumulator();

    /**
     * As defined by {@link UniConstraintCollector#incrementalAccumulator()}, but for {@link BiConstraintStream}.
     */
    default BiConstraintCollectorIncrementalAccumulator<A, B, ResultContainer_> incrementalAccumulator() {
        throw new UnsupportedOperationException();
    }

    /**
     * As defined by {@link UniConstraintCollector#isIncremental()}, but for {@link BiConstraintStream}.
     */
    default boolean isIncremental() {
        return false;
    }

    /**
     * As defined by {@link UniConstraintCollector#finisher()}, but for {@link BiConstraintStream}.
     */
    Function<ResultContainer_, @Nullable Result_> finisher();

}
