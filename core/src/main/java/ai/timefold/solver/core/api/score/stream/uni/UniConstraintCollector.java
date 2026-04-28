package ai.timefold.solver.core.api.score.stream.uni;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Usually created with {@link ConstraintCollectors}.
 * Used by {@link UniConstraintStream#groupBy(Function, UniConstraintCollector)}, ...
 * <p>
 * Loosely based on JDK's {@link Collector}, but it returns an undo operation for each accumulation
 * to enable incremental score calculation in {@link UniConstraintStream#groupBy(UniConstraintCollector)}.
 * It has two modes of operation:
 * <dl>
 * <dt>Insert+Retract</dt>
 * <dd>An element cannot be updated - every update is done by full retraction and reinsertion.
 * This is simpler to implement and therefore it is the default.
 * Provided by {@link #accumulator()}.</dd>
 * <dt>Fully incremental</dt>
 * <dd>An element may be inserted, updated and retracted,
 * allowing for increased performance when a smart update operation is available.
 * Enabled by {@link #incrementalAccumulator()}, when {@link #isIncremental()} returns true.</dd>
 * </dl>
 * <p>
 * It is recommended that if two constraint collectors implement the same functionality,
 * they should {@link Object#equals(Object) be equal}.
 * This may require comparing lambdas and method references for equality,
 * and in many cases this comparison will be false.
 * We still ask that you do this on the off chance that the objects are equal,
 * in which case Constraint Streams can perform some significant runtime performance optimizations.
 *
 * @param <A> the type of the one and only fact of the tuple in the source {@link UniConstraintStream}
 * @param <ResultContainer_> the mutable accumulation type (often hidden as an implementation detail)
 * @param <Result_> the type of the fact of the tuple in the destination {@link ConstraintStream}.
 *        Null when the result would be invalid, such as maximum value from an empty container.
 *        It is recommended that this type be deeply immutable.
 *        Not following this recommendation may lead to hard-to-debug hashing issues down the stream,
 *        especially if this value is ever used as a group key.
 *
 * @see ConstraintCollectors
 */
@NullMarked
public interface UniConstraintCollector<A, ResultContainer_, Result_> {

    /**
     * A lambda that creates the result container, one for each group key combination.
     */
    Supplier<ResultContainer_> supplier();

    /**
     * A lambda that extracts data from the matched fact,
     * accumulates it in the result container
     * and returns an undo operation for that accumulation.
     *
     * @return the accumulator. Called to insert the match and retract it when it no longer belongs in the group.
     * @see #incrementalAccumulator() High-performance alternative, disabled by default.
     */
    BiFunction<ResultContainer_, A, Runnable> accumulator();

    /**
     * A high-performance alternative to {@link #accumulator()} that returns an incremental accumulator.
     * This is more difficult to implement and therefore it is optional.
     * If implemented, override {@link #isIncremental()} to return true
     * and {@link #accumulator()} to throw {@link UnsupportedOperationException}.
     *
     * @return the accumulator. Called to insert the match, reflect changes
     *         or to revert it when the fact no longer belongs in the group.
     * @throws UnsupportedOperationException if {@link #isIncremental()} returns false
     */
    default UniConstraintCollectorIncrementalAccumulator<A, ResultContainer_> incrementalAccumulator() {
        throw new UnsupportedOperationException();
    }

    /**
     * Whether the solver should use {@link #incrementalAccumulator()} instead of {@link #accumulator()}.
     *
     * @return defaults to false. Only override to true if {@link #incrementalAccumulator()} is implemented.
     */
    default boolean isIncremental() {
        return false;
    }

    /**
     * A lambda that converts the result container into the result.
     * The result may be null, typically when there is nothing accumulated
     * and the product in that situation is undefined.
     * (Such as average of an empty set.)
     *
     * @return the operation to compute the finished result;
     */
    Function<ResultContainer_, @Nullable Result_> finisher();

}
