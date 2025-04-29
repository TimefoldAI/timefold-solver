package ai.timefold.solver.core.impl.bavet.uni;

import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed class ForEachIncludingUnassignedUniNode<A>
        extends AbstractForEachUniNode<A>
        permits ForEachFromSolutionUniNode {

    public ForEachIncludingUnassignedUniNode(Class<A> forEachClass, TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle,
            int outputStoreSize) {
        super(forEachClass, nextNodesTupleLifecycle, outputStoreSize);
    }

    @Override
    public void update(A a) {
        var tuple = tupleMap.get(a);
        if (tuple == null) {
            throw new IllegalStateException("The fact (%s) was never inserted, so it cannot update."
                    .formatted(a));
        }
        updateExisting(a, tuple);
    }

    @Override
    public boolean supports(LifecycleOperation lifecycleOperation) {
        return true;
    }

}
