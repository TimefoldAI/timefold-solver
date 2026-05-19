package ai.timefold.solver.core.impl.bavet.uni;

import java.util.Objects;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.bavet.common.AbstractRootNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ForEachFilteredUniNode<A>
        extends AbstractForEachUniNode<A> {

    private final TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle;
    private final Predicate<A> filter;
    private int tupleCountWithoutFiltering = 0;

    public ForEachFilteredUniNode(Class<A> forEachClass, Predicate<A> filter,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(forEachClass, nextNodesTupleLifecycle, outputStoreSize);
        this.nextNodesTupleLifecycle = Objects.requireNonNull(nextNodesTupleLifecycle);
        this.filter = Objects.requireNonNull(filter);
    }

    @Override
    public void afterAllFactsInserted(boolean unused) {
        nextNodesTupleLifecycle.afterAllFactsInserted(tupleCountWithoutFiltering > 0);
    }

    @Override
    public boolean isActive() {
        // The input may change during update,
        // and therefore the filter may let things propagate which it previously did not.
        // For this reason, this node must be considered active if it saw at least one input;
        // only with zero tuples can it be considered inactive, as the filter has nothing to propagate.
        return tupleCountWithoutFiltering > 0 && nextNodesTupleLifecycle.isActive();
    }

    @Override
    public void insert(@Nullable A a) {
        tupleCountWithoutFiltering++; // This is safe; each element is only inserted once, guaranteed by a fail-fast in the parent.
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
        tupleCountWithoutFiltering--;
        var tuple = tupleMap.remove(a);
        if (tuple == null) { // The tuple was never inserted because it did not pass the filter.
            return;
        }
        super.retractExisting(a, tuple);
    }

    @Override
    public boolean supports(AbstractRootNode.LifecycleOperation lifecycleOperation) {
        return true;
    }

}
