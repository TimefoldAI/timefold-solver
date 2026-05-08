package ai.timefold.solver.core.impl.bavet.uni;

import java.util.Objects;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.bavet.common.BavetRootNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ForEachFilteredUniNode<A>
        extends AbstractForEachUniNode<A> {

    private final TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle;
    private final Predicate<A> filter;

    public ForEachFilteredUniNode(Class<A> forEachClass, Predicate<A> filter,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(forEachClass, nextNodesTupleLifecycle, outputStoreSize);
        this.nextNodesTupleLifecycle = Objects.requireNonNull(nextNodesTupleLifecycle);
        this.filter = Objects.requireNonNull(filter);
    }

    @Override
    public void afterAllInserted() {
        nextNodesTupleLifecycle.afterAllFactsInserted(true);
    }

    @Override
    public boolean isActive() {
        // Always active, because we do not know what the filter will do.
        // Unless none of the downstream nodes are active.
        return nextNodesTupleLifecycle.isActive();
    }

    @Override
    public void insert(@Nullable A a) {
        if (!filter.test(a)) { // Skip inserting the tuple as it does not pass the filter.
            return;
        }
        super.insert(a);
    }

    @Override
    public void update(@Nullable A a) {
        var tuple = tupleMap.get(a);
        if (tuple == null) { // The tuple was never inserted because it did not pass the filter.
            insert(a);
        } else if (filter.test(a)) {
            updateExisting(a, tuple);
        } else { // Tuple no longer passes the filter.
            retract(a);
        }
    }

    @Override
    public void retract(@Nullable A a) {
        var tuple = tupleMap.remove(a);
        if (tuple == null) { // The tuple was never inserted because it did not pass the filter.
            return;
        }
        super.retractExisting(a, tuple);
    }

    @Override
    public boolean supports(BavetRootNode.LifecycleOperation lifecycleOperation) {
        return true;
    }

}
