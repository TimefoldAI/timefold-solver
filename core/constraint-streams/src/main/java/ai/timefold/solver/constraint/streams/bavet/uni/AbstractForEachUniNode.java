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

    protected final Map<A, UniTuple<A>> tupleMap = new IdentityHashMap<>(1000);
    protected final GenericDirtyQueue<UniTuple<A>> dirtyTupleQueue;

    public AbstractForEachUniNode(Class<A> forEachClass, TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle,
            int outputStoreSize) {
        this.forEachClass = forEachClass;
        this.outputStoreSize = outputStoreSize;
        this.dirtyTupleQueue = new GenericDirtyQueue<>(nextNodesTupleLifecycle);
    }

    protected UniTuple<A> createTuple(A a) {
        return new UniTuple<>(a, outputStoreSize);
    }

    public void insert(A a) {
        UniTuple<A> tuple = createTuple(a);
        UniTuple<A> old = tupleMap.put(a, tuple);
        if (old != null) {
            throw new IllegalStateException("The fact (" + a + ") was already inserted, so it cannot insert again.");
        }
        dirtyTupleQueue.insert(tuple);
    }

    public abstract void update(A a);

    protected void innerUpdate(A a, UniTuple<A> tuple) {
        if (tuple.state.isDirty()) {
            if (tuple.state == TupleState.DYING || tuple.state == TupleState.ABORTING) {
                throw new IllegalStateException("The fact (" + a + ") was retracted, so it cannot update.");
            }
        } else {
            dirtyTupleQueue.insertWithState(tuple, TupleState.UPDATING);
        }
    }

    public abstract void retract(A a);

    protected void innerRetract(A a, UniTuple<A> tuple) {
        if (tuple.state.isDirty()) {
            if (tuple.state == TupleState.DYING || tuple.state == TupleState.ABORTING) {
                throw new IllegalStateException("The fact (" + a + ") was already retracted, so it cannot retract.");
            }
            dirtyTupleQueue.changeState(tuple, TupleState.ABORTING);
        } else {
            dirtyTupleQueue.insertWithState(tuple, TupleState.DYING);
        }
    }

    @Override
    public void calculateScore() {
        dirtyTupleQueue.calculateScore(this);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + forEachClass.getSimpleName() + ")";
    }

    public Class<A> getForEachClass() {
        return forEachClass;
    }

}
