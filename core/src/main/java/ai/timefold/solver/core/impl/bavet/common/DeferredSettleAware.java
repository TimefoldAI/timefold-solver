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
 * Implemented by two-input nodes that defer their filtering cross-match computation
 * (the opposite-side read) to their own layer,
 * instead of computing it eagerly whenever a parent propagates into them:
 * {@code AbstractJoinNode} and {@code AbstractIfExistsNode}.
 * Both implement it unconditionally (filtering or not),
 * so {@link #canDeferWork()} is what actually distinguishes an instance with pending work
 * from one that never enqueues anything.
 */
public interface DeferredSettleAware {

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
     * Only a filtering node whose two inputs are at least two layers apart can:
     * a non-filtering node never dereferences a fact through a user predicate,
     * and a filtering node whose inputs are at most one layer apart cannot observe a stale tuple
     * that still reports itself as active, so both read the opposite side eagerly instead.
     * <p>
     * Requires {@link #setInputLayerDelta(long)} to have run.
     */
    boolean canDeferWork();

    /**
     * The absolute difference between the layers of this node's two input parents,
     * which decides whether a stale read is possible here at all;
     * see {@link #canDeferWork()}.
     * Called exactly once per node, while the node network is being layered,
     * before any tuple reaches the node.
     */
    void setInputLayerDelta(long inputLayerDelta);

    /**
     * Turns deferral off for the duration of a session preload
     * ({@link AbstractBavetNodeNetwork#settle()} filling an empty network for the first time),
     * where every tuple arrives exactly once as an insert
     * and therefore no stale read is possible regardless of layer distance.
     * The node is allowed to change the return of {@link #canDeferWork()}.
     */
    void preloadStarted();

    /**
     * The inverse of {@link #preloadStarted()}.
     */
    void preloadEnded();

}
