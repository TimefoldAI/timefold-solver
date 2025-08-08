package ai.timefold.solver.core.impl.bavet.uni;

import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ForEachIncludingUnassignedUniNode<A>
        extends AbstractForEachUniNode<A> {

    public ForEachIncludingUnassignedUniNode(Class<A> forEachClass, TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle,
            int outputStoreSize) {
        super(forEachClass, nextNodesTupleLifecycle, outputStoreSize);
    }

    @Override
    public void update(@Nullable A a) {
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
