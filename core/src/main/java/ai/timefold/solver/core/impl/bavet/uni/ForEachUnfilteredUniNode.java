package ai.timefold.solver.core.impl.bavet.uni;

import ai.timefold.solver.core.impl.bavet.common.BavetRootNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ForEachUnfilteredUniNode<A>
        extends AbstractForEachUniNode<A> {

    private final TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle;
    private boolean isActive;

    public ForEachUnfilteredUniNode(Class<A> forEachClass, TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle,
            int outputStoreSize) {
        super(forEachClass, nextNodesTupleLifecycle, outputStoreSize);
        this.nextNodesTupleLifecycle = nextNodesTupleLifecycle;
    }

    @Override
    public void afterAllInserted() {
        isActive = !tupleMap.isEmpty();
        nextNodesTupleLifecycle.initialize(isActive);
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void update(@Nullable A a) {
        var tuple = tupleMap.get(a);
        if (tuple == null) {
            throw new IllegalStateException("The fact (%s) was never inserted."
                    .formatted(a));
        }
        updateExisting(a, tuple);
    }

    @Override
    public boolean supports(BavetRootNode.LifecycleOperation lifecycleOperation) {
        return true;
    }

}
