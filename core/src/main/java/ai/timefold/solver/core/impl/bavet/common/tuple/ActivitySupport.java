package ai.timefold.solver.core.impl.bavet.common.tuple;

import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.impl.bavet.common.AbstractRootNode;
import ai.timefold.solver.core.impl.bavet.common.Propagator;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintSession;
import ai.timefold.solver.core.impl.score.stream.bavet.common.Scorer;

/**
 * When a Bavet session is created, some nodes can become inactive.
 * An inactive node cannot impact the score, as it cannot produce tuples.
 * Yet processing inserts, updates and retracts in inactive nodes still consumes CPU cycles.
 * This interface establishes a protocol through which nodes can signal at runtime
 * that they are inactive, so that the session can ignore them.
 * <p>
 * The pattern of interaction is as follows:
 * <ul>
 * <li>The session will first call {@link #afterAllFactsInserted(boolean)} on every {@link AbstractRootNode}.
 * Each lifecycle must propagate the call further downstream, until a {@link Scorer} is reached.
 * (In case of Constraint Streams.)</li>
 * <li>Then the session will call {@link #isActive()} on every single lifecycle,
 * and entirely remove deactivated lifecycles from propagation.
 * Each lifecycle makes its activity decision independently,
 * but may ask its downstream lifecycles.</li>
 * <li>Once these two steps have been executed,
 * no method of this interface will ever be called again
 * for the duration of the session.</li>
 * </ul>
 *
 * @see TupleLifecycle
 * @see LeftTupleLifecycle
 * @see RightTupleLifecycle
 */
public interface ActivitySupport {

    /**
     * Triggered after all facts which will ever be inserted have been inserted to the session;
     * it doesn't guarantee they were propagated as far as this lifecycle,
     * but the session now carries all the facts it will ever carry.
     * (The only way to insert or retract a fact is through a {@link ProblemChange},
     * and that will nuke the score director.)
     * <p>
     * It is the responsibility of the lifecycle to trigger initialization
     * of all of its downstream lifecycles, should there be any.
     * It must first decide for itself if it can produce tuples based on what it learned from upstream,
     * and then propagate that information downstream so that they can make their own activation decisions.
     * <p>
     * When deciding whether a lifecycle can produce tuples, consider the following:
     * <ul>
     * <li>
     * Typically, when upstream cannot produce tuples, neither can downstream.
     * (Unless downstream has the capability to fabricate tuples out of nowhere.)
     * </li>
     * <li>
     * Do not make decisions based on whether upstream actually produced any tuples by this point.
     * If upstream produced no tuples so far, it doesn't mean it will never produce any.
     * Filters on variables which previously did not match can easily create tuples during tuple updates.
     * </li>
     * </ul>
     *
     * @param upstreamCanProduceTuples True if the upstream lifecycle(s) will produce any tuples.
     *        If false, this lifecycle will never receive any tuples and can most likely deactivate itself.
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
     * but a join() downstream cannot, because its other side cannot produce tuples.
     * In this case, the left side must be deactivated as well,
     * unless it also outputs to another active downstream lifecycle.)
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
     * for each lifecycle, this method will only be called at the start of {@link BavetConstraintSession},
     * and never again.
     * It may be called multiple times by different upstream lifecycles,
     * and must always return the same value.
     *
     * <p>
     * Typically this decision will be {@code upstreamCanProduceTuples && downstreamIsActive},
     * but some specialized lifecycles may have to use different logic.
     *
     * @return true if this lifecycle can produce tuples
     */
    default boolean isActive() {
        throw new IllegalStateException(
                "Impossible state: lifecycle (%s) not yet initialized (afterAllFactsInserted not called)."
                        .formatted(this));
    }

}
