package ai.timefold.solver.core.impl.bavet.uni;

import java.util.Objects;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ForEachExcludingUnassignedUniNode<A>
        extends AbstractForEachUniNode<A> {

    private final Predicate<A> filter;

    public ForEachExcludingUnassignedUniNode(Class<A> forEachClass, Predicate<A> filter,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(forEachClass, nextNodesTupleLifecycle, outputStoreSize);
        this.filter = Objects.requireNonNull(filter);
    }

    @Override
    public void insert(A a) {
        if (!filter.test(a)) { // Skip inserting the tuple as it does not pass the filter.
            return;
        }
        super.insert(a);
    }

    @Override
    public void update(A a) {
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
    public void retract(A a) {
        var tuple = tupleMap.remove(a);
        if (tuple == null) { // The tuple was never inserted because it did not pass the filter.
            return;
        }
        super.retractExisting(a, tuple);
    }

    @Override
    public boolean supports(LifecycleOperation lifecycleOperation) {
        return true;
    }

}
