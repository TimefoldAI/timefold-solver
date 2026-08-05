package ai.timefold.solver.core.impl.bavet.common;

/**
 * Implemented by nodes that must run a preparation step at the start of their own layer's turn,
 * before {@link Propagator#propagateRetracts()}/{@link Propagator#propagateUpdates()}/
 * {@link Propagator#propagateInserts()} run for any node in that layer.
 * <p>
 * This is deliberately not a {@link Propagator} hook: {@link Propagator} is sealed to
 * {@code PropagationQueue}, {@code ProfilingPropagator} and {@code RecordAndReplayPropagator}, and a
 * default method added there would be silently inherited by the two decorators without forwarding to
 * the wrapped queue -- so a deferred preparation step would be skipped exactly when profiling or
 * record-and-replay is active. Driving this at the node level instead, from
 * {@link AbstractBavetNodeNetwork}, which already knows every node's layer and activity, avoids that
 * trap entirely, and costs nothing for the (overwhelming majority of) nodes that don't implement it.
 * <p>
 * Currently implemented only by join nodes that defer their filtering cross-match computation to
 * their own layer, instead of computing it eagerly whenever a parent propagates into them (see
 * {@code AbstractJoinNode}).
 */
public interface DeferredSettleAware {

    /**
     * Runs once per settle round, for this node's own layer, before any node in that layer begins its
     * retract/update/insert phases. By the time this runs, every ancestor of this node -- on every
     * input side, regardless of how deep -- has already completed its own full retract/update/insert
     * turn for this round, because layers are strictly ordered and this node's layer is strictly
     * greater than any of its ancestors'.
     */
    void prepareForSettle();

}
