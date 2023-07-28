package ai.timefold.solver.constraint.streams.bavet.uni;

import java.util.IdentityHashMap;
import java.util.Map;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractNode;
import ai.timefold.solver.constraint.streams.bavet.common.GenericDirtyQueue;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleState;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;

public final class ForEachUniNode<A> extends AbstractNode {

    private final Class<A> forEachClass;
    private final int outputStoreSize;

    private final Map<A, UniTuple<A>> tupleMap = new IdentityHashMap<>(1000);
    private final GenericDirtyQueue<UniTuple<A>> dirtyTupleQueue;

    public ForEachUniNode(Class<A> forEachClass, TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle, int outputStoreSize) {
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

    public void update(A a) {
        UniTuple<A> tuple = tupleMap.get(a);
        if (tuple == null) {
            throw new IllegalStateException("The fact (" + a + ") was never inserted, so it cannot update.");
        }
        if (tuple.state.isDirty()) {
            if (tuple.state == TupleState.DYING || tuple.state == TupleState.ABORTING) {
                throw new IllegalStateException("The fact (" + a + ") was retracted, so it cannot update.");
            }
        } else {
            dirtyTupleQueue.insertWithState(tuple, TupleState.UPDATING);
        }
    }

    public void retract(A a) {
        UniTuple<A> tuple = tupleMap.remove(a);
        if (tuple == null) {
            throw new IllegalStateException("The fact (" + a + ") was never inserted, so it cannot retract.");
        }
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
