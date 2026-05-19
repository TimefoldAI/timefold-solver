package ai.timefold.solver.core.impl.bavet.common.tuple;

import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.impl.bavet.common.Propagator;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintSession;

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

}
