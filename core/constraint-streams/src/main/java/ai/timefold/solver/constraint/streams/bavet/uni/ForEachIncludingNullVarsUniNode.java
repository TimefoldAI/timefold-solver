package ai.timefold.solver.constraint.streams.bavet.uni;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;

public final class ForEachIncludingNullVarsUniNode<A> extends AbstractForEachUniNode<A> {

    public ForEachIncludingNullVarsUniNode(Class<A> forEachClass, TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle,
            int outputStoreSize) {
        super(forEachClass, nextNodesTupleLifecycle, outputStoreSize);
    }

    @Override
    public void update(A a) {
        UniTuple<A> tuple = tupleMap.get(a);
        if (tuple == null) {
            throw new IllegalStateException("The fact (" + a + ") was never inserted, so it cannot update.");
        }
        innerUpdate(a, tuple);
    }

    @Override
    public void retract(A a) {
        UniTuple<A> tuple = tupleMap.remove(a);
        if (tuple == null) {
            throw new IllegalStateException("The fact (" + a + ") was never inserted, so it cannot retract.");
        }
        innerRetract(a, tuple);
    }

}
