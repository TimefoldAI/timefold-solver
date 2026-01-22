package ai.timefold.solver.core.impl.bavet.common.tuple;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.impl.bavet.common.AbstractNode;
import ai.timefold.solver.core.impl.bavet.common.BavetStream;
import ai.timefold.solver.core.impl.bavet.common.ConstraintNodeProfileId;
import ai.timefold.solver.core.impl.bavet.common.ConstraintProfiler;
import ai.timefold.solver.core.impl.bavet.common.StreamKind;
import ai.timefold.solver.core.impl.score.stream.bavet.common.Scorer;

public interface TupleLifecycle<Tuple_ extends Tuple> {

    static <Tuple_ extends Tuple> TupleLifecycle<Tuple_> ofLeft(LeftTupleLifecycle<Tuple_> leftTupleLifecycle) {
        return new LeftTupleLifecycleImpl<>(leftTupleLifecycle);
    }

    static <Tuple_ extends Tuple> TupleLifecycle<Tuple_> ofRight(RightTupleLifecycle<Tuple_> rightTupleLifecycle) {
        return new RightTupleLifecycleImpl<>(rightTupleLifecycle);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @SafeVarargs
    static <Tuple_ extends Tuple> TupleLifecycle<Tuple_> aggregate(TupleLifecycle<Tuple_>... tupleLifecycles) {
        var distinctTupleLifecycles = Arrays.stream(Objects.requireNonNull(tupleLifecycles))
                .distinct()
                .toArray(TupleLifecycle[]::new);
        return switch (distinctTupleLifecycles.length) {
            case 0 -> throw new IllegalStateException("Impossible state: there are no tuple lifecycles.");
            case 1 -> distinctTupleLifecycles[0];
            default -> new AggregatedTupleLifecycle<>(distinctTupleLifecycles);
        };
    }

    static <A> TupleLifecycle<UniTuple<A>> conditionally(TupleLifecycle<UniTuple<A>> tupleLifecycle, Predicate<A> predicate) {
        return new ConditionalTupleLifecycle<>(tupleLifecycle, tuple -> predicate.test(tuple.getA()));
    }

    static <A, B> TupleLifecycle<BiTuple<A, B>> conditionally(TupleLifecycle<BiTuple<A, B>> tupleLifecycle,
            BiPredicate<A, B> predicate) {
        return new ConditionalTupleLifecycle<>(tupleLifecycle, tuple -> predicate.test(tuple.getA(), tuple.getB()));
    }

    static <A, B, C> TupleLifecycle<TriTuple<A, B, C>> conditionally(TupleLifecycle<TriTuple<A, B, C>> tupleLifecycle,
            TriPredicate<A, B, C> predicate) {
        return new ConditionalTupleLifecycle<>(tupleLifecycle,
                tuple -> predicate.test(tuple.getA(), tuple.getB(), tuple.getC()));
    }

    static <A, B, C, D> TupleLifecycle<QuadTuple<A, B, C, D>>
            conditionally(TupleLifecycle<QuadTuple<A, B, C, D>> tupleLifecycle, QuadPredicate<A, B, C, D> predicate) {
        return new ConditionalTupleLifecycle<>(tupleLifecycle,
                tuple -> predicate.test(tuple.getA(), tuple.getB(), tuple.getC(), tuple.getD()));
    }

    static <Tuple_ extends Tuple> TupleLifecycle<Tuple_> recording() {
        return new RecordingTupleLifecycle<>();
    }

    static <Stream_ extends BavetStream, Tuple_ extends Tuple> TupleLifecycle<Tuple_> profiling(
            ConstraintProfiler constraintProfiler, long lifecycleId, Stream_ stream,
            TupleLifecycle<Tuple_> delegate) {
        if (delegate instanceof AggregatedTupleLifecycle) {
            // Do not profile aggregated tuple lifecycles; that will double
            // count the lifecycles being aggregated.
            return delegate;
        }

        var streamKind = StreamKind.FILTER;

        if (delegate instanceof AbstractNode node) {
            streamKind = node.getStreamKind();
        } else if (delegate instanceof LeftTupleLifecycleImpl<?> leftTupleLifecycle &&
                leftTupleLifecycle.leftTupleLifecycle() instanceof AbstractNode node) {
            streamKind = node.getStreamKind();
        } else if (delegate instanceof RightTupleLifecycleImpl<?> rightTupleLifecycle &&
                rightTupleLifecycle.rightTupleLifecycle() instanceof AbstractNode node) {
            streamKind = node.getStreamKind();
        } else if (delegate instanceof RecordingTupleLifecycle<Tuple_>) {
            streamKind = StreamKind.PRECOMPUTE;
        } else if (delegate instanceof Scorer<Tuple_>) {
            streamKind = StreamKind.SCORING;
        } else if (!(delegate instanceof ConditionalTupleLifecycle<Tuple_>)) {
            throw new IllegalStateException(
                    "Impossible state: encounter tuple lifecycle (%s) which is not a node and is not a known lifecycle implementation."
                            .formatted(delegate.getClass()));
        }
        return new ProfilingTupleLifecycle<>(constraintProfiler,
                new ConstraintNodeProfileId(lifecycleId, streamKind, stream.getLocationSet()),
                delegate);
    }

    void insert(Tuple_ tuple);

    void update(Tuple_ tuple);

    void retract(Tuple_ tuple);

}
