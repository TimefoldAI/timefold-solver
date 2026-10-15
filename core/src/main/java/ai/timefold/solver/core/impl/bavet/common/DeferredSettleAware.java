package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.AbstractBavetNodeNetwork;

/**
 * Implemented by nodes that must run a preparation step at the start of their own layer's turn,
 * before {@link Propagator#propagateRetracts()}/{@link Propagator#propagateUpdates()}/
 * {@link Propagator#propagateInserts()} run for any node in that layer.
 * <p>
 * This is deliberately not a {@link Propagator} hook:
 * {@link Propagator} is sealed and a default method added there would be silently inherited
 * by the two decorators without forwarding to the wrapped queue.
 * A deferred preparation step would be skipped exactly when profiling or record-and-replay is active.
 * Driving this at the node level instead,
 * from {@link AbstractBavetNodeNetwork} which already knows every node's layer and activity,
 * avoids that trap entirely,
 * and costs nothing for the (overwhelming majority of) nodes that don't implement it.
 * <p>
 * Implemented by {@code AbstractCrossMatchNode}, the shared parent of the two-input nodes that
 * cross-match a left tuple against a right tuple through a user predicate
 * ({@code AbstractJoinNode} and {@code AbstractIfExistsNode}).
 * It implements this unconditionally (filtering or not),
 * so {@link #canDeferWork()} is what actually distinguishes an instance with pending work
 * from one that never enqueues anything.
 */
public interface DeferredSettleAware {

    /**
     * The smallest settle distance at which a stale read is possible,
     * and therefore the smallest at which {@link #canDeferWork()} returns true.
     * Below it, a filtering node can only ever read a tuple that has already been told it is doomed,
     * which the per-read {@code isActive()} guards catch.
     */
    long MIN_DEFER_DISTANCE = 2;

    /**
     * Runs once per {@link AbstractBavetNodeNetwork#settle()} round,
     * for this node's own layer,
     * before any node in that layer begins its retract/update/insert phases.
     * By the time this runs, every ancestor of this node
     * (on every input side, regardless of how deep)
     * has already completed its own full retract/update/insert turn for this round.
     * Layers are strictly ordered and this node's layer is strictly greater than any of its ancestors'.
     */
    void prepareForSettle();

    /**
     * Whether the node can ever have pending work for {@link #prepareForSettle()} to drain.
     * Only a filtering node whose inputs are at least {@link #MIN_DEFER_DISTANCE} settlement steps apart can:
     * a non-filtering node never dereferences a fact through a user predicate,
     * and a filtering node whose inputs are closer than that cannot observe a stale tuple
     * that still reports itself as active, so both read the opposite side eagerly instead.
     * <p>
     * Requires {@link #setSettleDistance(long)} to have run.
     */
    boolean canDeferWork();

    /**
     * How many settlement steps apart this node's two inputs are:
     * the absolute difference between their layers,
     * plus one when the deeper parent is itself a deferring node
     * (it dooms its out-tuples in its own {@link #prepareForSettle()},
     * one step later than an ordinary parent retracts them).
     * This is what decides whether a stale read is possible here at all;
     * see {@link #canDeferWork()}.
     * Called exactly once per node, while the node network is being layered,
     * before any tuple reaches the node.
     */
    void setSettleDistance(long settleDistance);

    /**
     * Turns deferral off for the duration of a session preload
     * ({@link AbstractBavetNodeNetwork#settle()} filling an empty network for the first time),
     * where every tuple arrives exactly once as an insert
     * and therefore no stale read is possible regardless of settle distance.
     * The node is allowed to change the return of {@link #canDeferWork()}.
     */
    void preloadStarted();

    /**
     * The inverse of {@link #preloadStarted()}.
     */
    void preloadEnded();

}
