package ai.timefold.solver.core.impl.bavet.common.tuple;

/**
 * Supplies a tuple's "is this, transitively, still live" answer. {@link Tuple} extends this — every
 * tuple already answers this about itself via its own activityParent1/activityParent2 chain — so
 * {@link #isActiveTransitively()} is one method regardless of producer. Only a producer whose
 * out-tuple's validity can't be expressed as that chain overrides the default explicitly; currently
 * only groupBy's {@code Group} (an OR across however many contributors currently exist, not a fixed
 * 1-or-2-tuple chain).
 */
public interface TupleActivitySource {

    boolean isActiveTransitively();

}
