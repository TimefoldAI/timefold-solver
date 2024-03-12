package ai.timefold.solver.core.api.score.stream.tri;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintStream;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;

/**
 * As described by {@link UniConstraintCollector}, only for {@link TriConstraintStream}.
 *
 * @param <A> the type of the first fact of the tuple in the source {@link TriConstraintStream}
 * @param <B> the type of the second fact of the tuple in the source {@link TriConstraintStream}
 * @param <C> the type of the third fact of the tuple in the source {@link TriConstraintStream}
 * @param <ResultContainer_> the mutable accumulation type (often hidden as an implementation detail)
 * @param <Result_> the type of the fact of the tuple in the destination {@link ConstraintStream}
 *        It is recommended that this type be deeply immutable.
 *        Not following this recommendation may lead to hard-to-debug hashing issues down the stream,
 *        especially if this value is ever used as a group key.
 * @see ConstraintCollectors
 */
public interface TriConstraintCollector<A, B, C, ResultContainer_, Result_> {

    /**
     * A lambda that creates the result container, one for each group key combination.
     *
     * @return never null
     */
    Supplier<ResultContainer_> supplier();

    /**
     * A lambda that extracts data from the matched facts,
     * accumulates it in the result container
     * and returns an undo operation for that accumulation.
     *
     * @return never null, the undo operation. This lambda is called when the facts no longer matches.
     */
    QuadFunction<ResultContainer_, A, B, C, Runnable> accumulator();

    /**
     * A lambda that converts the result container into the result.
     *
     * @return never null
     */
    Function<ResultContainer_, Result_> finisher();

}
