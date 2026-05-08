package ai.timefold.solver.core.impl.bavet.common.tuple;

import static ai.timefold.solver.core.impl.bavet.common.ConstraintNodeProfileId.Qualifier;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import ai.timefold.solver.core.api.function.QuadPredicate;
import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.impl.bavet.common.AbstractNode;
import ai.timefold.solver.core.impl.bavet.common.BavetStream;
import ai.timefold.solver.core.impl.bavet.common.ConstraintNodeProfileId;
import ai.timefold.solver.core.impl.bavet.common.InnerConstraintProfiler;
import ai.timefold.solver.core.impl.bavet.common.Propagator;
import ai.timefold.solver.core.impl.bavet.common.StreamKind;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintSession;
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
            InnerConstraintProfiler constraintProfiler, long lifecycleId, Stream_ stream,
            TupleLifecycle<Tuple_> delegate) {
        if (delegate instanceof AggregatedTupleLifecycle) {
            // Do not profile aggregated tuple lifecycles; that will double
            // count the lifecycles being aggregated.
            return delegate;
        }

        var streamKind = StreamKind.FILTER;
        var qualifier = Qualifier.NONE;

        if (delegate instanceof AbstractNode node) {
            streamKind = node.getStreamKind();
            qualifier = Qualifier.NODE;
        } else if (delegate instanceof LeftTupleLifecycleImpl<?>(LeftTupleLifecycle<?> lifecycle)
                && lifecycle instanceof AbstractNode node) {
            streamKind = node.getStreamKind();
            qualifier = Qualifier.LEFT_INPUT;
        } else if (delegate instanceof RightTupleLifecycleImpl<?>(RightTupleLifecycle<?> tupleLifecycle)
                && tupleLifecycle instanceof AbstractNode node) {
            streamKind = node.getStreamKind();
            qualifier = Qualifier.RIGHT_INPUT;
        } else if (delegate instanceof RecordingTupleLifecycle<Tuple_>) {
            streamKind = StreamKind.PRECOMPUTE;
            qualifier = Qualifier.NODE;
        } else if (delegate instanceof Scorer<Tuple_>) {
            streamKind = StreamKind.SCORING;
        } else if (!(delegate instanceof ConditionalTupleLifecycle<Tuple_>)) {
            throw new IllegalStateException(
                    "Impossible state: encounter tuple lifecycle (%s) which is not a node and is not a known lifecycle implementation."
                            .formatted(delegate.getClass()));
        }
        var profileId = new ConstraintNodeProfileId(lifecycleId, streamKind, qualifier, stream.getLocationSet());
        return new ProfilingTupleLifecycle<>(constraintProfiler, profileId, delegate);
    }

    /**
     * Triggered after all facts which will ever be inserted have been inserted to the session;
     * it doesn't guarantee they were propagated as far as this lifecycle,
     * but the session now carries all the facts it will ever carry.
     * (The only way to insert or retract a fact is through a {@link ProblemChange},
     * and that will nuke the score director.)
     * <p>
     * It is the responsibility of the lifecycle to trigger initialization
     * of all of its downstream lifecycles, should there be any.
     * It must first decide for itself if it can produce tuples
     * based on what it learned from upstream,
     * and then propagate that information downstream so that they can make their own activation decisions.
     * <p>
     * When deciding whether a lifecycle can produce tuples, consider the following:
     * <ul>
     * <li>
     * Typically, when upstream cannot produce tuples, neither can downstream.
     * Exceptions exist; ifNotExists() produces tuples exactly when downstream doesn't.
     * </li>
     * <li>
     * Do not make decisions based on whether downstream produced any tuples by this point.
     * If upstream produced no tuples so far, it doesn't mean it will never produce any.
     * Filters on variables which previously did not match
     * can easily create tuples during tuple updates.
     * </li>
     * </ul>
     *
     * @param upstreamCanProduceTuples True if the upstream node(s) will produce any tuples.
     *        If false, this lifecycle will never receive any tuples and can deactivate itself.
     */
    void afterAllFactsInserted(boolean upstreamCanProduceTuples);

    /**
     * Lifecycles which are considered inactive will never be called by their {@link Propagator}.
     * A lifecycle is considered inactive if:
     *
     * <ul>
     * <li>It will not produce a tuple.
     * (Such as forEach(MyClass), when no MyClass instances were inserted.)</li>
     * <li>Its downstream tuples are inactive.
     * (A forEach() itself may be able to produce a tuple,
     * but a join downstream cannot, because its other side supplies no tuples.
     * In this case, the left side must be inactivated as well.)
     * </li>
     * </ul>
     *
     * A decision on whether a lifecycle is active can only be made when the following information is available:
     *
     * <ul>
     * <li>Upstream has informed us whether they can produce tuples.
     * This happens through {@link #afterAllFactsInserted(boolean) initialization}</li>
     * <li>Downstream has completed its initialization.
     * It is a lifecycle's responsibility to trigger downstream initialization.</li>
     * </ul>
     *
     * This is a one-time decision;
     * for each lifecycle, this method will only be called once for the duration of {@link BavetConstraintSession}.
     *
     * <p>
     * Typically this decision will be {@code upstreamCanProduceTuples && downstreamIsActive},
     * but some nodes will have to use different logic.
     *
     * @return true if this lifecycle can produce tuples
     */
    default boolean isActive() {
        throw new IllegalStateException("Impossible state: node (%s) not yet called initialized."
                .formatted(this));
    }

    void insert(Tuple_ tuple);

    void update(Tuple_ tuple);

    void retract(Tuple_ tuple);

}
