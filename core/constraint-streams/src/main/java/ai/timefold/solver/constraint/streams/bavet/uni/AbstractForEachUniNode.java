package ai.timefold.solver.constraint.streams.bavet.uni;

import java.util.IdentityHashMap;
import java.util.Map;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractNode;
import ai.timefold.solver.constraint.streams.bavet.common.GenericDirtyQueue;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;

/**
 * Filtering nodes are expensive.
 * Considering that most streams start with a nullity check on genuine planning variables,
 * it makes sense to create a specialized version of the node for this case ({@link ForEachExcludingNullVarsUniNode}),
 * as opposed to forcing an extra filter node on the generic case ({@link ForEachIncludingNullVarsUniNode}).
 *
 * @param <A>
 */
public abstract sealed class AbstractForEachUniNode<A>
        extends AbstractNode
        permits ForEachIncludingNullVarsUniNode, ForEachExcludingNullVarsUniNode {

    private final Class<A> forEachClass;
    private final int outputStoreSize;
    private final GenericDirtyQueue<UniTuple<A>> dirtyTupleQueue;
    protected final Map<A, UniTuple<A>> tupleMap = new IdentityHashMap<>(1000);

    public AbstractForEachUniNode(Class<A> forEachClass, TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle,
            int outputStoreSize) {
        this.forEachClass = forEachClass;
        this.outputStoreSize = outputStoreSize;
        this.dirtyTupleQueue = new GenericDirtyQueue<>(nextNodesTupleLifecycle);
    }

    public void insert(A a) {
        UniTuple<A> tuple = new UniTuple<>(a, outputStoreSize);
        UniTuple<A> old = tupleMap.put(a, tuple);
        if (old != null) {
            throw new IllegalStateException("The fact (" + a + ") was already inserted, so it cannot insert again.");
        }
        dirtyTupleQueue.insert(tuple);
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
            dirtyTupleQueue.insertWithState(tuple, TupleState.UPDATING);
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
            dirtyTupleQueue.changeState(tuple, state == TupleState.CREATING ? TupleState.ABORTING : TupleState.DYING);
        } else {
            dirtyTupleQueue.insertWithState(tuple, TupleState.DYING);
        }
    }

    @Override
    public final void calculateScore() {
        dirtyTupleQueue.calculateScore(this);
    }

    @Override
    public final String toString() {
        return super.toString() + "(" + forEachClass.getSimpleName() + ")";
    }

    public final Class<A> getForEachClass() {
        return forEachClass;
    }

}
