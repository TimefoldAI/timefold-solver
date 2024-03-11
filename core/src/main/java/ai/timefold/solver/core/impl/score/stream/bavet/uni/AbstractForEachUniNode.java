package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.util.IdentityHashMap;
import java.util.Map;

import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.Propagator;
import ai.timefold.solver.core.impl.score.stream.bavet.common.StaticPropagationQueue;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleState;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

/**
 * Filtering nodes are expensive.
 * Considering that most streams start with a nullity check on genuine planning variables,
 * it makes sense to create a specialized version of the node for this case ({@link ForEachExcludingUnassignedUniNode}),
 * as opposed to forcing an extra filter node on the generic case ({@link ForEachIncludingUnassignedUniNode}).
 *
 * @param <A>
 */
public abstract sealed class AbstractForEachUniNode<A>
        extends AbstractNode
        permits ForEachExcludingUnassignedUniNode, ForEachIncludingUnassignedUniNode {

    private final Class<A> forEachClass;
    private final int outputStoreSize;
    private final StaticPropagationQueue<UniTuple<A>> propagationQueue;
    protected final Map<A, UniTuple<A>> tupleMap = new IdentityHashMap<>(1000);

    public AbstractForEachUniNode(Class<A> forEachClass, TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle,
            int outputStoreSize) {
        this.forEachClass = forEachClass;
        this.outputStoreSize = outputStoreSize;
        this.propagationQueue = new StaticPropagationQueue<>(nextNodesTupleLifecycle);
    }

    public void insert(A a) {
        UniTuple<A> tuple = new UniTuple<>(a, outputStoreSize);
        UniTuple<A> old = tupleMap.put(a, tuple);
        if (old != null) {
            throw new IllegalStateException("The fact (" + a + ") was already inserted, so it cannot insert again.");
        }
        propagationQueue.insert(tuple);
    }

    public abstract void update(A a);

    protected final void innerUpdate(A a, UniTuple<A> tuple) {
        TupleState state = tuple.state;
        if (state.isDirty()) {
            if (state == TupleState.DYING || state == TupleState.ABORTING) {
                throw new IllegalStateException("The fact (" + a + ") was retracted, so it cannot update.");
            }
            // CREATING or UPDATING is ignored; it's already in the queue.
        } else {
            propagationQueue.update(tuple);
        }
    }

    public void retract(A a) {
        UniTuple<A> tuple = tupleMap.remove(a);
        if (tuple == null) {
            throw new IllegalStateException("The fact (" + a + ") was never inserted, so it cannot retract.");
        }
        TupleState state = tuple.state;
        if (state.isDirty()) {
            if (state == TupleState.DYING || state == TupleState.ABORTING) {
                throw new IllegalStateException("The fact (" + a + ") was already retracted, so it cannot retract.");
            }
            propagationQueue.retract(tuple, state == TupleState.CREATING ? TupleState.ABORTING : TupleState.DYING);
        } else {
            propagationQueue.retract(tuple, TupleState.DYING);
        }
    }

    @Override
    public Propagator getPropagator() {
        return propagationQueue;
    }

    public final Class<A> getForEachClass() {
        return forEachClass;
    }

    @Override
    public final String toString() {
        return super.toString() + "(" + forEachClass.getSimpleName() + ")";
    }

}
