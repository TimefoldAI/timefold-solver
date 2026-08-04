package ai.timefold.solver.core.impl.bavet.common.tuple;

import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A tuple is an <i>out tuple</i> in exactly one node and an <i>in tuple</i> in one or more nodes.
 *
 * <p>
 * A tuple must not implement equals()/hashCode() to fact equality,
 * because some stream operations ({@link UniConstraintStream#map(Function)}, ...)
 * might create 2 different tuple instances to contain the same facts
 * and because a tuple's origin may replace a tuple's fact.
 *
 * <p>
 * A tuple is modifiable.
 * However, only the origin node of a tuple (the node where the tuple is the out tuple) may modify it.
 */
@NullMarked
public sealed interface Tuple permits BiTuple, QuadTuple, TriTuple, UniTuple {

    TupleState getState();

    void setState(TupleState state);

    <Value_> @Nullable Value_ getStore(int index);

    void setStore(int index, @Nullable Object value);

    <Value_> @Nullable Value_ removeStore(int index);

    /**
     * Records the one or two other tuples, if any, whose own activity this tuple's activity is entangled
     * with (see {@link #isActiveTransitively}). Not readable from here: only {@link #isActiveTransitively}
     * ever needs to walk this link, and it does so via direct field access on the sealed hierarchy's sole
     * implementation, without a getter pair.
     * <p>
     * {@code map}/{@code flatten} call the single-argument overload, pointing at their one input.
     * Join nodes call the two-argument overload, pointing at their left and right input. {@code
     * ifExists}/{@code ifNotExists} need no equivalent: they re-propagate the original left tuple unchanged,
     * so they inherit whatever chain that tuple already carries. {@code groupBy}/{@code distinct} must never
     * call this: a group tuple's validity isn't reducible to any single contributing tuple (it's an N:1
     * aggregate), so there is no correct parent to link.
     */
    void setActivityParent(Tuple activityParent);

    /**
     * @see #setActivityParent(Tuple)
     */
    void setActivityParents(Tuple leftParent, Tuple rightParent);

    /**
     * Whether {@code tuple}, and everything it was derived from, is still live — closes a race that
     * plain {@code tuple.getState().isActive()} cannot see on its own.
     * <p>
     * A join, {@code ifExists}/{@code ifNotExists}, {@code map}, or {@code flatten} node retracts its
     * own out-tuple the moment its retracting parent's flush <i>reaches</i> it — but that flush only runs
     * during the parent's own network layer, which can come <i>after</i> a shallow sibling has already
     * re-tested this out-tuple against some unrelated update. During that window, {@code tuple.getState()}
     * alone still reports {@code OK} or {@code UPDATING} (both "active"), even though the tuple is only one
     * flush away from being retracted. Concretely: a 3-hop self-join's last join can be poked by its own
     * right-hand {@code .map()} bridge (a shallow, low-layer node) before an earlier join in the chain has
     * had its turn to retract the tuple that poke is about to read — the read happens on stale-but-"active"
     * data, and a user filtering predicate that dereferences a now-null shadow variable crashes.
     * <p>
     * {@link #setActivityParent(Tuple)} closes that window: it points at a tuple this one was built from,
     * and that tuple's own {@code getState()} is always synchronously correct, because retraction eagerly
     * sets a tuple's state the instant its own producing node's {@code retract()} runs — regardless of when
     * that producing node's own downstream notification is flushed. Walking the chain therefore always
     * terminates on state that's trustworthy, even when the tuple in hand is not (yet).
     * <p>
     * A join's out-tuple has <i>two</i> such ancestors (left and right), because either side retracting
     * makes it stale — not just the left one; both are checked (the right one recursively, since it can
     * itself be a deeper map/flatten chain).
     * <p>
     * Ceiling: {@code groupBy}/{@code distinct} never call {@link #setActivityParent(Tuple)} on their
     * output, because a group tuple has no single input tuple whose activity implies the group's own — it's
     * an N:1 aggregate that can outlive any one contributor's retraction, or die because a <i>different</i>
     * contributor left. The walk correctly stops there; it just can't see past it.
     */
    boolean isActiveTransitively();

}
